package com.demo.modern.visualizer.api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

  @ConfigProperty(name = "demo.visualizer.snapshot-urls")
  String snapshotUrls;

  public record UiConfig(List<String> snapshotUrls) {}

  @GET
  public UiConfig get() {
    if (snapshotUrls == null || snapshotUrls.isBlank()) {
      return new UiConfig(List.of());
    }
    List<String> urls = Arrays.stream(snapshotUrls.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toList());
    return new UiConfig(urls);
  }
}

