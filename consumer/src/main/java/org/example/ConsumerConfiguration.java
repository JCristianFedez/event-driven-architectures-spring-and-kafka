package org.example;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class ConsumerConfiguration {

  private Map<String, Object> consumerConfigs() {
    return Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
        ConsumerConfig.GROUP_ID_CONFIG, "tutorialGroup"
    );
  }

  @Bean
  public ConsumerFactory<String, TimestampEvent> consumerFactory() {
    final JsonDeserializer<TimestampEvent> timestampEventDeserializer = new JsonDeserializer<>(TimestampEvent.class);
    timestampEventDeserializer.setRemoveTypeHeaders(false);
    timestampEventDeserializer.addTrustedPackages("*");
    timestampEventDeserializer.setUseTypeMapperForKey(true);
    return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(), timestampEventDeserializer);
  }

  @Bean
  public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, TimestampEvent>> kafkaListenerContainerFactory() {
    final ConcurrentKafkaListenerContainerFactory<String, TimestampEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }

  @Bean
  public NewTopic timestampTopic() {
    return TopicBuilder.name("timestamp")
        .build();
  }

}
