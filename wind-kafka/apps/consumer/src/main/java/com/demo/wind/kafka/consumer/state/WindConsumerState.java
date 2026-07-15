package com.demo.wind.kafka.consumer.state;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.demo.wind.kafka.consumer.model.WindEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
@Named("windConsumerState")
public class WindConsumerState {

  private final ObjectMapper mapper;

  private final AtomicLong consumed = new AtomicLong(0);
  private final AtomicLong totalWatts = new AtomicLong(0);
  private final AtomicLong lastTs = new AtomicLong(0);

  private volatile WindEvent last;
  private volatile double distance;
  private volatile double speedFactor;

  private final BroadcastProcessor<Snapshot> snapshots = BroadcastProcessor.create();

  @ConfigProperty(name = "demo.consumer.name")
  String consumerName;

  public WindConsumerState(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public void onMessage(String body) {
    try {
      WindEvent ev = mapper.readValue(body, WindEvent.class);
      last = ev;

      long n = consumed.incrementAndGet();
      totalWatts.addAndGet(Math.max(0, ev.watts));

      long prev = lastTs.getAndSet(ev.ts);
      long dtMs = prev == 0 ? 0 : Math.max(0, ev.ts - prev);

      // Toy physics:
      // - speedFactor reacts to message rate and watts
      // - distance integrates speedFactor over time
      double wattsNorm = Math.min(1.0, ev.watts / 2500.0);
      double rateBoost = dtMs == 0 ? 0.0 : Math.min(1.0, (250.0 / dtMs)); // higher when messages are faster
      speedFactor = clamp01(0.15 + (0.65 * wattsNorm) + (0.20 * rateBoost));

      distance += speedFactor * 4.0; // arbitrary units per event

      snapshots.onNext(snapshot(n));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Snapshot snapshot() {
    return snapshot(consumed.get());
  }

  public Multi<Snapshot> stream() {
    return Multi.createFrom().publisher(snapshots);
  }

  private Snapshot snapshot(long n) {
    return new Snapshot(
        consumerName,
        n,
        totalWatts.get(),
        Instant.now().toEpochMilli(),
        last,
        distance,
        speedFactor);
  }

  private static double clamp01(double v) {
    if (v < 0) {
      return 0;
    }
    if (v > 1) {
      return 1;
    }
    return v;
  }

  public record Snapshot(
      String consumer,
      long consumed,
      long totalWatts,
      long serverTs,
      WindEvent last,
      double distance,
      double speedFactor) {
  }
}

