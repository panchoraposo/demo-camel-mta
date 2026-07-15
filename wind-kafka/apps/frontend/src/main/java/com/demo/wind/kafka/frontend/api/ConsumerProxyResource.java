package com.demo.wind.kafka.frontend.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.common.annotation.Blocking;

@Path("/api/consumer")
public class ConsumerProxyResource {

  @ConfigProperty(name = "demo.consumer.base-url")
  String consumerBaseUrl;

  static final HttpClient CLIENT = HttpClient.newBuilder().build();

  @GET
  @Path("/snapshot")
  @Produces(MediaType.APPLICATION_JSON)
  @Blocking
  public Response snapshot() {
    String base = consumerBaseUrl == null ? "" : consumerBaseUrl.replaceAll("/+$", "");
    String url = base + "/api/snapshot";

    try {
      HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
      HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
      return Response.status(resp.statusCode()).type(MediaType.APPLICATION_JSON).entity(resp.body()).build();
    } catch (Exception e) {
      return Response.status(503)
          .type(MediaType.APPLICATION_JSON)
          .entity("{\"error\":\"consumer proxy failed\"}")
          .build();
    }
  }
}

