package com.demo.legacy.producer.service;

import java.time.Instant;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.demo.legacy.producer.model.DemoEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ProducerService {

  @ConfigProperty(name = "demo.producer.id")
  String producerId;

  @Inject
  ProducerTemplate template;

  @Inject
  ObjectMapper mapper;

  public DemoEvent sendOne(String payload) throws Exception {
    DemoEvent event = new DemoEvent(
        UUID.randomUUID().toString(),
        producerId,
        Instant.now().toString(),
        payload
    );
    String json = mapper.writeValueAsString(event);
    template.sendBody("direct:send", json);
    return event;
  }
}

