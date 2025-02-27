package com.bybud.kafka.util;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Component
@DependsOn("kafkaAdmin")  // Explicitly declare dependency
public class KafkaTopicUtil {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicUtil.class);
    private final KafkaAdmin kafkaAdmin;

    @Autowired
    public KafkaTopicUtil(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
        logger.info("KafkaTopicUtil initialized");
    }

    /**
     * Creates a Kafka topic with the specified name, partitions, and replication factor
     */
    public Mono<Void> createTopic(String topicName, int partitions, int replicationFactor) {
        if (topicName == null || topicName.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Topic name cannot be null or empty"));
        }

        if (kafkaAdmin == null) {
            return Mono.error(new IllegalStateException("KafkaAdmin not initialized"));
        }

        return Mono.fromCallable(() -> {
                    Map<String, Object> configs = kafkaAdmin.getConfigurationProperties();
                    logger.debug("Creating topic {} with partitions={}, replicationFactor={}",
                            topicName, partitions, replicationFactor);

                    try (AdminClient adminClient = AdminClient.create(configs)) {
                        NewTopic topic = new NewTopic(topicName, partitions, (short) replicationFactor);
                        adminClient.createTopics(Collections.singleton(topic)).all().get();
                        return true;
                    } catch (Exception e) {
                        logger.error("Failed to create Kafka topic {}: {}", topicName, e.getMessage());
                        throw new RuntimeException("Failed to create Kafka topic: " + topicName, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> logger.info("Successfully created Kafka topic: {}", topicName))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(e -> !(e instanceof IllegalArgumentException))
                        .maxBackoff(Duration.ofSeconds(5)))
                .then();
    }

    /**
     * Checks if a Kafka topic exists
     */
    public Mono<Boolean> topicExists(String topicName) {
        if (topicName == null || topicName.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Topic name cannot be null or empty"));
        }

        if (kafkaAdmin == null) {
            return Mono.error(new IllegalStateException("KafkaAdmin not initialized"));
        }

        return Mono.fromCallable(() -> {
                    Map<String, Object> configs = kafkaAdmin.getConfigurationProperties();
                    try (AdminClient adminClient = AdminClient.create(configs)) {
                        boolean exists = adminClient.listTopics().names().get().contains(topicName);
                        logger.debug("Topic {} exists: {}", topicName, exists);
                        return exists;
                    } catch (Exception e) {
                        logger.error("Failed to check if Kafka topic exists {}: {}", topicName, e.getMessage());
                        throw new RuntimeException("Failed to check if Kafka topic exists: " + topicName, e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(e -> !(e instanceof IllegalArgumentException))
                        .maxBackoff(Duration.ofSeconds(5)));
    }

    /**
     * Creates a topic if it doesn't exist
     */
    public Mono<Void> createTopicIfNotExists(String topicName, int partitions, int replicationFactor) {
        return topicExists(topicName)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        logger.info("Topic {} already exists", topicName);
                        return Mono.empty();
                    } else {
                        return createTopic(topicName, partitions, replicationFactor);
                    }
                });
    }
}