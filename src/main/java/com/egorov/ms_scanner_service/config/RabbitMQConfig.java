package com.egorov.ms_scanner_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String SCAN_RESULTS_QUEUE = "scan.results.queue";
  public static final String SCAN_REQUESTS_QUEUE = "scan.requests.queue";
  public static final String SCAN_NOTIFICATIONS_QUEUE = "scan.notifications.queue";

  public static final String SCAN_EXCHANGE = "scan.exchange";

  public static final String SCAN_RESULT_RK = "scan.result";
  public static final String SCAN_REQUEST_RK = "scan.request";
  public static final String SCAN_NOTIFICATION_RK = "scan.notification";

  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    converter.setCreateMessageIds(true);
    return converter;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter());
    template.setMandatory(true);
    template.setReplyTimeout(10000);
    return template;
  }

  @Bean
  public TopicExchange scanExchange() {
    return ExchangeBuilder.topicExchange(SCAN_EXCHANGE)
        .durable(true)
        .build();
  }

  @Bean
  public Queue scanResultsQueue() {
    return QueueBuilder.durable(SCAN_RESULTS_QUEUE)
        .withArgument("x-max-priority", 5)
        .build();
  }

  @Bean
  public Queue scanRequestsQueue() {
    return QueueBuilder.durable(SCAN_REQUESTS_QUEUE).build();
  }

  @Bean
  public Queue scanNotificationsQueue() {
    return QueueBuilder.durable(SCAN_NOTIFICATIONS_QUEUE)
        .withArgument("x-message-ttl", 60000) // TTL 1 минута для уведомлений
        .withArgument("x-dead-letter-exchange", SCAN_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", "scan.notification.dlq")
        .build();
  }

  @Bean
  public Binding scanResultsBinding() {
    return BindingBuilder
        .bind(scanResultsQueue())
        .to(scanExchange())
        .with(SCAN_RESULT_RK);
  }

  @Bean
  public Binding scanRequestBinding() {
    return BindingBuilder
        .bind(scanRequestsQueue())
        .to(scanExchange())
        .with(SCAN_REQUEST_RK);
  }

  @Bean
  public Binding scanNotificationBinding() {
    return BindingBuilder
        .bind(scanNotificationsQueue())
        .to(scanExchange())
        .with(SCAN_NOTIFICATION_RK);
  }
}