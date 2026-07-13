package com.demo.modern.kaoto.consumer.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.demo.modern.kaoto.consumer.state.ConsumerState;

@Path("/api/snapshot")
@Produces(MediaType.APPLICATION_JSON)
public class SnapshotResource {

  @Inject
  ConsumerState state;

  @GET
  public ConsumerState.Snapshot snapshot() {
    return state.snapshot();
  }
}

