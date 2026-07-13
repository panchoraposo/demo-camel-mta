import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import jakarta.inject.Inject;

import com.demo.modern.visualizer.config.VisualizerClustersConfig;
import com.demo.modern.visualizer.config.VisualizerClustersConfig.Cluster;

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

