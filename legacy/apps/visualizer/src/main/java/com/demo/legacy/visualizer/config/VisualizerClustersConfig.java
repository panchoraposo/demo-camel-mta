package com.demo.legacy.visualizer.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class VisualizerClustersConfig {

  @ConfigProperty(name = "demo.ui.amq1-label", defaultValue = "AMQ1")
  String amq1Label;

  @ConfigProperty(name = "demo.ui.amq1-color", defaultValue = "#2563eb")
  String amq1Color;

  @ConfigProperty(name = "demo.consumers.amq1-snapshot-url", defaultValue = "")
  String amq1SnapshotUrl;

  @ConfigProperty(name = "demo.ui.amq2-label", defaultValue = "AMQ2")
  String amq2Label;

  @ConfigProperty(name = "demo.ui.amq2-color", defaultValue = "#16a34a")
  String amq2Color;

  @ConfigProperty(name = "demo.consumers.amq2-snapshot-url", defaultValue = "")
  String amq2SnapshotUrl;

  @ConfigProperty(name = "demo.ui.amq3-label", defaultValue = "AMQ3")
  String amq3Label;

  @ConfigProperty(name = "demo.ui.amq3-color", defaultValue = "#f97316")
  String amq3Color;

  @ConfigProperty(name = "demo.consumers.amq3-snapshot-url", defaultValue = "")
  String amq3SnapshotUrl;

  // Back-compat for the single-list version (comma-separated).
  @ConfigProperty(name = "demo.visualizer.snapshot-urls", defaultValue = "")
  String legacySnapshotUrls;

  public record Cluster(String id, String label, String color, String snapshotUrl) {
    public boolean hasSnapshotUrl() {
      return snapshotUrl != null && !snapshotUrl.isBlank();
    }
  }

  public List<Cluster> clusters() {
    boolean anyExplicit = !isBlank(amq1SnapshotUrl) || !isBlank(amq2SnapshotUrl) || !isBlank(amq3SnapshotUrl);
    if (anyExplicit) {
      return List.of(
          new Cluster("amq1", amq1Label, amq1Color, amq1SnapshotUrl),
          new Cluster("amq2", amq2Label, amq2Color, amq2SnapshotUrl),
          new Cluster("amq3", amq3Label, amq3Color, amq3SnapshotUrl));
    }

    // Fallback: map legacy list onto AMQ1..N
    List<String> urls = parseLegacyList(legacySnapshotUrls);
    List<Cluster> out = new ArrayList<>();
    if (urls.size() > 0)
      out.add(new Cluster("amq1", amq1Label, amq1Color, urls.get(0)));
    if (urls.size() > 1)
      out.add(new Cluster("amq2", amq2Label, amq2Color, urls.get(1)));
    if (urls.size() > 2)
      out.add(new Cluster("amq3", amq3Label, amq3Color, urls.get(2)));
    return out;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static List<String> parseLegacyList(String raw) {
    if (raw == null || raw.isBlank())
      return List.of();
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }
}

