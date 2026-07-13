package com.demo.modern.producer.state;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProducerState {

  private final AtomicLong sent = new AtomicLong(0);
  private final Deque<LastItem> last = new ArrayDeque<>();

  public record LastItem(String sentAt, String eventId, String body) {}

  public record Snapshot(long sent, List<LastItem> last) {}

  public void recordSend(String eventId, String body) {
    sent.incrementAndGet();
    synchronized (last) {
      last.addFirst(new LastItem(Instant.now().toString(), eventId, body));
      while (last.size() > 20) {
        last.removeLast();
      }
    }
  }

  public Snapshot snapshot() {
    List<LastItem> out;
    synchronized (last) {
      out = new ArrayList<>(last);
    }
    return new Snapshot(sent.get(), out);
  }
}

