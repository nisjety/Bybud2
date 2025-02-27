package com.bybud.userservice.controller;

import com.bybud.entity.dto.CreateUserDTO;
import com.bybud.entity.dto.UpdateUserDTO;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.dto.UserCredentialsDTO;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.response.BaseResponse;
import com.bybud.userservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    // Inject the internal secret from configuration
    @Value("${internal.secret}")
    protected String internalSecret;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Public endpoint to register a new user.
     * The roles must be provided and validated.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<BaseResponse<UserDTO>>> registerUser(@Valid @RequestBody CreateUserDTO createUserDTO) {
        logger.info("Received registration request for user with username: {} and roles: {}",
                createUserDTO.getUsername(), createUserDTO.getRoles());

        // Validate all roles
        if (!areValidRoles(createUserDTO.getRoles())) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(BaseResponse.error("Invalid role(s) provided. Allowed roles are: CUSTOMER, COURIER, ADMIN.")));
        }

        return userService.createUser(createUserDTO)
                .map(userDTO -> ResponseEntity.ok(BaseResponse.success("User registered successfully.", userDTO)))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage())))
                );
    }

    /**
     * Secure endpoint to fetch a user by ID. Requires authentication.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER', 'COURIER')")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<UserDTO>>> getUserById(@PathVariable("id") String id) {
        logger.info("Received request to fetch user by ID: {}", id);
        return userService.getUserById(id)
                .map(userDTO -> ResponseEntity.ok(BaseResponse.success("User fetched successfully.", userDTO)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Secure endpoint to fetch user details by username or email. Requires authentication.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER', 'COURIER')")
    @GetMapping("/details")
    public Mono<ResponseEntity<BaseResponse<UserDTO>>> getUserDetails(@RequestParam("usernameOrEmail") String usernameOrEmail) {
        logger.info("Received request to fetch user details for: {}", usernameOrEmail);
        return userService.getUserDetails(usernameOrEmail)
                .map(userDTO -> ResponseEntity.ok(BaseResponse.success("User details fetched successfully.", userDTO)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Secure endpoint to fetch minimal user credentials (including hashed password).
     * This endpoint is intended for internal use by the auth gateway.
     */
    @GetMapping("/credentials")
    public Mono<ResponseEntity<BaseResponse<UserCredentialsDTO>>> getUserCredentials(
            @RequestParam("usernameOrEmail") String usernameOrEmail,
            @RequestHeader("X-INTERNAL-SECRET") String providedSecret) {

        logger.info("Received request to fetch user credentials for: {}", usernameOrEmail);

        if (!internalSecret.equals(providedSecret)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("Forbidden: Invalid internal secret")));
        }

        return userService.getUserCredentials(usernameOrEmail)
                .map(credentials -> ResponseEntity.ok(BaseResponse.success("User credentials fetched successfully.", credentials)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Admin-only endpoint to fetch all users.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public Mono<ResponseEntity<BaseResponse<List<UserDTO>>>> getAllUsers() {
        logger.info("Admin request to fetch all users.");
        return userService.getAllUsers()
                .map(users -> ResponseEntity.ok(BaseResponse.success("All users fetched successfully.", users)))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage()))));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public Mono<String> healthCheck() {
        return Mono.just("UserService is running");
    }

    /**
     * Endpoint to update user profile.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER', 'COURIER')")
    @PutMapping("/{id}")
    public Mono<ResponseEntity<BaseResponse<UserDTO>>> updateUserProfile(
            @PathVariable("id") String userId,
            @Valid @RequestBody UpdateUserDTO updateUserDTO) {
        logger.info("Received update profile request for user: {}", userId);
        return userService.updateUserProfile(userId, updateUserDTO)
                .map(updatedUser -> ResponseEntity.ok(BaseResponse.success("User updated successfully.", updatedUser)))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(BaseResponse.error(e.getMessage()))));
    }

    /**
     * Utility method to validate a set of roles.
     */
    private boolean areValidRoles(Set<RoleName> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().allMatch(role -> {
            for (RoleName validRole : RoleName.values()) {
                if (validRole == role) {
                    return true;
                }
            }
            return false;
        });
    }
}
