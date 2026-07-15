package com.demo.wind.kafka.consumer.model;

public class WindEvent {
  public String producerId;
  public long ts;
  public int speed; // 0..100
  public int gust; // 0..100
  public int watts; // arbitrary "power" derived from speed

  public WindEvent() {
  }
}

