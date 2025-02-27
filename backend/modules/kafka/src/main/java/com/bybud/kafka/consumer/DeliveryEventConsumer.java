package com.bybud.kafka.consumer;

import com.bybud.entity.model.DeliveryStatus;
import com.bybud.entity.repository.DeliveryRepository;
import com.bybud.kafka.config.KafkaTopicsConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumer for delivery-related Kafka events
 */
@Component
public class DeliveryEventConsumer extends BaseKafkaConsumer {
    private final DeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsConfig topicsConfig;
    private final Map<String, Object> consumerConfigs;
    private final Map<String, ReceiverOptions<String, String>> topicOptions;
    private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();

    public DeliveryEventConsumer(
            @Qualifier("userConsumerConfigs") Map<String, Object> consumerConfigs,
            DeliveryRepository deliveryRepository,
            ObjectMapper objectMapper,
            KafkaTopicsConfig topicsConfig) {
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
        this.topicsConfig = topicsConfig;
        this.consumerConfigs = consumerConfigs;
        this.topicOptions = createTopicOptions();
    }

    @Override
    protected String getDefaultConsumerGroup() {
        return "delivery-consumer-group";
    }

    // Kafka Topic Configuration
    private Map<String, ReceiverOptions<String, String>> createTopicOptions() {
        Map<String, ReceiverOptions<String, String>> options = new HashMap<>();

        try {
            // Get topic names with explicit null checks
            String deliveryCreatedTopic = topicsConfig.getDeliveryCreatedTopic();
            String deliveryStatusUpdatedTopic = topicsConfig.getDeliveryStatusUpdatedTopic();

            logger.info("Configuring Kafka receivers for delivery topics: {} and {}",
                    deliveryCreatedTopic, deliveryStatusUpdatedTopic);

            // Add topics to options map
            if (deliveryCreatedTopic != null) {
                options.put(deliveryCreatedTopic, createConsumerOptions(consumerConfigs, deliveryCreatedTopic));
            }

            if (deliveryStatusUpdatedTopic != null) {
                options.put(deliveryStatusUpdatedTopic, createConsumerOptions(consumerConfigs, deliveryStatusUpdatedTopic));
            }
        } catch (Exception e) {
            logger.error("Error creating delivery topic options: {}", e.getMessage(), e);
        }

        return options;
    }

    // Kafka Message Processing
    @PostConstruct
    public void startListeners() {
        topicOptions.keySet().forEach(this::startTopicListener);
    }

