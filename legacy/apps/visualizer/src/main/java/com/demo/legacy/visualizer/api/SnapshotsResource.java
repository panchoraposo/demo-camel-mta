package com.demo.legacy.visualizer.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/api/snapshots")
@Produces(MediaType.APPLICATION_JSON)
public class SnapshotsResource {

  @ConfigProperty(name = "demo.visualizer.snapshot-urls")
  String snapshotUrls;

  @Inject
  ObjectMapper mapper;

  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  public record SnapshotEntry(String url, boolean ok, JsonNode snapshot, String error, String fetchedAt) {}

  @GET
  public List<SnapshotEntry> list() {
    List<String> urls = parseUrls(snapshotUrls);
    return urls.stream()
        .map(this::fetchOne)
        .collect(Collectors.toList());
  }

  private SnapshotEntry fetchOne(String url) {
    try {
      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(2))
          .GET()
          .build();
      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() / 100 != 2) {
        return new SnapshotEntry(url, false, null, "HTTP " + res.statusCode(), Instant.now().toString());
      }
      JsonNode node = mapper.readTree(res.body());
      return new SnapshotEntry(url, true, node, null, Instant.now().toString());
    } catch (Exception e) {
      return new SnapshotEntry(url, false, null, e.getClass().getSimpleName() + ": " + e.getMessage(), Instant.now().toString());
    }
  }

  private static List<String> parseUrls(String raw) {
    if (raw == null || raw.isBlank()) return List.of();
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toList());
  }
}

