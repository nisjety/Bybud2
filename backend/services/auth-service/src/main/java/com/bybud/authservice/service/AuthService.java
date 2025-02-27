/***
package com.bybud.authservice.service;

import com.bybud.common.exception.UserNotFoundException;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import com.bybud.entity.response.JwtResponse;
import com.bybud.kafka.config.KafkaTopicsConfig;
import com.bybud.kafka.producer.KafkaProducerService;
import com.bybud.security.config.JwtTokenProvider;
import com.bybud.security.service.ReactiveTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final ReactiveTokenService tokenService;
    private final KafkaProducerService kafkaProducerService;
    private final KafkaTopicsConfig kafkaTopicsConfig;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // Example TTLs: 15 min for access token, 7 days for refresh token
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    public AuthService(UserRepository userRepository,
                       ReactiveTokenService tokenService,
                       KafkaProducerService kafkaProducerService,
                       KafkaTopicsConfig kafkaTopicsConfig,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder,
                       ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.kafkaProducerService = kafkaProducerService;
        this.kafkaTopicsConfig = kafkaTopicsConfig;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    /**
     * Login:
     * 1) Find user by username or email.
     * 2) Validate password.
     * 3) Generate access & refresh tokens.
     * 4) Store them in Redis (Dragonfly) with TTL.
     * 5) Send Kafka event for successful login.
     */

