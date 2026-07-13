package com.demo.modern.producer.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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

  @ConfigProperty(name = "demo.producer.event-types", defaultValue = "tick,order,inventory,alert")
  String eventTypes;

  @Inject
  ProducerTemplate template;

  @Inject
  ObjectMapper mapper;

  @Inject
  ProducerState state;

  public DemoEvent sendOne(String payload) throws Exception {
    return sendOne("custom", payload);
  }

  public DemoEvent sendOne(String type, String payload) throws Exception {
    DemoEvent event = new DemoEvent(
        UUID.randomUUID().toString(),
        producerId,
        Instant.now().toString(),
        type,
        payload
    );
    String json = mapper.writeValueAsString(event);
    template.sendBody("direct:send", json);
    state.recordSend(event.eventId, json);
    return event;
  }

  public DemoEvent sendAuto() throws Exception {
    String type = pickType();
    String payload = generatePayload(type);
    return sendOne(type, payload);
  }

  private String pickType() {
    List<String> types = parseCsv(eventTypes);
    if (types.isEmpty()) return "tick";
    int i = ThreadLocalRandom.current().nextInt(types.size());
    return types.get(i);
  }

  private static List<String> parseCsv(String raw) {
    if (raw == null || raw.isBlank()) return List.of();
    String[] parts = raw.split(",");
    List<String> out = new ArrayList<>();
    for (String p : parts) {
      String s = p == null ? "" : p.trim();
      if (!s.isBlank()) out.add(s);
    }
    return out;
  }

  private static String generatePayload(String type) {
    String t = type == null ? "" : type.trim().toLowerCase();
    long n = ThreadLocalRandom.current().nextLong(1000, 9999);
    int sev = ThreadLocalRandom.current().nextInt(1, 6);
    return switch (t) {
      case "order" -> "orderId=ORD-" + n + " amount=" + ThreadLocalRandom.current().nextInt(10, 900) + " currency=USD";
      case "inventory" -> "sku=SKU-" + n + " delta=" + ThreadLocalRandom.current().nextInt(-50, 120);
      case "alert" -> "severity=" + sev + " code=AL-" + n + " msg=threshold exceeded";
      default -> "tick=" + n;
    };
  }
}

