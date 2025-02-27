package com.bybud.kafka.event;

import com.bybud.kafka.config.KafkaTopicsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseEventHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final EventPublisher eventPublisher;
    protected final KafkaTopicsConfig topicsConfig;

    protected BaseEventHandler(
            @Lazy EventPublisher eventPublisher,
            KafkaTopicsConfig topicsConfig) {
        this.eventPublisher = eventPublisher;
        this.topicsConfig = topicsConfig;
    }

    protected Mono<Void> publishEventWithLogging(String topic, Map<String, Object> eventData, String eventType, String identifier) {
        // Add common fields
        eventData.putIfAbsent("timestamp", System.currentTimeMillis());
        eventData.putIfAbsent("eventType", eventType);

        return eventPublisher.publishEvent(topic, eventData)
                .doOnSuccess(v -> logger.info("Published {} event for {}: {}", eventType, identifier, eventData))
                .doOnError(error -> logger.error("Failed to publish {} event for {}: {}",
                        eventType, identifier, error.getMessage()));
    }

    protected Map<String, Object> createBaseEventData(String eventType) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", eventType);
        eventData.put("timestamp", System.currentTimeMillis());
        return eventData;
    }
}