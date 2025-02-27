package com.bybud.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaSender<String, String> kafkaSender, ObjectMapper objectMapper) {
        this.kafkaSender = kafkaSender;
        this.objectMapper = objectMapper;
        logger.info("KafkaProducerService initialized with sender: {}",
                kafkaSender != null ? "Valid KafkaSender" : "NULL KafkaSender");
    }

    public Mono<Void> sendMessage(String topic, Object message) {
        if (topic == null || topic.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Topic name cannot be null or empty"));
        }

        if (message == null) {
            return Mono.error(new IllegalArgumentException("Message cannot be null"));
        }

        if (kafkaSender == null) {
            logger.error("KafkaSender is null - cannot send message to topic: {}", topic);
            return Mono.error(new IllegalStateException("KafkaSender not initialized"));
        }

        logger.debug("Sending message to topic {}: {}", topic, message);

        return Mono.fromCallable(() -> serializeMessage(message))
                .flatMap(messageJson -> sendToKafka(topic, messageJson))
                .doOnSuccess(result -> logger.debug("Message sent to topic {}", topic))
                .doOnError(error -> logger.error("Failed to send message to topic {}: {}", topic, error.getMessage(), error))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(2)))
                .then();  // Convert to Mono<Void>
    }

    public Mono<Void> sendMessage(String topic, String key, Object message) {
        if (topic == null || topic.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Topic name cannot be null or empty"));
        }

        if (message == null) {
            return Mono.error(new IllegalArgumentException("Message cannot be null"));
        }

        if (kafkaSender == null) {
            logger.error("KafkaSender is null - cannot send message to topic: {}", topic);
            return Mono.error(new IllegalStateException("KafkaSender not initialized"));
        }

        logger.debug("Sending message to topic {} with key {}: {}", topic, key, message);

        return Mono.fromCallable(() -> serializeMessage(message))
                .flatMap(messageJson -> sendToKafka(topic, key, messageJson))
                .doOnSuccess(result -> logger.debug("Message sent to topic {} with key {}", topic, key))
                .doOnError(error -> logger.error("Failed to send message to topic {} with key {}: {}",
                        topic, key, error.getMessage(), error))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(2)))
                .then();  // Convert to Mono<Void>
    }

    private String serializeMessage(Object message) {
        try {
            if (message instanceof String) {
                return (String) message;
            }
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.error("Failed to serialize message: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to serialize message", e);
        }
    }

    private Mono<SenderResult<Void>> sendToKafka(String topic, String message) {
        return sendToKafka(topic, null, message);
    }

    private Mono<SenderResult<Void>> sendToKafka(String topic, String key, String message) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, message);

        return kafkaSender.send(Mono.just(SenderRecord.<String, String, Void>create(producerRecord, null)))
                .next()
                .doOnNext(result -> logger.debug("Message sent to partition {} offset {}",
                        result.recordMetadata().partition(), result.recordMetadata().offset()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}