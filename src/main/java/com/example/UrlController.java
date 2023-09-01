package com.example;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Acl;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RequestMapping("/s")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class UrlController extends Action {

  private final MessageDigest messageDigest;
  private final ComponentClient componentClient;

  record Url(String url) {}

  public UrlController(ComponentClient componentClient) throws NoSuchAlgorithmException {
    messageDigest = MessageDigest.getInstance("MD5");
    this.componentClient = componentClient;
  }

  @PostMapping("/create")
  public Effect<String> createShortUrl(@RequestBody Url url) {

    // generate the short id
    var md5Hash = messageDigest.digest(url.url.getBytes());
    var shortId = Base62.encode(md5Hash).substring(0, 7);

    var creationCmd = new ShortUrlEntity.CreateUrlCmd(UUID.randomUUID().toString(), shortId, url.url);
    var defCall = componentClient.forEventSourcedEntity(shortId).call(ShortUrlEntity::create).params(creationCmd);

    // create deferred call to ES entity
    return effects().forward(defCall);
  }

  @GetMapping("/{shortId}")
  public Effect<String> get(@PathVariable String shortId) {
    return effects().forward(componentClient.forEventSourcedEntity(shortId).call(ShortUrlEntity::get));
  }
}
