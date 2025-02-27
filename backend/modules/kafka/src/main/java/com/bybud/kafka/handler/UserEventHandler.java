package com.bybud.kafka.handler;

import com.bybud.kafka.config.KafkaTopicsConfig;
import com.bybud.kafka.event.BaseEventHandler;
import com.bybud.kafka.event.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Handler for user-related application events that publishes them to Kafka
 */
@Component
public class UserEventHandler extends BaseEventHandler {
    private final ApplicationEventPublisher applicationEventPublisher;

    // Event Classes
        public record UserCreatedEvent(String userId, String username) {
    }

    public record UserUpdatedEvent(String userId, String username) {
    }

    public UserEventHandler(
            EventPublisher eventPublisher,
            KafkaTopicsConfig topicsConfig,
            ApplicationEventPublisher applicationEventPublisher) {
        super(eventPublisher, topicsConfig);
        this.applicationEventPublisher = applicationEventPublisher;

        // Log available topic names at initialization
        logger.info("User event handler initialized with topics: " +
                        "user-registered={}, user-profile-updates={}",
                topicsConfig.getUserRegisteredTopic(),
                topicsConfig.getUserProfileUpdatesTopic());
    }

    // Publishing Methods
    public void publishUserCreated(UserCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishUserUpdated(UserUpdatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    // Event Listeners
    @EventListener
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        Map<String, Object> eventData = createBaseEventData("USER_CREATED");
        eventData.put("userId", event.userId());
        eventData.put("username", event.username());

        String topic = topicsConfig.getUserRegisteredTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for user created events, using default");
            topic = "user-registered-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "user creation",
                event.username()
        ).onErrorResume(e -> {
            logger.error("Failed to publish user created event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }

    @EventListener
    public void handleUserUpdatedEvent(UserUpdatedEvent event) {
        Map<String, Object> eventData = createBaseEventData("USER_UPDATED");
        eventData.put("userId", event.userId());
        eventData.put("username", event.username());

        String topic = topicsConfig.getUserProfileUpdatesTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for user updated events, using default");
            topic = "user-profile-updates-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "user update",
                event.username()
        ).onErrorResume(e -> {
            logger.error("Failed to publish user updated event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }
}