package com.demo.modern.producer.service;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.demo.modern.producer.model.DemoEvent;
import com.demo.modern.producer.state.ProducerState;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ProducerService {

  @ConfigProperty(name = "demo.producer.id")
  String producerId;

  @Inject
  ProducerTemplate template;

  @Inject
  ObjectMapper mapper;

  @Inject
  ProducerState state;

  public DemoEvent sendOne(String payload) throws Exception {
    DemoEvent event = new DemoEvent(
        UUID.randomUUID().toString(),
        producerId,
        Instant.now().toString(),
        payload
    );
    String json = mapper.writeValueAsString(event);
    template.sendBody("direct:send", json);
    state.recordSend(event.eventId, json);
    return event;
  }
}