/*
    public Mono<JwtResponse> login(String usernameOrEmail, String password) {
        return Mono.firstWithSignal(
                        userRepository.findByUsername(usernameOrEmail),
                        userRepository.findByEmail(usernameOrEmail)
                )
                .switchIfEmpty(Mono.error(new UserNotFoundException("Invalid credentials")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        return handleAccountLockout(user.getUsername())
                                .then(Mono.error(new IllegalArgumentException("Invalid password.")));
                    }
                    String accessToken = jwtTokenProvider.generateJwtToken(user.getUsername());
                    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

                    return tokenService.storeToken(accessToken, ACCESS_TOKEN_TTL)
                            .then(tokenService.storeToken(refreshToken, REFRESH_TOKEN_TTL))
                            .then(sendUserAuthenticatedEvent(user, accessToken))
                            .thenReturn(new JwtResponse(
                                    accessToken,
                                    refreshToken,
                                    user.getId(),
                                    user.getUsername(),
                                    user.getEmail(),
                                    user.getFullName(),
                                    user.getRoles()
                            ));
                });
    }

    /**
     * Refresh token:
     * 1) Validate the refresh token.
     * 2) Check if it's active in Redis.
     * 3) Find the user and generate new tokens.
     * 4) Store new tokens and remove the old refresh token.
     * 5) Send Kafka event for token refresh.
     *

    public Mono<JwtResponse> refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return Mono.error(new IllegalArgumentException("Invalid refresh token."));
        }
        return tokenService.isTokenActive(refreshToken)
                .flatMap(isActive -> {
                    if (!isActive) {
                        return Mono.error(new IllegalArgumentException("Refresh token is no longer active."));
                    }
                    String username = jwtTokenProvider.getSubjectFromJwt(refreshToken);
                    return userRepository.findByUsername(username)
                            .switchIfEmpty(Mono.error(new UserNotFoundException("User not found for refresh token.")))
                            .flatMap(user -> {
                                String newAccessToken = jwtTokenProvider.generateJwtToken(username);
                                String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

                                return tokenService.storeToken(newAccessToken, ACCESS_TOKEN_TTL)
                                        .then(tokenService.storeToken(newRefreshToken, REFRESH_TOKEN_TTL))
                                        .then(tokenService.removeToken(refreshToken))
                                        .then(sendTokenRefreshedEvent(user, newAccessToken))
                                        .thenReturn(new JwtResponse(
                                                newAccessToken,
                                                newRefreshToken,
                                                user.getId(),
                                                user.getUsername(),
                                                user.getEmail(),
                                                user.getFullName(),
                                                user.getRoles()
                                        ));
                            });
                });
    }

    /**
     * Logout:
     * 1) Remove tokens from Redis.
     * 2) Send Kafka event for logout.
     *
    public Mono<Void> logout(String accessToken, String refreshToken) {
        String username = jwtTokenProvider.getSubjectFromJwt(accessToken);

        return userRepository.findByUsername(username)
                .flatMap(user -> tokenService.removeToken(accessToken)
                        .then(tokenService.removeToken(refreshToken))
                        .then(sendUserLogoutEvent(user, accessToken)))
                .then();
    }

    /**
     * Invalidate token:
     * 1) Remove token from Redis.
     * 2) Send Kafka event for token invalidation.
     *
    public Mono<Void> invalidateToken(String accessToken) {
        String username = jwtTokenProvider.getSubjectFromJwt(accessToken);

        return userRepository.findByUsername(username)
                .flatMap(user -> tokenService.removeToken(accessToken)
                        .then(sendTokenInvalidatedEvent(user, accessToken)))
                .then();
    }

    /**
     * Fetch user details reactively.
     *
    public Mono<UserDTO> getUserDetails(String usernameOrEmail) {
        return Mono.firstWithSignal(
                        userRepository.findByUsername(usernameOrEmail),
                        userRepository.findByEmail(usernameOrEmail)
                )
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found.")))
                .map(this::mapToUserDTO);
    }

    /**
     * Handle account lockout by sending a Kafka event.
     *
    private Mono<Void> handleAccountLockout(String username) {
        logger.warn("Account locked for username: {}", username);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event", "ACCOUNT_LOCKOUT");
        eventData.put("username", username);
        eventData.put("timestamp", System.currentTimeMillis());

        return kafkaProducerService.sendMessage("auth-account-lockout-topic", eventData);
    }

    /**
     * Send event when user is authenticated successfully.
     *
    private Mono<Void> sendUserAuthenticatedEvent(User user, String accessToken) {
        logger.info("User authenticated: {}", user.getUsername());
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event", "USER_AUTHENTICATED");
        eventData.put("username", user.getUsername());
        eventData.put("userId", user.getId());
        eventData.put("roles", user.getRoles());
        eventData.put("tokenSignature", getTokenSignature(accessToken));
        eventData.put("timestamp", System.currentTimeMillis());

        return kafkaProducerService.sendMessage("auth-user-authenticated-topic", eventData);
    }

    /**
     * Send event when token is refreshed.
     *
    private Mono<Void> sendTokenRefreshedEvent(User user, String accessToken) {
        logger.info("Token refreshed for user: {}", user.getUsername());
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event", "TOKEN_REFRESHED");
        eventData.put("username", user.getUsername());
        eventData.put("userId", user.getId());
        eventData.put("tokenSignature", getTokenSignature(accessToken));
        eventData.put("timestamp", System.currentTimeMillis());

        return kafkaProducerService.sendMessage("auth-token-refreshed-topic", eventData);
    }

    /**
     * Send event when user logs out.
     /
    private Mono<Void> sendUserLogoutEvent(User user, String accessToken) {
        logger.info("User logout: {}", user.getUsername());
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event", "USER_LOGOUT");
        eventData.put("username", user.getUsername());
        eventData.put("userId", user.getId());
        eventData.put("tokenSignature", getTokenSignature(accessToken));
        eventData.put("timestamp", System.currentTimeMillis());

        return kafkaProducerService.sendMessage("auth-user-logout-topic", eventData);
    }

    /**
     * Send event when token is invalidated.
     *
    private Mono<Void> sendTokenInvalidatedEvent(User user, String accessToken) {
        logger.info("Token invalidated for user: {}", user.getUsername());
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event", "TOKEN_INVALIDATED");
        eventData.put("username", user.getUsername());
        eventData.put("userId", user.getId());
        eventData.put("tokenSignature", getTokenSignature(accessToken));
        eventData.put("timestamp", System.currentTimeMillis());

        return kafkaProducerService.sendMessage("auth-token-invalidated-topic", eventData);
    }

    /**
     * Get token signature (last part of JWT) for reference without exposing the full token.
     *
    private String getTokenSignature(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        String[] parts = token.split("\\.");
        return (parts.length == 3) ? parts[2] : "";
    }

    private UserDTO mapToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setFullName(user.getFullName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setActive(user.isActive());
        userDTO.setDateOfBirth(user.getDateOfBirth());
        userDTO.setRoles(user.getRoles());
        return userDTO;
    }
}

 */