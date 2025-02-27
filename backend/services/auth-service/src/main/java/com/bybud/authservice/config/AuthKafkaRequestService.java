/*package com.bybud.authservice.config;

import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
public class AuthKafkaRequestService {
    private static final Logger logger = LoggerFactory.getLogger(AuthKafkaRequestService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserRepository userRepository;
    private final CompletableFuture<UserDTO> responseFuture = new CompletableFuture<>();

    public AuthKafkaRequestService(KafkaTemplate<String, String> kafkaTemplate,
                                   UserRepository userRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "user_response", groupId = "auth-service-group")
    public void listenUserResponse(UserDTO user) {
        logger.info("Received user details via Kafka: {}", user);
        responseFuture.complete(user);
    }

    public Mono<UserDTO> fetchUserFromUserService(String username) {
        logger.info("Requesting user details for: {}", username);
        kafkaTemplate.send("user_request", username);
        return Mono.fromFuture(responseFuture);
    }

    @KafkaListener(topics = "user_profile_updated", groupId = "auth-service-group")
    public void listenUserUpdated(UserDTO userEvent) {
        logger.info("Received user update event: {}", userEvent);
        userRepository.findById(userEvent.getId())
                .flatMap(existingUser -> {
                    existingUser.setUsername(userEvent.getUsername());
                    existingUser.setFullName(userEvent.getFullName());
                    existingUser.setEmail(userEvent.getEmail());
                    existingUser.setPhoneNumber(userEvent.getPhoneNumber());
                    existingUser.setRoles(userEvent.getRoles());
                    return userRepository.save(existingUser);
                })
                .subscribe();
    }
}


 */