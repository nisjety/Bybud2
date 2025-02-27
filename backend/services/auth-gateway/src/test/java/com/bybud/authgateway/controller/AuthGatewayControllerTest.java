package com.bybud.authgateway.controller;

import com.bybud.authgateway.service.AuthGatewayService;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.request.LoginRequest;
import com.bybud.entity.response.JwtResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthGatewayControllerTest {

    @Mock
    private AuthGatewayService authGatewayService;

    @InjectMocks
    private AuthGatewayController authGatewayController;

    private WebTestClient webTestClient;

    private JwtResponse testJwtResponse;
    private UserDTO testUserDTO;
    private final String testUsername = "testuser";
    private final String testPassword = "password";
    private final String testEmail = "test@example.com";
    private final String testAccessToken = "test-access-token";
    private final String testRefreshToken = "test-refresh-token";

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(authGatewayController).build();

        // Create test JWT response
        Set<RoleName> roles = new HashSet<>();
        roles.add(RoleName.CUSTOMER);

        testJwtResponse = new JwtResponse(
                testAccessToken,
                testRefreshToken,
                "user123",
                testUsername,
                testEmail,
                "Test User",
                roles
        );

        // Create test UserDTO
        testUserDTO = new UserDTO();
        testUserDTO.setId("user123");
        testUserDTO.setUsername(testUsername);
        testUserDTO.setEmail(testEmail);
        testUserDTO.setFullName("Test User");
        testUserDTO.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUserDTO.setPhoneNumber("+1234567890");
        testUserDTO.setRoles(roles);
        testUserDTO.setActive(true);
    }

    @Test
    void loginSuccess() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(testUsername);
        loginRequest.setPassword(testPassword);

        when(authGatewayService.login(eq(testUsername), eq(testPassword), any(ServerWebExchange.class)))
                .thenReturn(Mono.just(testJwtResponse));

        // Act & Assert
        webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("Login successful.")
                .jsonPath("$.data.accessToken").isEqualTo(testAccessToken)
                .jsonPath("$.data.refreshToken").isEqualTo(testRefreshToken)
                .jsonPath("$.data.username").isEqualTo(testUsername);
    }

    @Test
    void loginFailure() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(testUsername);
        loginRequest.setPassword("wrong-password");

        when(authGatewayService.login(eq(testUsername), eq("wrong-password"), any(ServerWebExchange.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid password.")));

        // Act & Assert
        webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Login failed: Invalid password.");
    }

    @Test
    void refreshTokenSuccess() {
        // Arrange
        when(authGatewayService.refreshToken(testRefreshToken))
                .thenReturn(Mono.just(testJwtResponse));

        // Act & Assert
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/refresh")
                        .queryParam("refreshToken", testRefreshToken)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("Token refreshed successfully.")
                .jsonPath("$.data.accessToken").isEqualTo(testAccessToken)
                .jsonPath("$.data.refreshToken").isEqualTo(testRefreshToken);
    }

    @Test
    void refreshTokenFailure() {
        // Arrange
        when(authGatewayService.refreshToken("invalid-token"))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid refresh token.")));

        // Act & Assert
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/refresh")
                        .queryParam("refreshToken", "invalid-token")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Token refresh failed: Invalid refresh token.");
    }

    @Test
    void getUserDetailsSuccess() {
        // Arrange
        when(authGatewayService.getUserDetails(testUsername))
                .thenReturn(Mono.just(testUserDTO));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/user")
                        .queryParam("usernameOrEmail", testUsername)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("User details fetched successfully.")
                .jsonPath("$.data.username").isEqualTo(testUsername)
                .jsonPath("$.data.email").isEqualTo(testEmail);
    }

    @Test
    void getUserDetailsFailure() {
        // Arrange
        when(authGatewayService.getUserDetails("nonexistent"))
                .thenReturn(Mono.error(new RuntimeException("User not found.")));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/user")
                        .queryParam("usernameOrEmail", "nonexistent")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Failed to fetch user details: User not found.");
    }

    @Test
    void logoutSuccess() {
        // Arrange
        when(authGatewayService.logout(anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/logout")
                        .queryParam("refreshToken", testRefreshToken)
                        .build())
                .header("Authorization", "Bearer " + testAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("Logout successful.");
    }

    @Test
    void logoutFailureInvalidHeader() {
        // Act & Assert
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/logout")
                        .queryParam("refreshToken", testRefreshToken)
                        .build())
                .header("Authorization", "InvalidHeader")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Invalid authorization header");
    }

    @Test
    void invalidateTokenSuccess() {
        // Arrange
        when(authGatewayService.invalidateToken(anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.post()
                .uri("/api/auth/invalidate")
                .header("Authorization", "Bearer " + testAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("Token invalidated successfully.");
    }

    @Test
    void invalidateTokenFailureInvalidHeader() {
        // Act & Assert
        webTestClient.post()
                .uri("/api/auth/invalidate")
                .header("Authorization", "InvalidHeader")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Invalid authorization header");
    }

    @Test
    void healthCheck() {
        webTestClient.get()
                .uri("/api/auth/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("auth-gateway")
                .jsonPath("$.status").isEqualTo("UP");
        // You can add more assertions for timestamp if needed
    }

}