    private void startTopicListener(String topic) {
        ReceiverOptions<String, String> options = topicOptions.get(topic);
        if (options == null) {
            logger.error("No receiver options found for topic: {}", topic);
            return;
        }

        try {
            Disposable subscription = KafkaReceiver.create(options)
                    .receive()
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(record -> {
                        logger.debug("Received delivery event from topic {}: {}", record.topic(), record.value());
                        processRecord(topic, record.value())
                                .doFinally(signalType -> record.receiverOffset().acknowledge())
                                .subscribe(
                                        success -> {},
                                        error -> logger.error("Error processing delivery record: {}", error.getMessage())
                                );
                    })
                    .doOnError(error -> logger.error("Error in Kafka delivery receiver: {}", error.getMessage()))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10)))
                    .subscribe();

            subscriptions.put(topic, subscription);
            logger.info("Started delivery listener for topic: {}", topic);
        } catch (Exception e) {
            logger.error("Failed to start delivery listener for topic {}: {}", topic, e.getMessage(), e);
        }
    }

    private Mono<Void> processRecord(String topic, String value) {
        if (topic.equals(topicsConfig.getDeliveryCreatedTopic())) {
            return handleDeliveryCreation(value).then();
        } else if (topic.equals(topicsConfig.getDeliveryStatusUpdatedTopic())) {
            return handleDeliveryStatusUpdate(value).then();
        }
        return Mono.empty();
    }

    // Event Data Processing
    private Mono<Void> handleDeliveryCreation(String eventData) {
        return parseEventDataMono(eventData)
                .flatMap(data -> {
                    String deliveryId = (String) data.get("deliveryId");
                    String customerId = (String) data.get("customerId");

                    logger.info("Processing delivery creation event for deliveryId: {}, customerId: {}",
                            deliveryId, customerId);

                    return deliveryRepository.findById(deliveryId)
                            .doOnNext(delivery -> logger.info("Delivery exists in database: {}", delivery.getId()))
                            .switchIfEmpty(
                                    Mono.defer(() -> {
                                        logger.info("Delivery {} not found in repository, no action needed", deliveryId);
                                        return Mono.empty();
                                    })
                            );
                })
                .then()
                .doOnSuccess(v -> logger.info("Successfully processed delivery creation event"))
                .doOnError(error -> logger.error("Failed to process delivery creation event: {}", error.getMessage()));
    }

    private Mono<Void> handleDeliveryStatusUpdate(String eventData) {
        return parseEventDataMono(eventData)
                .flatMap(data -> {
                    String deliveryId = (String) data.get("deliveryId");
                    String newStatus = (String) data.get("newStatus");

                    logger.info("Processing delivery status update event for deliveryId: {}, new status: {}",
                            deliveryId, newStatus);

                    // If a valid status is provided, attempt to update it
                    DeliveryStatus status = null;
                    try {
                        if (newStatus != null) {
                            status = DeliveryStatus.valueOf(newStatus);
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid delivery status received: {}", newStatus);
                    }

                    final DeliveryStatus finalStatus = status;

                    if (finalStatus != null) {
                        return deliveryRepository.findById(deliveryId)
                                .flatMap(delivery -> {
                                    delivery.setStatus(finalStatus);
                                    logger.info("Updating delivery {} status to {}", delivery.getId(), finalStatus);
                                    return deliveryRepository.save(delivery);
                                })
                                .switchIfEmpty(
                                        Mono.defer(() -> {
                                            logger.warn("Delivery {} not found for status update, no action taken", deliveryId);
                                            return Mono.empty();
                                        })
                                );
                    } else {
                        return Mono.empty();
                    }
                })
                .then()
                .doOnSuccess(v -> logger.info("Successfully processed delivery status update event"))
                .doOnError(error -> logger.error("Failed to process delivery status update event: {}", error.getMessage()));
    }

    private Mono<Map<String, Object>> parseEventDataMono(String eventData) {
        return Mono.fromCallable(() -> {
            try {
                // Parse the event data as a generic Map instead of a specific class
                JsonNode rootNode = objectMapper.readTree(eventData);
                Map<String, Object> result = new HashMap<>();

                // Extract common fields
                if (rootNode.has("eventType")) {
                    result.put("eventType", rootNode.get("eventType").asText());
                }

                if (rootNode.has("deliveryId")) {
                    result.put("deliveryId", rootNode.get("deliveryId").asText());
                }

                if (rootNode.has("customerId")) {
                    result.put("customerId", rootNode.get("customerId").asText());
                }

                if (rootNode.has("newStatus")) {
                    result.put("newStatus", rootNode.get("newStatus").asText());
                }

                if (rootNode.has("timestamp")) {
                    result.put("timestamp", rootNode.get("timestamp").asLong());
                }

                // Check if we have the minimum required field
                if (!result.containsKey("deliveryId")) {
                    throw new IllegalArgumentException("Event missing required field deliveryId");
                }

                return result;
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse delivery event data: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid delivery event data format", e);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        subscriptions.values().forEach(subscription -> {
            try {
                subscription.dispose();
            } catch (Exception e) {
                logger.error("Error disposing delivery subscription: {}", e.getMessage());
            }
        });
        subscriptions.clear();
        logger.info("Cleaned up all delivery Kafka subscriptions");
    }
}