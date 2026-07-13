package com.demo.modern.producer.jms;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JmsConnectionFactoryProducer {

  @ConfigProperty(name = "demo.broker.url")
  String url;

  @ConfigProperty(name = "demo.broker.username")
  String username;

  @ConfigProperty(name = "demo.broker.password")
  String password;

  @Produces
  @ApplicationScoped
  public ConnectionFactory connectionFactory() {
    return new ActiveMQConnectionFactory(url, username, password);
  }
}

