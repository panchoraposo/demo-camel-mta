package com.demo.legacy.consumer.routes;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.demo.legacy.consumer.state.ConsumerState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ConsumerRoutes extends RouteBuilder {

  @ConfigProperty(name = "demo.broker.queue")
  String queueName;

  @Inject
  ConsumerState state;

  @Inject
  ObjectMapper mapper;

  @Override
  public void configure() {
    from("jms:queue:" + queueName)
        .routeId("consumeFromBroker")
        .process(e -> {
          String body = e.getIn().getBody(String.class);
          String eventId = null;
          try {
            JsonNode n = mapper.readTree(body);
            JsonNode id = n.get("eventId");
            if (id != null && !id.isNull()) {
              eventId = id.asText();
            }
          } catch (Exception ignored) {
            // keep body as-is
          }
          state.onEvent(eventId, body);
        })
        .to("log:consumer?level=INFO");
  }
}

