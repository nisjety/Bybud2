package com.bybud.userservice.controller;

import com.bybud.entity.dto.CreateUserDTO;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.dto.UserCredentialsDTO; // new DTO for credentials
import com.bybud.entity.model.RoleName;
import com.bybud.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private CreateUserDTO validUserDto;
    private UserDTO mockUser;
    private UserCredentialsDTO mockCredentials;

    // Set the expected internal secret value.
    private final String internalSecret = "yourSharedInternalSecret";

    @BeforeEach
    void setUp() {
        validUserDto = new CreateUserDTO();
        validUserDto.setUsername("testuser");
        validUserDto.setEmail("test@example.com");
        validUserDto.setPassword("securePass123");
        validUserDto.setRoles(Set.of(RoleName.CUSTOMER));

        mockUser = new UserDTO();
        mockUser.setId("123");
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setRoles(Set.of(RoleName.CUSTOMER));

        mockCredentials = new UserCredentialsDTO();
        mockCredentials.setId("123");
        mockCredentials.setUsername("testuser");
        mockCredentials.setHashedPassword("encodedSecurePass123");
        mockCredentials.setRoles(Set.of(RoleName.CUSTOMER));

        // Manually set the internal secret on the controller
        userController.internalSecret = internalSecret;
    }

    @Test
    void registerUser_Success() {
        when(userService.createUser(any(CreateUserDTO.class))).thenReturn(Mono.just(mockUser));

        StepVerifier.create(userController.registerUser(validUserDto))
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).isEqualTo("User registered successfully.");
                    assertThat(response.getBody().getData().getUsername()).isEqualTo(mockUser.getUsername());
                })
                .verifyComplete();
    }

    @Test
    void registerUser_InvalidRole() {
        CreateUserDTO invalidUserDto = new CreateUserDTO();
        invalidUserDto.setUsername("invalidUser");
        invalidUserDto.setEmail("invalid@example.com");
        invalidUserDto.setPassword("password123");
        invalidUserDto.setRoles(Set.of());

        StepVerifier.create(userController.registerUser(invalidUserDto))
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).contains("Invalid role(s) provided");
                })
                .verifyComplete();
    }

    @Test
    void getUserById_Success() {
        when(userService.getUserById("123")).thenReturn(Mono.just(mockUser));

        StepVerifier.create(userController.getUserById("123"))
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).isEqualTo("User fetched successfully.");
                    assertThat(response.getBody().getData().getId()).isEqualTo(mockUser.getId());
                })
                .verifyComplete();
    }

    @Test
    void getUserById_NotFound() {
        when(userService.getUserById("999")).thenReturn(Mono.empty());

        StepVerifier.create(userController.getUserById("999"))
                .consumeNextWith(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void getUserDetails_Success() {
        when(userService.getUserDetails("testuser")).thenReturn(Mono.just(mockUser));

        StepVerifier.create(userController.getUserDetails("testuser"))
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).isEqualTo("User details fetched successfully.");
                })
                .verifyComplete();
    }

    @Test
    void getUserDetails_NotFound() {
        when(userService.getUserDetails("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(userController.getUserDetails("unknown"))
                .consumeNextWith(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void getAllUsers_Success() {
        List<UserDTO> users = List.of(mockUser);
        when(userService.getAllUsers()).thenReturn(Mono.just(users));

        StepVerifier.create(userController.getAllUsers())
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).isEqualTo("All users fetched successfully.");
                    assertThat(response.getBody().getData()).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    void getAllUsers_Error() {
        when(userService.getAllUsers()).thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(userController.getAllUsers())
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).isEqualTo("Database error");
                })
                .verifyComplete();
    }

    // New tests for credentials endpoint

    @Test
    void getUserCredentials_Success() {
        when(userService.getUserCredentials("testuser")).thenReturn(Mono.just(mockCredentials));

        StepVerifier.create(userController.getUserCredentials("testuser", internalSecret))
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).isEqualTo("User credentials fetched successfully.");
                    assertThat(response.getBody().getData().getUsername()).isEqualTo(mockCredentials.getUsername());
                    assertEquals(mockCredentials.getHashedPassword(), response.getBody().getData().getHashedPassword());
                })
                .verifyComplete();
    }

    @Test
    void getUserCredentials_NotFound() {
        when(userService.getUserCredentials("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(userController.getUserCredentials("unknown", internalSecret))
                .consumeNextWith(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void getUserCredentials_InvalidSecret() {
        // When an invalid secret is provided, the controller should return 403 Forbidden.
        StepVerifier.create(userController.getUserCredentials("testuser", "invalidSecret"))
                .consumeNextWith(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getMessage()).contains("Forbidden");
                })
                .verifyComplete();
    }
}
