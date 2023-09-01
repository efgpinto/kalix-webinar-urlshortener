package com.example;

import kalix.javasdk.Metadata;
import kalix.javasdk.StatusCode;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.MalformedURLException;
import java.net.URL;

@Id("shortId")
@TypeId("url")
@RequestMapping("/u/{shortId}")
public class ShortUrlEntity extends EventSourcedEntity<ShortUrlEntity.UrlState, ShortUrlEntity.UrlEvent> {

  private static final Logger logger = LoggerFactory.getLogger(ShortUrlEntity.class);

  record UrlState(String id, String shortId, URL originalUrl) {}

  sealed interface UrlEvent {
    record UrlCreated(String id, String shortId, URL originalUrl, long ts) implements UrlEvent {}
    record UrlAccessed(String id, long ts) implements UrlEvent {}
  }

  record CreateUrlCmd(String id, String shortId, String originalUrl) {}

  @PostMapping("/create")
  public Effect<String> create(@RequestBody CreateUrlCmd createUrlCmd) {
    logger.info("Creating url: " + createUrlCmd);

    try {
      var createdUrlEvent = new UrlEvent.UrlCreated(createUrlCmd.id, createUrlCmd.shortId, new URL(createUrlCmd.originalUrl()), System.currentTimeMillis());

      return effects()
          .emitEvent(createdUrlEvent)
          .thenReply(__ -> createdUrlEvent.shortId);
    } catch (MalformedURLException e) {
      return effects().error("Bad request URL:" + createUrlCmd.originalUrl);
    }
  }

  @GetMapping
  public Effect<String> get() {
    if (currentState() == null) {
      effects().error("URL does not exist", StatusCode.ErrorCode.NOT_FOUND);
    }

    logger.info("get url: " + currentState());

    var md = Metadata.EMPTY
        .add("Location", currentState().originalUrl.toString())
        .add("_kalix-http-code", "301");

    var accessedEvent = new UrlEvent.UrlAccessed(currentState().id, System.currentTimeMillis());

    return effects()
        .emitEvent(accessedEvent)
        .thenReply(__ -> "redirected", md);
  }

  @EventHandler
  public UrlState onCreate(UrlEvent.UrlCreated urlCreated) {
    return new UrlState(urlCreated.id, urlCreated.shortId(), urlCreated.originalUrl());
  }

  @EventHandler
  public UrlState onAccess(UrlEvent.UrlAccessed urlAccessed) {
    return currentState();
  }
}
