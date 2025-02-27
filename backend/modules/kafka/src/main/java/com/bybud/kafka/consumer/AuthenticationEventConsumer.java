package com.bybud.kafka.consumer;

import com.bybud.security.service.ReactiveTokenService;
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

@Component
public class AuthenticationEventConsumer extends BaseKafkaConsumer {

    private final ReactiveTokenService tokenService;
    private final ObjectMapper objectMapper;
    private final Map<String, ReceiverOptions<String, String>> topicOptions = new HashMap<>();
    private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();

    private static final String TOKEN_INVALIDATED_TOPIC = "auth-token-invalidated-topic";
    private static final String USER_LOGOUT_TOPIC = "auth-user-logout-topic";

    public AuthenticationEventConsumer(
            @Qualifier("authConsumerConfigs") Map<String, Object> consumerConfigs,
            ReactiveTokenService tokenService,
            ObjectMapper objectMapper) {

        this.tokenService = tokenService;
        this.objectMapper = objectMapper;

        // Create receiver options for auth topics
        try {
            logger.info("Configuring Kafka receivers for auth topics: {} and {}",
                    TOKEN_INVALIDATED_TOPIC, USER_LOGOUT_TOPIC);

            topicOptions.put(TOKEN_INVALIDATED_TOPIC,
                    createConsumerOptions(consumerConfigs, TOKEN_INVALIDATED_TOPIC));

            topicOptions.put(USER_LOGOUT_TOPIC,
                    createConsumerOptions(consumerConfigs, USER_LOGOUT_TOPIC));
        } catch (Exception e) {
            logger.error("Error creating auth topic options: {}", e.getMessage(), e);
        }
    }

    @Override
    protected String getDefaultConsumerGroup() {
        return "auth-consumer-group";
    }

    @PostConstruct
    public void startListeners() {
        try {
            startTopicListener(TOKEN_INVALIDATED_TOPIC);
            startTopicListener(USER_LOGOUT_TOPIC);
        } catch (Exception e) {
            logger.error("Error starting auth listeners: {}", e.getMessage(), e);
        }
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
                        logger.debug("Received auth event from topic {}: {}", record.topic(), record.value());
                        processRecord(record.topic(), record.value())
                                .doFinally(signalType -> record.receiverOffset().acknowledge())
                                .subscribe(
                                        success -> {},
                                        error -> logger.error("Error processing auth record: {}", error.getMessage())
                                );
                    })
                    .doOnError(error -> logger.error("Error in Kafka auth receiver: {}", error.getMessage()))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10)))
                    .subscribe();

            subscriptions.put(topic, subscription);
            logger.info("Started auth listener for topic: {}", topic);
        } catch (Exception e) {
            logger.error("Failed to start listener for topic {}: {}", topic, e.getMessage(), e);
        }
    }

    private Mono<Void> processRecord(String topic, String value) {
        if (topic.equals(TOKEN_INVALIDATED_TOPIC)) {
            return handleTokenInvalidation(value).then();
        } else if (topic.equals(USER_LOGOUT_TOPIC)) {
            return handleUserLogout(value).then();
        }
        return Mono.empty();
    }

    // Event Processing
    private Mono<Void> handleTokenInvalidation(String eventData) {
        return parseEventDataMono(eventData)
                .flatMap(data -> {
                    String tokenSignature = (String) data.get("tokenSignature");
                    String username = (String) data.get("username");
                    String reason = (String) data.getOrDefault("reason", "unspecified");

                    if (tokenSignature == null || tokenSignature.isEmpty()) {
                        logger.warn("Token invalidation event received without token signature");
                        return Mono.empty();
                    }

                    logger.info("Processing token invalidation for user {}, reason: {}", username, reason);

                    // Blacklist the token
                    return tokenService.blacklistToken(tokenSignature, Duration.ofDays(7))
                            .doOnSuccess(v -> logger.info("Successfully blacklisted token for user: {}", username))
                            .doOnError(e -> logger.error("Failed to blacklist token: {}", e.getMessage()));
                })
                .then()
                .doOnError(e -> logger.error("Error handling token invalidation: {}", e.getMessage()));
    }

    private Mono<Void> handleUserLogout(String eventData) {
        return parseEventDataMono(eventData)
                .flatMap(data -> {
                    String tokenSignature = (String) data.get("tokenSignature");
                    String username = (String) data.get("username");

                    if (tokenSignature == null || tokenSignature.isEmpty()) {
                        logger.warn("User logout event received without token signature");
                        return Mono.empty();
                    }

                    logger.info("Processing logout for user: {}", username);

                    // Blacklist the token on logout
                    return tokenService.blacklistToken(tokenSignature, Duration.ofDays(7))
                            .doOnSuccess(v -> logger.info("Successfully blacklisted token on logout for user: {}", username))
                            .doOnError(e -> logger.error("Failed to blacklist token on logout: {}", e.getMessage()));
                })
                .then()
                .doOnError(e -> logger.error("Error handling user logout: {}", e.getMessage()));
    }

    private Mono<Map<String, Object>> parseEventDataMono(String eventData) {
        return Mono.fromCallable(() -> {
            try {
                // Parse the event data as a generic Map instead of expecting specific structure
                JsonNode rootNode = objectMapper.readTree(eventData);
                Map<String, Object> result = new HashMap<>();

                // Extract fields that might be in the event
                if (rootNode.has("eventType")) {
                    result.put("eventType", rootNode.get("eventType").asText());
                }

                if (rootNode.has("userId")) {
                    result.put("userId", rootNode.get("userId").asText());
                }

                if (rootNode.has("username")) {
                    result.put("username", rootNode.get("username").asText());
                }

                if (rootNode.has("tokenSignature")) {
                    result.put("tokenSignature", rootNode.get("tokenSignature").asText());
                }

                if (rootNode.has("reason")) {
                    result.put("reason", rootNode.get("reason").asText());
                }

                if (rootNode.has("oldTokenSignature")) {
                    result.put("oldTokenSignature", rootNode.get("oldTokenSignature").asText());
                }

                if (rootNode.has("newTokenSignature")) {
                    result.put("newTokenSignature", rootNode.get("newTokenSignature").asText());
                }

                if (rootNode.has("timestamp")) {
                    result.put("timestamp", rootNode.get("timestamp").asLong());
                }

                return result;
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse auth event data: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid auth event data format", e);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        subscriptions.values().forEach(subscription -> {
            try {
                subscription.dispose();
            } catch (Exception e) {
                logger.error("Error disposing auth subscription: {}", e.getMessage());
            }
        });
        subscriptions.clear();
        logger.info("Cleaned up all auth Kafka subscriptions");
    }
}