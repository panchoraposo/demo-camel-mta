package com.demo.legacy.producer.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.demo.legacy.producer.model.DemoEvent;
import com.demo.legacy.producer.service.ProducerService;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProducerResource {

  public static class SendRequest {
    public String payload;
  }

  @Inject
  ProducerService service;

  @POST
  @Path("/send")
  public DemoEvent send(SendRequest req) throws Exception {
    String payload = (req != null && req.payload != null) ? req.payload : "hello";
    return service.sendOne(payload);
  }

  @POST
  @Path("/send/{count}")
  public DemoEvent[] sendMany(@PathParam("count") int count, SendRequest req) throws Exception {
    int n = Math.max(1, Math.min(count, 200));
    String payload = (req != null && req.payload != null) ? req.payload : "hello";
    DemoEvent[] out = new DemoEvent[n];
    for (int i = 0; i < n; i++) {
      out[i] = service.sendOne(payload);
    }
    return out;
  }
}

