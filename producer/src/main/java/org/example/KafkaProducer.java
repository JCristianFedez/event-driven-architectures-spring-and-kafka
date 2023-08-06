package org.example;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaProducer.class);

  @Autowired
  private KafkaTemplate<String, TimestampEvent> kafkaTemplate;

  @Scheduled(fixedRate = 5000)
  public void reportCurrentTime() {
    final TimestampEvent event = new TimestampEvent(ZonedDateTime.now());
    this.kafkaTemplate.send("timestamp", event);
    LOG.info("Sent: {}", event.timestamp());
  }
}
