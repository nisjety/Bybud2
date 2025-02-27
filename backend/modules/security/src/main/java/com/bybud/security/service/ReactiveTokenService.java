package com.bybud.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service to handle token storage, validation, and blacklisting in Redis.
 * Tokens are stored with a TTL and can be blacklisted for revocation.
 */
@Service
public class ReactiveTokenService {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveTokenService.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_PREFIX = "token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public ReactiveTokenService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        logger.info("ReactiveTokenService initialized");
    }

    /**
     * Store a token in Redis with a specified TTL.
     * @param token the JWT token string.
     * @param ttl the duration to live.
     * @return Mono<Boolean> indicating success.
     */
    public Mono<Boolean> storeToken(String token, Duration ttl) {
        String signature = extractTokenSignature(token);
        String key = TOKEN_PREFIX + signature;

        logger.debug("STORAGE: About to store token with key: {}, value: 'ACTIVE', TTL: {}", key, ttl);

        return redisTemplate.hasKey(key)
                .flatMap(exists -> {
                    if (exists) {
                        logger.debug("STORAGE: Token key already exists: {}", key);
                        // If key exists, extend TTL
                        return redisTemplate.expire(key, ttl);
                    }

                    return redisTemplate.opsForValue().set(key, "ACTIVE", ttl)
                            .doOnSuccess(success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    logger.debug("STORAGE: Token stored successfully, key: {}", key);
                                } else {
                                    logger.warn("STORAGE: Failed to store token, key: {}", key);
                                }
                            })
                            .doOnError(e -> logger.error("STORAGE: Error storing token: {}", e.getMessage(), e));
                });
    }

    /**
     * Check if a token is still active.
     * @param token the JWT token string.
     * @return Mono<Boolean> true if active.
     */
    public Mono<Boolean> isTokenActive(String token) {
        String signature = extractTokenSignature(token);
        String key = TOKEN_PREFIX + signature;

        logger.debug("RETRIEVAL: Checking if token is active with key: {}", key);

        return redisTemplate.hasKey(key)
                .flatMap(exists -> {
                    if (!exists) {
                        logger.debug("RETRIEVAL: Token key does not exist: {}", key);
                        return Mono.just(false);
                    }

                    logger.debug("RETRIEVAL: Token key exists: {}", key);
                    return redisTemplate.opsForValue().get(key)
                            .doOnNext(value -> logger.debug("RETRIEVAL: Redis value for key {}: '{}'", key, value))
                            .map("ACTIVE"::equals)
                            .defaultIfEmpty(false);
                })
                .doOnNext(active -> logger.debug("RETRIEVAL: Token active status: {}", active));
    }

    /**
     * Remove a token from Redis.
     * @param token the JWT token string.
     * @return Mono<Boolean> indicating if the token was removed.
     */
    public Mono<Boolean> removeToken(String token) {
        String signature = extractTokenSignature(token);
        String key = TOKEN_PREFIX + signature;

        logger.debug("Removing token with key: {}", key);

        return redisTemplate.hasKey(key)
                .flatMap(exists -> {
                    if (!exists) {
                        logger.debug("Token key does not exist during removal: {}", key);
                        return Mono.just(false);
                    }

                    return redisTemplate.delete(key)
                            .map(deleted -> deleted > 0)
                            .doOnNext(success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    logger.debug("Token removed successfully: {}", key);
                                } else {
                                    logger.warn("Failed to remove token: {}", key);
                                }
                            });
                });
    }

    /**
     * Blacklist a token (e.g., for security reasons).
     * The blacklist entry will remain for the max lifetime of a token.
     * @param token the JWT token string or its signature.
     * @param expiry how long to keep the token in the blacklist.
     * @return Mono<Boolean> indicating success.
     */
    public Mono<Boolean> blacklistToken(String token, Duration expiry) {
        String signature = extractTokenSignature(token);
        String key = BLACKLIST_PREFIX + signature;

        logger.debug("Blacklisting token with key: {}", key);

        return redisTemplate.opsForValue().set(key, "REVOKED", expiry)
                .doOnSuccess(success -> logger.debug("Token blacklisted: {}", success));
    }

    /**
     * Check if a token is blacklisted.
     * @param token the JWT token string or its signature.
     * @return Mono<Boolean> true if blacklisted.
     */
    public Mono<Boolean> isTokenBlacklisted(String token) {
        String signature = extractTokenSignature(token);
        String key = BLACKLIST_PREFIX + signature;

        logger.debug("Checking if token is blacklisted with key: {}", key);

        return redisTemplate.hasKey(key)
                .flatMap(exists -> {
                    if (!exists) {
                        logger.debug("Token not in blacklist: {}", key);
                        return Mono.just(false);
                    }

                    return redisTemplate.opsForValue().get(key)
                            .map("REVOKED"::equals)
                            .defaultIfEmpty(false);
                })
                .doOnNext(blacklisted -> logger.debug("Token blacklist status: {}", blacklisted));
    }

    /**
     * Extract the signature part from a JWT token for use as a Redis key.
     * @param token the JWT token
     * @return the signature part or the original token if it's not a valid JWT
     */
    private String extractTokenSignature(String token) {
        try {
            logger.debug("Extracting signature from token: {}", token);

            // Check if the token has the JWT format (contains dots)
            if (token != null && token.contains(".")) {
                String[] parts = token.split("\\.");

                // A JWT has 3 parts: header.payload.signature
                if (parts.length == 3) {
                    String signature = parts[2];
                    logger.debug("Extracted signature: {}", signature);
                    return signature;
                }
            }

            // If not a valid JWT, use the full token as key
            logger.debug("Using full token as key (not a valid JWT format)");
            return token;
        } catch (Exception e) {
            logger.warn("Error extracting token signature: {}", e.getMessage(), e);
            // Fall back to using the full token in case of errors
            return token;
        }
    }
}
