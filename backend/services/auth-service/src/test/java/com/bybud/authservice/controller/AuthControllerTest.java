/*package com.bybud.authservice.controller;

import com.bybud.authservice.service.AuthService;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.request.LoginRequest;
import java.util.Set;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.response.JwtResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
        webTestClient = WebTestClient.bindToController(authController)
                .configureClient()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @Test
    void login_Success() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        JwtResponse jwtResponse = new JwtResponse(
                "access-token",
                "refresh-token",
                "user123",
                "testuser",
                "test@example.com",
                "Test User",
                Set.of(RoleName.CUSTOMER));

        when(authService.login(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()))
                .thenReturn(Mono.just(jwtResponse));

        // Act & Assert
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    System.out.println("Response body: " + new String(response.getResponseBody()));
                })
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("Login successful.")
                .jsonPath("$.data.accessToken").isEqualTo("access-token")
                .jsonPath("$.data.refreshToken").isEqualTo("refresh-token")
                .jsonPath("$.data.userId").isEqualTo("user123")
                .jsonPath("$.data.username").isEqualTo("testuser")
                .jsonPath("$.data.email").isEqualTo("test@example.com")
                .jsonPath("$.data.fullName").isEqualTo("Test User")
                .jsonPath("$.data.roles[0]").isEqualTo("CUSTOMER");
    }

    @Test
    void login_Failure() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        when(authService.login(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()))
                .thenReturn(Mono.error(new RuntimeException("Invalid credentials")));

        // Act & Assert
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Login failed: Invalid credentials");
    }

    @Test
    void refresh_Success() {
        // Arrange
        String refreshToken = "valid-refresh-token";
        JwtResponse jwtResponse = new JwtResponse(
                "new-access-token",
                "new-refresh-token",
                "user123",
                "testuser",
                "test@example.com",
                "Test User",
                Set.of(RoleName.COURIER));

        when(authService.refreshToken(refreshToken))
                .thenReturn(Mono.just(jwtResponse));

        // Act & Assert
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/refresh")
                        .queryParam("refreshToken", refreshToken)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    System.out.println("Refresh response body: " + new String(response.getResponseBody()));
                })
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("Token refreshed successfully.")
                .jsonPath("$.data.accessToken").isEqualTo("new-access-token")
                .jsonPath("$.data.refreshToken").isEqualTo("new-refresh-token")
                .jsonPath("$.data.userId").isEqualTo("user123")
                .jsonPath("$.data.username").isEqualTo("testuser")
                .jsonPath("$.data.email").isEqualTo("test@example.com")
                .jsonPath("$.data.fullName").isEqualTo("Test User")
                .jsonPath("$.data.roles[0]").isEqualTo("COURIER");
    }

    @Test
    void refresh_Failure() {
        // Arrange
        String refreshToken = "invalid-refresh-token";

        when(authService.refreshToken(refreshToken))
                .thenReturn(Mono.error(new RuntimeException("Invalid refresh token")));

        // Act & Assert
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/refresh")
                        .queryParam("refreshToken", refreshToken)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Token refresh failed: Invalid refresh token");
    }

    @Test
    void getUserDetails_Success() {
        // Arrange
        String usernameOrEmail = "testuser";
        UserDTO userDTO = new UserDTO(); // Fill with test data as needed

        when(authService.getUserDetails(usernameOrEmail))
                .thenReturn(Mono.just(userDTO));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/user")
                        .queryParam("usernameOrEmail", usernameOrEmail)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("User details fetched successfully.");
    }

    @Test
    void getUserDetails_Failure() {
        // Arrange
        String usernameOrEmail = "nonexistent";

        when(authService.getUserDetails(usernameOrEmail))
                .thenReturn(Mono.error(new RuntimeException("User not found")));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/user")
                        .queryParam("usernameOrEmail", usernameOrEmail)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Failed to fetch user details: User not found");
    }
}

 */