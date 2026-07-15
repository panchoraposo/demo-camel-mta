package com.demo.wind.kafka.producer.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.demo.wind.kafka.producer.state.WindProducerState;

@Path("/api/snapshot")
@Produces(MediaType.APPLICATION_JSON)
public class SnapshotResource {

  @Inject
  WindProducerState state;

  @GET
  public WindProducerState.Snapshot snapshot() {
    return state.snapshot();
  }
}

