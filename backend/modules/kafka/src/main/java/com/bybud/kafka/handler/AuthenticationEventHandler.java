package com.bybud.kafka.handler;

import com.bybud.entity.model.RoleName;
import com.bybud.kafka.config.KafkaTopicsConfig;
import com.bybud.kafka.event.BaseEventHandler;
import com.bybud.kafka.event.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationEventHandler extends BaseEventHandler {
    private final ApplicationEventPublisher applicationEventPublisher;

    // Event Classes
        public record UserAuthenticatedEvent(String userId, String username, Set<RoleName> roles, String tokenSignature) {
    }

    public record UserLogoutEvent(String userId, String username, String tokenSignature) {
    }

    public record TokenRefreshedEvent(String userId, String username, String oldTokenSignature,
                                      String newTokenSignature) {
    }

    public record TokenInvalidatedEvent(String userId, String username, String tokenSignature, String reason) {
    }

    public record AccountLockoutEvent(String username, int failedAttempts, String ipAddress) {
    }

    public AuthenticationEventHandler(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTopicsConfig topicsConfig,
            EventPublisher eventPublisher) {
        super(eventPublisher, topicsConfig);
        this.applicationEventPublisher = applicationEventPublisher;

        // Log available topic names at initialization
        logger.info("Authentication event handler initialized with topics: " +
                        "auth-user-authenticated={}, auth-user-logout={}, auth-token-refreshed={}, " +
                        "auth-token-invalidated={}, auth-account-lockout={}",
                topicsConfig.getUserAuthenticatedTopic(),
                topicsConfig.getUserLogoutTopic(),
                topicsConfig.getTokenRefreshedTopic(),
                topicsConfig.getTokenInvalidatedTopic(),
                topicsConfig.getAccountLockoutTopic());
    }

    // Publishing Methods
    public void publishUserAuthenticated(UserAuthenticatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishUserLogout(UserLogoutEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishTokenRefreshed(TokenRefreshedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishTokenInvalidated(TokenInvalidatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishAccountLockout(AccountLockoutEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    // Event Listeners
    @EventListener
    public void handleUserAuthenticatedEvent(UserAuthenticatedEvent event) {
        Map<String, Object> eventData = createBaseEventData("USER_AUTHENTICATED");
        eventData.put("username", event.username());
        eventData.put("userId", event.userId());
        eventData.put("roles", event.roles());
        eventData.put("tokenSignature", event.tokenSignature());

        String topic = topicsConfig.getUserAuthenticatedTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for user authenticated events, using default");
            topic = "auth-user-authenticated-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "user authentication",
                event.username()
        ).onErrorResume(e -> {
            logger.error("Failed to publish user authenticated event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }

    @EventListener
    public void handleUserLogoutEvent(UserLogoutEvent event) {
        Map<String, Object> eventData = createBaseEventData("USER_LOGOUT");
        eventData.put("username", event.username());
        eventData.put("userId", event.userId());
        eventData.put("tokenSignature", event.tokenSignature());

        String topic = topicsConfig.getUserLogoutTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for user logout events, using default");
            topic = "auth-user-logout-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "user logout",
                event.username()
        ).onErrorResume(e -> {
            logger.error("Failed to publish user logout event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }

    @EventListener
    public void handleTokenRefreshedEvent(TokenRefreshedEvent event) {
        Map<String, Object> eventData = createBaseEventData("TOKEN_REFRESHED");
        eventData.put("username", event.username());
        eventData.put("userId", event.userId());
        eventData.put("oldTokenSignature", event.oldTokenSignature());
        eventData.put("newTokenSignature", event.newTokenSignature());

        String topic = topicsConfig.getTokenRefreshedTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for token refreshed events, using default");
            topic = "auth-token-refreshed-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "token refresh",
                event.username()
        ).onErrorResume(e -> {
            logger.error("Failed to publish token refreshed event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }

    @EventListener
    public void handleTokenInvalidatedEvent(TokenInvalidatedEvent event) {
        Map<String, Object> eventData = createBaseEventData("TOKEN_INVALIDATED");
        eventData.put("username", event.username());
        eventData.put("userId", event.userId());
        eventData.put("tokenSignature", event.tokenSignature());
        eventData.put("reason", event.reason());

        String topic = topicsConfig.getTokenInvalidatedTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for token invalidated events, using default");
            topic = "auth-token-invalidated-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "token invalidation",
                event.username()
        ).onErrorResume(e -> {
            logger.error("Failed to publish token invalidated event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }

    @EventListener
    public void handleAccountLockoutEvent(AccountLockoutEvent event) {
        Map<String, Object> eventData = createBaseEventData("ACCOUNT_LOCKOUT");
        eventData.put("username", event.username());
        eventData.put("failedAttempts", event.failedAttempts());
        eventData.put("ipAddress", event.ipAddress());

        String topic = topicsConfig.getAccountLockoutTopic();
        if (topic == null || topic.isEmpty()) {
            logger.warn("No topic configured for account lockout events, using default");
            topic = "auth-account-lockout-topic"; // Fallback
        }

        publishEventWithLogging(
                topic,
                eventData,
                "account lockout",
                event.username()
        ).onErrorResume(e -> {
            logger.error("Failed to publish account lockout event: {}", e.getMessage(), e);
            return Mono.empty();
        }).subscribe();
    }
}