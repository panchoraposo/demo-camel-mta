package com.demo.modern.producer.routes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.demo.modern.producer.service.ProducerService;

@ApplicationScoped
public class ProducerRoutes extends RouteBuilder {

  @ConfigProperty(name = "demo.producer.timer-enabled")
  boolean timerEnabled;

  @ConfigProperty(name = "demo.producer.period-ms")
  long periodMs;

  @ConfigProperty(name = "demo.broker.queue")
  String queueName;

  @Inject
  ProducerService service;

  @Override
  public void configure() {
    from("direct:send")
        .routeId("directSendToBroker")
        .toD("jms:queue:" + queueName);

    fromF("timer:producerTick?period=%d", Math.max(250, periodMs))
        .routeId("timerSendToBroker")
        .autoStartup(timerEnabled)
        .process(e -> {
          try {
            service.sendAuto();
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        });
  }
}

