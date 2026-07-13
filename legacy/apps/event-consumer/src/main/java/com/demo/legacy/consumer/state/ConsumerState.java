package com.demo.legacy.consumer.state;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConsumerState {
  private final AtomicLong received = new AtomicLong();
  private final AtomicLong duplicates = new AtomicLong();
  private final Map<String, Boolean> seen = new ConcurrentHashMap<>();
  private final Deque<EventSample> last = new ArrayDeque<>();

  public void onEvent(String eventId, String body) {
    String key = (eventId != null && !eventId.isBlank()) ? eventId : body;
    if (key != null) {
      boolean dup = (seen.putIfAbsent(key, Boolean.TRUE) != null);
      if (dup) {
        duplicates.incrementAndGet();
        return;
      }
    }

    received.incrementAndGet();
    synchronized (last) {
      last.addFirst(new EventSample(Instant.now().toString(), eventId, body));
      while (last.size() > 20) {
        last.removeLast();
      }
    }
  }

  public Snapshot snapshot() {
    synchronized (last) {
      return new Snapshot(received.get(), duplicates.get(), last.toArray(new EventSample[0]));
    }
  }

  public record EventSample(String receivedAt, String eventId, String body) {}

  public record Snapshot(long received, long duplicates, EventSample[] last) {}
}

