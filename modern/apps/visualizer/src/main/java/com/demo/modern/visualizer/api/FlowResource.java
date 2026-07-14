package com.demo.modern.visualizer.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("/api/flow")
@Produces(MediaType.APPLICATION_JSON)
public class FlowResource {

  @ConfigProperty(name = "demo.flow.producer-snapshot-url", defaultValue = "http://modern-queue-producer:8080/api/snapshot")
  String producerSnapshotUrl;

  @ConfigProperty(name = "demo.flow.consumer-snapshot-url", defaultValue = "http://modern-event-consumer:8081/api/snapshot")
  String consumerSnapshotUrl;

  @ConfigProperty(name = "demo.flow.consumer2-snapshot-url", defaultValue = "http://modern-kaoto-consumer:8083/api/snapshot")
  String consumer2SnapshotUrl;

  @ConfigProperty(name = "demo.flow.consumer1-name", defaultValue = "event-consumer")
  String consumer1Name;

  @ConfigProperty(name = "demo.flow.consumer2-name", defaultValue = "kaoto-consumer")
  String consumer2Name;

  @ConfigProperty(name = "demo.flow.broker-jolokia-url", defaultValue = "http://amq-broker-wconsj-0-svc:8161/console/jolokia")
  String brokerJolokiaUrl;

  @ConfigProperty(name = "demo.flow.broker-username", defaultValue = "admin")
  String brokerUsername;

  @ConfigProperty(name = "demo.flow.broker-password", defaultValue = "admin")
  String brokerPassword;

  @ConfigProperty(name = "demo.flow.queue", defaultValue = "demo.events")
  String queueName;

  @Inject
  ObjectMapper mapper;

  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  @GET
  public JsonNode flow() {
    ObjectNode root = mapper.createObjectNode();
    root.put("fetchedAt", Instant.now().toString());
    root.put("queue", queueName);

    ObjectNode status = mapper.createObjectNode();
    root.set("status", status);

    // producer
    root.set("producer", fetchJson(producerSnapshotUrl, status, "producerOk", "producerError"));

    // consumers (two)
    JsonNode consumer1 = fetchJson(consumerSnapshotUrl, status, "consumer1Ok", "consumer1Error");
    JsonNode consumer2 = fetchJson(consumer2SnapshotUrl, status, "consumer2Ok", "consumer2Error");

    // Backward-compatible fields
    root.set("consumer", consumer1);
    root.set("consumer2", consumer2);
    status.put("consumerOk", status.path("consumer1Ok").asBoolean(false) && status.path("consumer2Ok").asBoolean(false));

    ArrayNode consumers = mapper.createArrayNode();
    ObjectNode c1 = mapper.createObjectNode();
    c1.put("id", "consumer1");
    c1.put("name", consumer1Name);
    c1.set("snapshot", consumer1);
    consumers.add(c1);
    ObjectNode c2 = mapper.createObjectNode();
    c2.put("id", "consumer2");
    c2.put("name", consumer2Name);
    c2.set("snapshot", consumer2);
    consumers.add(c2);
    root.set("consumers", consumers);

    // broker jolokia queue metrics
    root.set("broker", fetchBrokerQueueMetrics(status));

    return root;
  }

  private JsonNode fetchJson(String url, ObjectNode status, String okField, String errField) {
    try {
      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(2))
          .GET()
          .build();
      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() / 100 != 2) {
        status.put(okField, false);
        status.put(errField, "HTTP " + res.statusCode());
        return mapper.nullNode();
      }
      status.put(okField, true);
      return mapper.readTree(res.body());
    } catch (Exception e) {
      status.put(okField, false);
      status.put(errField, e.getClass().getSimpleName() + ": " + e.getMessage());
      return mapper.nullNode();
    }
  }

  private JsonNode fetchBrokerQueueMetrics(ObjectNode status) {
    try {
      JolokiaClient jolokia = new JolokiaClient(brokerJolokiaUrl, brokerUsername, brokerPassword, mapper, client);

      List<String> mbeans = jolokia.search(
          "org.apache.activemq.artemis:broker=*,component=addresses,address=*,subcomponent=queues,routing-type=*,queue=*");

      String mbean = mbeans.stream().filter(s -> s.contains("queue=\"" + queueName + "\"")).findFirst().orElse(null);
      if (mbean == null) {
        status.put("brokerOk", false);
        status.put("brokerError", "Queue mbean not found for queue=" + queueName);
        return mapper.nullNode();
      }

      JsonNode read = jolokia.read(mbean, List.of(
          "MessageCount",
          "MessagesAdded",
          "MessagesAcknowledged",
          "DeliveringCount",
          "ConsumerCount"));

      status.put("brokerOk", true);
      ObjectNode out = mapper.createObjectNode();
      out.put("mbean", mbean);
      out.set("metrics", read);
      return out;
    } catch (Exception e) {
      status.put("brokerOk", false);
      status.put("brokerError", e.getClass().getSimpleName() + ": " + e.getMessage());
      return mapper.nullNode();
    }
  }

  static class JolokiaClient {
    private final URI base;
    private final String authHeader;
    private final ObjectMapper mapper;
    private final HttpClient client;

    JolokiaClient(String url, String user, String pass, ObjectMapper mapper, HttpClient client) {
      this.base = URI.create(url);
      String token = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
      this.authHeader = "Basic " + token;
      this.mapper = mapper;
      this.client = client;
    }

    List<String> search(String mbeanPattern) throws Exception {
      ObjectNode req = mapper.createObjectNode();
      req.put("type", "search");
      req.put("mbean", mbeanPattern);
      JsonNode res = post(req);
      ArrayNode value = (ArrayNode) res.get("value");
      if (value == null) return List.of();
      return mapper.convertValue(value, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    JsonNode read(String mbean, List<String> attributes) throws Exception {
      ObjectNode req = mapper.createObjectNode();
      req.put("type", "read");
      req.put("mbean", mbean);
      ArrayNode attrs = mapper.createArrayNode();
      for (String a : attributes) attrs.add(a);
      req.set("attribute", attrs);

      JsonNode res = post(req);
      JsonNode value = res.get("value");
      return value == null ? mapper.nullNode() : value;
    }

    private JsonNode post(JsonNode body) throws Exception {
      HttpRequest req = HttpRequest.newBuilder()
          .uri(base)
          .timeout(Duration.ofSeconds(3))
          .header("Content-Type", "application/json")
          .header("Authorization", authHeader)
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
          .build();

      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() / 100 != 2) {
        throw new RuntimeException("Jolokia HTTP " + res.statusCode());
      }
      JsonNode node = mapper.readTree(res.body());
      JsonNode status = node.get("status");
      if (status != null && status.asInt() != 200) {
        throw new RuntimeException("Jolokia status=" + status.asInt());
      }
      return node;
    }
  }
}

