package com.demo.legacy.consumer.jms;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.jms.ConnectionFactory;

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
  @Named("connectionFactory")
  public ConnectionFactory connectionFactory() {
    return new ActiveMQConnectionFactory(url, username, password);
  }
}

