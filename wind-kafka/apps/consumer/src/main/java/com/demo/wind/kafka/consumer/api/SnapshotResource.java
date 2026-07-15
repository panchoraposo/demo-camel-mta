package com.demo.wind.kafka.consumer.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.demo.wind.kafka.consumer.state.WindConsumerState;

import io.smallrye.mutiny.Multi;

@Path("/api")
public class SnapshotResource {

  @Inject
  WindConsumerState state;

  @GET
  @Path("/snapshot")
  @Produces(MediaType.APPLICATION_JSON)
  public WindConsumerState.Snapshot snapshot() {
    return state.snapshot();
  }

  @GET
  @Path("/stream")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  public Multi<WindConsumerState.Snapshot> stream() {
    return state.stream();
  }
}

