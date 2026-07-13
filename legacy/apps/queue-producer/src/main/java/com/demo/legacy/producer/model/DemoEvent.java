package com.demo.legacy.producer.model;

public class DemoEvent {
  public String eventId;
  public String producerId;
  public String createdAt;
  public String payload;

  public DemoEvent() {}

  public DemoEvent(String eventId, String producerId, String createdAt, String payload) {
    this.eventId = eventId;
    this.producerId = producerId;
    this.createdAt = createdAt;
    this.payload = payload;
  }
}

