package com.demo.legacy.visualizer.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.demo.legacy.visualizer.config.VisualizerClustersConfig;
import com.demo.legacy.visualizer.config.VisualizerClustersConfig.Cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("/api/snapshots")
@Produces(MediaType.APPLICATION_JSON)
public class SnapshotsResource {

  @Inject
  VisualizerClustersConfig cfg;

  @Inject
  ObjectMapper mapper;

  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  public record SnapshotEntry(String id, String label, String color, String url, boolean ok, JsonNode snapshot, String error,
      String fetchedAt) {}

  @GET
  public List<SnapshotEntry> list() {
    List<Cluster> clusters = cfg.clusters();
    return clusters.stream().map(this::fetchOne).toList();
  }

  private SnapshotEntry fetchOne(Cluster c) {
    if (!c.hasSnapshotUrl()) {
      return new SnapshotEntry(c.id(), c.label(), c.color(), "DEMO_DATA", true, syntheticSnapshot(c), null, Instant.now().toString());
    }

    try {
      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(c.snapshotUrl()))
          .timeout(Duration.ofSeconds(2))
          .GET()
          .build();
      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() / 100 != 2) {
        return new SnapshotEntry(c.id(), c.label(), c.color(), c.snapshotUrl(), false, null, "HTTP " + res.statusCode(),
            Instant.now().toString());
      }
      JsonNode node = mapper.readTree(res.body());
      return new SnapshotEntry(c.id(), c.label(), c.color(), c.snapshotUrl(), true, node, null, Instant.now().toString());
    } catch (Exception e) {
      return new SnapshotEntry(c.id(), c.label(), c.color(), c.snapshotUrl(), false, null,
          e.getClass().getSimpleName() + ": " + e.getMessage(), Instant.now().toString());
    }
  }

  private JsonNode syntheticSnapshot(Cluster c) {
    long now = System.currentTimeMillis();
    long received = (now / 1500) % 1000;

    ObjectNode root = mapper.createObjectNode();
    root.put("received", received);
    root.put("duplicates", 0);

    ArrayNode last = mapper.createArrayNode();
    for (int i = 0; i < 5; i++) {
      ObjectNode item = mapper.createObjectNode();
      item.put("receivedAt", Instant.ofEpochMilli(now - (i * 1500L)).toString());
      item.put("eventId", c.id() + "-" + (received - i));
      item.put("body", "{\"producerId\":\"demo-data\",\"payload\":\"tick\",\"cluster\":\"" + c.label() + "\"}");
      last.add(item);
    }
    root.set("last", last);
    return root;
  }
}

