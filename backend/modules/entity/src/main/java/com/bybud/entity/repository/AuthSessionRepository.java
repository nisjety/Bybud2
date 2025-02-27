package com.bybud.entity.repository;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AuthSessionRepository {
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public AuthSessionRepository(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> saveToken(String userId, String token) {
        return redisTemplate.opsForValue().set("auth:" + userId, token);
    }

    public Mono<String> getToken(String userId) {
        return redisTemplate.opsForValue().get("auth:" + userId);
    }

    public Mono<Boolean> deleteToken(String userId) {
        return redisTemplate.delete("auth:" + userId)
                .map(deleted -> deleted > 0);
    }
}

