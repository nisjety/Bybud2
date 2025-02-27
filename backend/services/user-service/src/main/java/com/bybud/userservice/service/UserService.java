package com.bybud.userservice.service;

import com.bybud.common.exception.UserNotFoundException;
import com.bybud.entity.dto.CreateUserDTO;
import com.bybud.entity.dto.UpdateUserDTO;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.dto.UserCredentialsDTO;
import com.bybud.entity.mapper.UserMapper;
import com.bybud.entity.repository.UserRepository;
import com.bybud.kafka.handler.UserEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserEventHandler eventHandler;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       @Lazy UserEventHandler eventHandler,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventHandler = eventHandler;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        logger.info("UserService initialized");
    }

    /**
     * Creates a new user reactively.
     */
    public Mono<UserDTO> createUser(CreateUserDTO createUserDTO) {
        logger.info("Attempting to create user with username: {}", createUserDTO.getUsername());

        // Encode the raw password before mapping.
        createUserDTO.setPassword(passwordEncoder.encode(createUserDTO.getPassword()));

        return validateUserData(createUserDTO)
                .then(Mono.fromCallable(() -> userMapper.toUser(createUserDTO)))
                .flatMap(userRepository::save)
                .doOnNext(user -> {
                    logger.info("User created successfully with ID: {}", user.getId());
                    // Publish user created event asynchronously
                    publishUserCreatedEvent(user.getId(), user.getUsername());
                })
                .map(userMapper::toUserDTO)
                .doOnError(error -> logger.error("Failed to create user: {}", error.getMessage()));
    }

    /**
     * Updates a user profile reactively.
     */
    public Mono<UserDTO> updateUserProfile(String id, UpdateUserDTO updateUserDTO) {
        logger.info("Updating user profile for ID: {}", id);

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with ID: " + id)))
                .flatMap(user -> {
                    if (updateUserDTO.getPassword() != null) {
                        updateUserDTO.setPassword(passwordEncoder.encode(updateUserDTO.getPassword()));
                    }
                    return Mono.just(userMapper.updateUser(user, updateUserDTO));
                })
                .flatMap(userRepository::save)
                .doOnNext(user -> {
                    logger.debug("User {} updated successfully", user.getId());
                    // Publish user updated event asynchronously
                    publishUserUpdatedEvent(user.getId(), user.getUsername());
                })
                .map(userMapper::toUserDTO)
                .doOnError(error -> logger.error("Failed to update user {}: {}", id, error.getMessage()));
    }

    /**
     * Retrieves all users as a reactive list.
     */
    public Mono<List<UserDTO>> getAllUsers() {
        logger.info("Fetching all users");

        return userRepository.findAll()
                .map(userMapper::toUserDTO)
                .collectList()
                .doOnSuccess(users -> logger.debug("Retrieved {} users", users.size()))
                .doOnError(error -> logger.error("Failed to fetch users: {}", error.getMessage()));
    }

    /**
     * Retrieves a user by ID reactively.
     */
    public Mono<UserDTO> getUserById(String id) {
        logger.info("Fetching user with ID: {}", id);

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with ID: " + id)))
                .map(userMapper::toUserDTO)
                .doOnSuccess(user -> logger.debug("Retrieved user: {}", user.getUsername()))
                .doOnError(error -> logger.error("Failed to fetch user {}: {}", id, error.getMessage()));
    }

    /**
     * Retrieves user details by username or email reactively.
     */
    public Mono<UserDTO> getUserDetails(String usernameOrEmail) {
        logger.info("Fetching user details for: {}", usernameOrEmail);

        return userRepository.findByUsername(usernameOrEmail)
                .switchIfEmpty(userRepository.findByEmail(usernameOrEmail))
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with username or email: " + usernameOrEmail)))
                .map(userMapper::toUserDTO)
                .doOnSuccess(user -> logger.debug("Retrieved user details for: {}", user.getUsername()))
                .doOnError(error -> logger.error("Failed to fetch user details for {}: {}", usernameOrEmail, error.getMessage()));
    }

    /**
     * Retrieves minimal user credentials (including hashed password) for authentication.
     * This method is intended for internal use (e.g., by the auth gateway).
     */
    public Mono<UserCredentialsDTO> getUserCredentials(String usernameOrEmail) {
        logger.info("Fetching user credentials for: {}", usernameOrEmail);

        return userRepository.findByUsername(usernameOrEmail)
                .switchIfEmpty(userRepository.findByEmail(usernameOrEmail))
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with username or email: " + usernameOrEmail)))
                .map(user -> {
                    UserCredentialsDTO dto = new UserCredentialsDTO();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setHashedPassword(user.getPassword());
                    dto.setRoles(user.getRoles());
                    return dto;
                })
                .doOnSuccess(credentials -> logger.debug("Retrieved credentials for user: {}", credentials.getUsername()))
                .doOnError(error -> logger.error("Failed to fetch user credentials for {}: {}", usernameOrEmail, error.getMessage()));
    }

    /**
     * Validates that the username and email are not already in use.
     */
    private Mono<Void> validateUserData(CreateUserDTO dto) {
        return userRepository.existsByUsername(dto.getUsername())
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        logger.warn("Attempted to create user with existing username: {}", dto.getUsername());
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already in use"));
                    }
                    return userRepository.existsByEmail(dto.getEmail());
                })
                .flatMap(emailExists -> {
                    if (emailExists) {
                        logger.warn("Attempted to create user with existing email: {}", dto.getEmail());
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use"));
                    }
                    return Mono.empty();
                });
    }

    // Event publishing methods

    private void publishUserCreatedEvent(String userId, String username) {
        Mono.fromRunnable(() ->
                        eventHandler.publishUserCreated(
                                new UserEventHandler.UserCreatedEvent(userId, username)
                        )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> logger.error("Failed to publish user created event: {}", error.getMessage()))
                .onErrorComplete() // Don't let event publishing failures affect the main flow
                .subscribe();
    }

    private void publishUserUpdatedEvent(String userId, String username) {
        Mono.fromRunnable(() ->
                        eventHandler.publishUserUpdated(
                                new UserEventHandler.UserUpdatedEvent(userId, username)
                        )
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> logger.error("Failed to publish user updated event: {}", error.getMessage()))
                .onErrorComplete() // Don't let event publishing failures affect the main flow
                .subscribe();
    }
}
