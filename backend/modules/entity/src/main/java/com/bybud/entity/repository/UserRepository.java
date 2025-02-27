package com.bybud.entity.repository;

import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {

    // Find user by username
    Mono<User> findByUsername(String username);

    // Find user by email
    Mono<User> findByEmail(String email);

    // Check if a username already exists
    Mono<Boolean> existsByUsername(String username);

    // Find user by phone number
    Mono<User> findByPhoneNumber(String phoneNumber);

    // Check if an email already exists
    Mono<Boolean> existsByEmail(String email);

    // Find all active or inactive users with sorting
    Flux<User> findAllByActive(boolean active, Sort sort);

    // Find all users by a specific role
    Flux<User> findAllByRoles(RoleName role);
}
