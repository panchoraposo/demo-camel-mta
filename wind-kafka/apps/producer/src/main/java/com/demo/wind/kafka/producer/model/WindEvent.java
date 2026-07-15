package com.demo.wind.kafka.producer.model;

public class WindEvent {
  public String producerId;
  public long ts;
  public int speed; // 0..100
  public int gust; // 0..100
  public int watts; // arbitrary "power" derived from speed

  public WindEvent() {
  }

  public WindEvent(String producerId, long ts, int speed, int gust, int watts) {
    this.producerId = producerId;
    this.ts = ts;
    this.speed = speed;
    this.gust = gust;
    this.watts = watts;
  }
}

