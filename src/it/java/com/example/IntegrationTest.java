package com.example;

import com.example.Main;
import kalix.eventing.Eventing;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Java SDK.
 *
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@SpringBootTest(classes = Main.class)
public class IntegrationTest extends KalixIntegrationTestKitSupport {

  private Duration timeout = Duration.of(5, SECONDS);

  @Autowired
  private ComponentClient componentClient;

  @Autowired
  private KalixTestKit kalixTestKit;

  private EventingTestKit.Topic outputTopic;

  @BeforeAll
  public void beforeAll() {
    outputTopic = kalixTestKit.getTopic("urls-events");
  }

  @Test
  public void test() throws Exception {
    // start by creating a shortened url
    var url = new UrlController.Url("https://google.pt");
    var shortId = componentClient.forAction().call(UrlController::createShortUrl).params(url).execute()
        .toCompletableFuture()
        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

    // assert that there is a create url event to topic urls-events
    var createdEvent = outputTopic.expectOneTyped(ShortUrlEntity.UrlEvent.UrlCreated.class);
    assertTrue(shortId.contains(createdEvent.getPayload().shortId()));

    componentClient.forEventSourcedEntity(createdEvent.getPayload().shortId()).call(ShortUrlEntity::get).execute()
        .toCompletableFuture()
        .get();

    var accessedEvent = outputTopic.expectOneTyped(ShortUrlEntity.UrlEvent.UrlAccessed.class);
    assertEquals(createdEvent.getPayload().id(), accessedEvent.getPayload().id());

  }
}