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
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
@Named("consumerState")
public class ConsumerState {

  private static final Logger LOG = Logger.getLogger(ConsumerState.class);

  @ConfigProperty(name = "demo.consumer.name", defaultValue = "kaoto-consumer")
  String consumerName;

  @ConfigProperty(name = "demo.broker.queue", defaultValue = "demo.events")
  String queueName;

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
    String type = extractField(body, "type");
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

    LOG.infov("consumer={0} queue={1} type={2} eventId={3} receivedTotal={4}",
        consumerName, queueName, type, id, received.get());
  }

  public Snapshot snapshot() {
    List<LastItem> out;
    synchronized (last) {
      out = new ArrayList<>(last);
    }
    return new Snapshot(received.get(), duplicates.get(), out);
  }

  private static String extractEventId(String body) {
    return extractField(body, "eventId");
  }

  private static String extractField(String body, String field) {
    if (body == null) return null;
    // Cheap extraction for demo payloads: {"field":"..."}
    String key = "\"" + field + "\"";
    int i = body.indexOf(key);
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

