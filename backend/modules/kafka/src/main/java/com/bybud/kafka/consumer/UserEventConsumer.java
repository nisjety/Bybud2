package com.bybud.kafka.consumer;

import com.bybud.entity.mapper.UserMapper;
import com.bybud.entity.repository.UserRepository;
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
 * Consumer for user-related Kafka events
 */
@Component
public class UserEventConsumer extends BaseKafkaConsumer {
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsConfig topicsConfig;
    private final Map<String, ReceiverOptions<String, String>> topicOptions;
    private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Object> consumerConfigs;

    public UserEventConsumer(
            @Qualifier("userConsumerConfigs") Map<String, Object> consumerConfigs,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            KafkaTopicsConfig topicsConfig,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.topicsConfig = topicsConfig;
        this.consumerConfigs = consumerConfigs;
        this.topicOptions = createTopicOptions();
    }

    @Override
    protected String getDefaultConsumerGroup() {
        return "user-consumer-group";
    }

    // Kafka Topic Configuration
    private Map<String, ReceiverOptions<String, String>> createTopicOptions() {
        Map<String, ReceiverOptions<String, String>> options = new HashMap<>();

        try {
            // Get topic names, with explicit null checks
            String userRegisteredTopic = topicsConfig.getUserRegisteredTopic();
            String userProfileUpdatesTopic = topicsConfig.getUserProfileUpdatesTopic();

            logger.info("Configuring Kafka receivers for topics: {} and {}",
                    userRegisteredTopic, userProfileUpdatesTopic);

            // Add topics to options map
            if (userRegisteredTopic != null) {
                options.put(userRegisteredTopic, createConsumerOptions(consumerConfigs, userRegisteredTopic));
            }

            if (userProfileUpdatesTopic != null) {
                options.put(userProfileUpdatesTopic, createConsumerOptions(consumerConfigs, userProfileUpdatesTopic));
            }
        } catch (Exception e) {
            logger.error("Error creating topic options: {}", e.getMessage(), e);
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
                        logger.debug("Received user event from topic {}: {}", record.topic(), record.value());
                        processRecord(topic, record.value())
                                .doFinally(signalType -> record.receiverOffset().acknowledge())
                                .subscribe(
                                        success -> {},
                                        error -> logger.error("Error processing record: {}", error.getMessage())
                                );
                    })
                    .doOnError(error -> logger.error("Error in Kafka receiver: {}", error.getMessage()))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10)))
                    .subscribe();

            subscriptions.put(topic, subscription);
            logger.info("Started user listener for topic: {}", topic);
        } catch (Exception e) {
            logger.error("Failed to start listener for topic {}: {}", topic, e.getMessage(), e);
        }
    }

    private Mono<Void> processRecord(String topic, String value) {
        if (topic.equals(topicsConfig.getUserRegisteredTopic())) {
            return handleUserRegistration(value).then();
        } else if (topic.equals(topicsConfig.getUserProfileUpdatesTopic())) {
            return handleUserUpdate(value).then();
        }
        return Mono.empty();
    }

    // Event Data Processing
    private Mono<Void> handleUserRegistration(String eventData) {
        return parseEventDataMono(eventData)
                .flatMap(data -> {
                    String userId = (String) data.get("userId");
                    String username = (String) data.get("username");

                    logger.info("Processing user registration event for userId: {}, username: {}", userId, username);

                    // In a real implementation, you might fetch additional user data or
                    // create a minimal user record if it doesn't exist already
                    return userRepository.findById(userId)
                            .doOnNext(user -> logger.info("User exists in database: {}", user.getUsername()))
                            .switchIfEmpty(
                                    Mono.defer(() -> {
                                        logger.info("User {} not found in repository, no action needed", username);
                                        return Mono.empty();
                                    })
                            );
                })
                .then()
                .doOnSuccess(v -> logger.info("Successfully processed user registration event"))
                .doOnError(error -> logger.error("Failed to process user registration event: {}", error.getMessage()));
    }

    private Mono<Void> handleUserUpdate(String eventData) {
        return parseEventDataMono(eventData)
                .flatMap(data -> {
                    String userId = (String) data.get("userId");
                    String username = (String) data.get("username");

                    logger.info("Processing user update event for userId: {}, username: {}", userId, username);

                    // In a real implementation, you might update user status or sync data
                    return userRepository.findById(userId)
                            .doOnNext(user -> logger.info("User exists in database and will be synced: {}", user.getUsername()))
                            .switchIfEmpty(
                                    Mono.defer(() -> {
                                        logger.warn("User {} not found for update event, no action taken", username);
                                        return Mono.empty();
                                    })
                            );
                })
                .then()
                .doOnSuccess(v -> logger.info("Successfully processed user update event"))
                .doOnError(error -> logger.error("Failed to process user update event: {}", error.getMessage()));
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

                if (rootNode.has("userId")) {
                    result.put("userId", rootNode.get("userId").asText());
                }

                if (rootNode.has("username")) {
                    result.put("username", rootNode.get("username").asText());
                }

                if (rootNode.has("timestamp")) {
                    result.put("timestamp", rootNode.get("timestamp").asLong());
                }

                // Check if we have the minimum required fields
                if (!result.containsKey("userId") || !result.containsKey("username")) {
                    throw new IllegalArgumentException("Event missing required fields userId or username");
                }

                return result;
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse event data: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid event data format", e);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        subscriptions.values().forEach(subscription -> {
            try {
                subscription.dispose();
            } catch (Exception e) {
                logger.error("Error disposing subscription: {}", e.getMessage());
            }
        });
        subscriptions.clear();
        logger.info("Cleaned up all Kafka subscriptions");
    }
}