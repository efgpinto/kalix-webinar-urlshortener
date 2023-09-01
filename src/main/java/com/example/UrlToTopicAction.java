package com.example;

import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;

@Subscribe.EventSourcedEntity(value = ShortUrlEntity.class, ignoreUnknown = true)
public class UrlToTopicAction extends Action {

  @Publish.Topic("urls-events")
  public Effect<ShortUrlEntity.UrlEvent> onCreate(ShortUrlEntity.UrlEvent.UrlCreated urlCreated) {
    Metadata md = Metadata.EMPTY.add("ce-subject", urlCreated.shortId());
    return effects().reply(urlCreated, md);
  }

  @Publish.Topic("urls-events")
  public Effect<ShortUrlEntity.UrlEvent> onAccess(ShortUrlEntity.UrlEvent.UrlAccessed urlAccessed) {
    Metadata md = Metadata.EMPTY.add("ce-subject", actionContext().eventSubject().get());
    return effects().reply(urlAccessed, md);
  }
}
