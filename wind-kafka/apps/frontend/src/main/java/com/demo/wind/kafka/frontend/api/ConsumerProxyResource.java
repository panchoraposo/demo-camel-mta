package com.demo.wind.kafka.frontend.api;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

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

  @GET
  @Path("/snapshot")
  @Produces(MediaType.APPLICATION_JSON)
  @Blocking
  public Response snapshot() {
    String base = consumerBaseUrl == null ? "" : consumerBaseUrl.replaceAll("/+$", "");
    // Force new TCP connections so kube-proxy can balance across consumer pods.
    String url = base + "/api/snapshot?c=" + ThreadLocalRandom.current().nextInt(1_000_000);

    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
      conn.setRequestProperty("Connection", "close");
      conn.setConnectTimeout(2000);
      conn.setReadTimeout(5000);

      int code = conn.getResponseCode();
      InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
      byte[] bytes = is == null ? new byte[0] : is.readAllBytes();
      conn.disconnect();

      String body = new String(bytes, StandardCharsets.UTF_8);
      return Response.status(code).type(MediaType.APPLICATION_JSON).entity(body).build();
    } catch (Exception e) {
      return Response.status(503)
          .type(MediaType.APPLICATION_JSON)
          .entity("{\"error\":\"consumer proxy failed\"}")
          .build();
    }
  }
}

