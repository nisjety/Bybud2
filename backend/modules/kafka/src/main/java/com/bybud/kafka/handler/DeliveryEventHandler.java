package com.bybud.kafka.handler;

import com.bybud.kafka.config.KafkaTopicsConfig;
import com.bybud.kafka.event.BaseEventHandler;
import com.bybud.kafka.event.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class DeliveryEventHandler extends BaseEventHandler {
    private final ApplicationEventPublisher applicationEventPublisher;

    // Event Classes
        public record DeliveryCreatedEvent(String deliveryId, String customerId) {
    }

    public record DeliveryStatusUpdatedEvent(String deliveryId, String newStatus) {
    }

    public DeliveryEventHandler(
            EventPublisher eventPublisher,
            KafkaTopicsConfig topicsConfig,
            ApplicationEventPublisher applicationEventPublisher) {
        super(eventPublisher, topicsConfig);
        this.applicationEventPublisher = applicationEventPublisher;

        // Log available topic names at initialization
        logger.info("Delivery event handler initialized with topics: " +
                        "delivery-created={}, delivery-status-updated={}",
                topicsConfig.getDeliveryCreatedTopic(),
                topicsConfig.getDeliveryStatusUpdatedTopic());
    }

    // Publishing Methods
    public void publishDeliveryCreated(DeliveryCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishDeliveryStatusUpdated(DeliveryStatusUpdatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    // Event Listeners
    @EventListener
    public void handleDeliveryCreatedEvent(DeliveryCreatedEvent event) {
        Map<String, Object> eventData = createBaseEventData("DELIVERY_CREATED");
        eventData.put("deliveryId", event.deliveryId());
        eventData.put("customerId", event.customerId());

        String topic = topicsConfig.getDeliveryCreatedTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for delivery created events, using default");
            topic = "delivery-created-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "delivery creation",
                event.deliveryId()
        ).onErrorResume(e -> {
            logger.error("Failed to publish delivery created event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }

    @EventListener
    public void handleDeliveryStatusUpdatedEvent(DeliveryStatusUpdatedEvent event) {
        Map<String, Object> eventData = createBaseEventData("DELIVERY_STATUS_UPDATED");
        eventData.put("deliveryId", event.deliveryId());
        eventData.put("newStatus", event.newStatus());

        String topic = topicsConfig.getDeliveryStatusUpdatedTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for delivery status updated events, using default");
            topic = "delivery-status-updated-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "delivery status update",
                event.deliveryId()
        ).onErrorResume(e -> {
            logger.error("Failed to publish delivery status updated event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }
}