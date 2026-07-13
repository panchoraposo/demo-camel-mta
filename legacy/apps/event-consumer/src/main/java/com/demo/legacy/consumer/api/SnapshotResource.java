package com.demo.legacy.consumer.api;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.demo.legacy.consumer.state.ConsumerState;

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

