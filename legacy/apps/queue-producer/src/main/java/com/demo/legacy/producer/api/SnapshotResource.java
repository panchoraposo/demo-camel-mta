package com.demo.legacy.producer.api;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.demo.legacy.producer.state.ProducerState;

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

