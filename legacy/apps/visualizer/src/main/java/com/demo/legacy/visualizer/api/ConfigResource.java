package com.demo.legacy.visualizer.api;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.demo.legacy.visualizer.config.VisualizerClustersConfig;
import com.demo.legacy.visualizer.config.VisualizerClustersConfig.Cluster;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

  @Inject
  VisualizerClustersConfig cfg;

  public record UiCluster(String id, String label, String color, String snapshotUrl) {}

  public record UiConfig(List<UiCluster> clusters) {}

  @GET
  public UiConfig get() {
    List<Cluster> clusters = cfg.clusters();
    return new UiConfig(clusters.stream()
        .map(c -> new UiCluster(c.id(), c.label(), c.color(), c.snapshotUrl()))
        .toList());
  }
}

