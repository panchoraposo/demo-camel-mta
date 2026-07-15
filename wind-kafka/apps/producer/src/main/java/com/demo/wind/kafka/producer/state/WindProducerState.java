package com.demo.wind.kafka.producer.state;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.demo.wind.kafka.producer.model.WindEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
@Named("windProducerState")
public class WindProducerState {

  private final ObjectMapper mapper;
  private final Random random = new Random();
  private final AtomicLong produced = new AtomicLong(0);

  // Simple oscillator to make the demo feel "alive"
  private double phase = 0.0;
  private volatile WindEvent last;

  @ConfigProperty(name = "demo.producer.id")
  String producerId;

  public WindProducerState(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public synchronized String nextEventJson() {
    long n = produced.incrementAndGet();
    phase += 0.12;

    int base = (int) Math.round(50 + (Math.sin(phase) * 35));
    int gust = Math.max(0, Math.min(100, base + random.nextInt(30) - 15));
    int speed = Math.max(0, Math.min(100, (int) Math.round((base * 0.7) + (gust * 0.3))));

    // "Power" rises sharply with speed (toy model)
    int watts = (int) Math.round(Math.pow(speed / 100.0, 3) * 2500);

    WindEvent ev = new WindEvent(producerId, Instant.now().toEpochMilli(), speed, gust, watts);
    last = ev;

    try {
      return mapper.writeValueAsString(ev);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public Snapshot snapshot() {
    return new Snapshot(produced.get(), last);
  }

  public record Snapshot(long produced, WindEvent last) {
  }
}

