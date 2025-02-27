package com.bybud.kafka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

// Remove @Component annotation to avoid duplicate bean creation
@ConfigurationProperties(prefix = "bybud.kafka")
public class KafkaTopicsConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicsConfig.class);

    // Initialize with an empty map to prevent NullPointerException
    private Map<String, String> topics = new HashMap<>();

    public Map<String, String> getTopics() {
        return topics;
    }

    public void setTopics(Map<String, String> topics) {
        this.topics = topics != null ? topics : new HashMap<>();
    }

    // Add null safety to all topic getters with default values
    public String getUserRegisteredTopic() {
        return topics.getOrDefault("user-registered", "user-registered-topic");
    }

    public String getUserProfileUpdatesTopic() {
        return topics.getOrDefault("user-profile-updates", "user-profile-updates-topic");
    }

    public String getDeliveryCreatedTopic() {
        return topics.getOrDefault("delivery-created", "delivery-created-topic");
    }

    public String getDeliveryStatusUpdatedTopic() {
        return topics.getOrDefault("delivery-status-updated", "delivery-status-updated-topic");
    }

    public String getUserAuthenticatedTopic() {
        return topics.getOrDefault("auth-user-authenticated", "auth-user-authenticated-topic");
    }

    public String getUserLogoutTopic() {
        return topics.getOrDefault("auth-user-logout", "auth-user-logout-topic");
    }

    public String getTokenRefreshedTopic() {
        return topics.getOrDefault("auth-token-refreshed", "auth-token-refreshed-topic");
    }

    public String getTokenInvalidatedTopic() {
        return topics.getOrDefault("auth-token-invalidated", "auth-token-invalidated-topic");
    }

    public String getAccountLockoutTopic() {
        return topics.getOrDefault("auth-account-lockout", "auth-account-lockout-topic");
    }

    @PostConstruct
    public void logTopics() {
        logger.info("Kafka Topics Config Loaded: {}", topics);
    }
}