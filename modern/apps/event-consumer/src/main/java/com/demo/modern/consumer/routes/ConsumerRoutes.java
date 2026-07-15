package com.demo.modern.consumer.routes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.demo.modern.consumer.state.ConsumerState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ConsumerRoutes extends RouteBuilder {

  private static final Logger LOG = Logger.getLogger(ConsumerRoutes.class);

  @ConfigProperty(name = "demo.broker.queue")
  String queueName;

  @ConfigProperty(name = "demo.consumer.name", defaultValue = "event-consumer")
  String consumerName;

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
          String eventType = null;
          try {
            JsonNode n = mapper.readTree(body);
            JsonNode id = n.get("eventId");
            if (id != null && !id.isNull()) {
              eventId = id.asText();
            }
            JsonNode type = n.get("type");
            if (type != null && !type.isNull()) {
              eventType = type.asText();
            }
          } catch (Exception ignored) {
            // keep body as-is
          }
          state.onEvent(eventId, body);

          long received = state.snapshot().received();
          LOG.infov("CONSUME consumer={0} queue={1} type={2} eventId={3} receivedTotal={4}",
              consumerName, queueName, eventType, eventId, received);
        })
        .end();
  }
}

