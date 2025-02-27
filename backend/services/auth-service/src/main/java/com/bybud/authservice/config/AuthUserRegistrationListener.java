/*
package com.bybud.authservice.config;


import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthUserRegistrationListener {

    private static final Logger logger = LoggerFactory.getLogger(AuthUserRegistrationListener.class);
    private final UserRepository userRepository;

    public AuthUserRegistrationListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "${bybud.kafka.topics.user-registered}", groupId = "auth-service-group")
    public void handleUserRegistered(UserDTO userEvent) {
        logger.info("Received user registration event: {}", userEvent);
        // Check if the user already exists in auth-service's MongoDB
        userRepository.findById(userEvent.getId())
                .switchIfEmpty(Mono.defer(() -> {
                    User user = convertUserDTOToUser(userEvent);
                    return userRepository.save(user);
                }))
                .subscribe(saved -> logger.info("Auth-service stored user with id: {}", saved.getId()));
    }

    private User convertUserDTOToUser(UserDTO dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setActive(dto.isActive());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setRoles(dto.getRoles());
        return user;
    }
}

 */
