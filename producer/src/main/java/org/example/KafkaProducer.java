package org.example;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);

  private final KafkaTemplate<String, String> kafkaTemplate;

  KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  void sendMessage(String message, String topicName) {
    this.kafkaTemplate.send(topicName, message);
  }

  @Scheduled(fixedRate = 5000)
  public void reportCurrentTime() {
    final String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
    sendMessage(timestamp, "timestamp");
    LOG.info("Sent: {}", timestamp);
  }
}
