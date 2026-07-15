package com.demo.wind.kafka.frontend.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

  @ConfigProperty(name = "demo.consumer.base-url")
  String consumerBaseUrl;

  @ConfigProperty(name = "demo.consumer.streams")
  int consumerStreams;

  @GET
  public Config config() {
    String base = consumerBaseUrl == null ? null : consumerBaseUrl.replaceAll("/+$", "");
    String streamUrl = (base == null || base.isBlank()) ? null : base + "/api/stream";
    return new Config(consumerBaseUrl, consumerStreams, "/api/consumer/snapshot", streamUrl);
  }

  public record Config(String consumerBaseUrl, int consumerStreams, String snapshotUrl, String streamUrl) {
  }
}

