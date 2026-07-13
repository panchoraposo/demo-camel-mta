package com.demo.modern.producer.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.demo.modern.producer.state.ProducerState;

@Path("/api/snapshot")
@Produces(MediaType.APPLICATION_JSON)
public class SnapshotResource {

  @Inject
  ProducerState state;

  @GET
  public ProducerState.Snapshot snapshot() {
    return state.snapshot();
  }
}

