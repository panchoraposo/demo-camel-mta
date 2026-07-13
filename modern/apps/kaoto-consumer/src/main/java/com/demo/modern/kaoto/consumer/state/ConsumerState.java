package com.demo.modern.kaoto.consumer.state;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConsumerState {

  private final AtomicLong received = new AtomicLong(0);
  private final AtomicLong duplicates = new AtomicLong(0);
  private final Set<String> seenIds = new HashSet<>();
  private final Deque<LastItem> last = new ArrayDeque<>();

  public record LastItem(String receivedAt, String eventId, String body) {}

  public record Snapshot(long received, long duplicates, List<LastItem> last) {}

  /**
   * Called from the YAML route via "bean:consumerState?method=onMessage".
   */
  public void onMessage(String body) {
    String id = extractEventId(body);
    boolean dup;
    synchronized (seenIds) {
      dup = (id != null && !id.isBlank()) && !seenIds.add(id);
    }
    if (dup) {
      duplicates.incrementAndGet();
    } else {
      received.incrementAndGet();
    }

    synchronized (last) {
      last.addFirst(new LastItem(Instant.now().toString(), id, body));
      while (last.size() > 20) last.removeLast();
    }
  }

  public Snapshot snapshot() {
    List<LastItem> out;
    synchronized (last) {
      out = new ArrayList<>(last);
    }
    return new Snapshot(received.get(), duplicates.get(), out);
  }

  private static String extractEventId(String body) {
    if (body == null) return null;
    // Cheap extraction for demo payloads: {"eventId":"..."}
    int i = body.indexOf("\"eventId\"");
    if (i < 0) return null;
    int colon = body.indexOf(':', i);
    if (colon < 0) return null;
    int q1 = body.indexOf('"', colon);
    if (q1 < 0) return null;
    int q2 = body.indexOf('"', q1 + 1);
    if (q2 < 0) return null;
    return body.substring(q1 + 1, q2);
  }
}

