/*
package com.bybud.authservice.integration;

import com.bybud.authservice.config.TestRedisConfig;
import com.bybud.authservice.service.AuthService;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import com.bybud.entity.response.JwtResponse;
import com.bybud.kafka.producer.KafkaProducerService;
import com.bybud.security.config.JwtTokenProvider;
import com.bybud.security.service.ReactiveTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        // Exclude the reactive auto-configuration to avoid duplicate Redis templates:
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
})
@Import(TestRedisConfig.class)
class AuthServiceIntegrationTest extends AbstractRedisIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ReactiveRedisConnectionFactory connectionFactory;

    @Autowired
    private ReactiveTokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User mockUser;
    private final String username = "testuser";
    private final String password = "password123";
    private final String hashedPassword = "hashedPassword123";

    @BeforeEach
    void setUp() {
        // Clear Redis and verify connection
        StepVerifier.create(connectionFactory.getReactiveConnection()
                        .serverCommands()
                        .flushAll()
                        .then(connectionFactory.getReactiveConnection().ping()))
                .expectNext("PONG")
                .verifyComplete();

        // Create mock user
        mockUser = new User();
        mockUser.setId("user123");
        mockUser.setUsername(username);
        mockUser.setEmail("test@example.com");
        mockUser.setPassword(hashedPassword);
        mockUser.setFullName("Test User");
        mockUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        mockUser.setRoles(Set.of(RoleName.CUSTOMER));

        // Set up mock responses
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(mockUser));
        when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);
        when(jwtTokenProvider.generateJwtToken(anyString())).thenReturn("test-access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("test-refresh-token");
        when(jwtTokenProvider.validateRefreshToken("test-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getSubjectFromJwt("test-refresh-token")).thenReturn(username);
    }

    @Test
    void loginAndRefreshToken_Success() {
        // Test login
        StepVerifier.create(authService.login(username, password))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("test-access-token", response.getAccessToken());
                    assertEquals("test-refresh-token", response.getRefreshToken());
                    assertEquals(username, response.getUsername());
                    assertEquals("test@example.com", response.getEmail());
                    assertTrue(response.getRoles().contains(RoleName.CUSTOMER));
                })
                .verifyComplete();

        // Verify access token is stored
        StepVerifier.create(tokenService.isTokenActive("test-access-token"))
                .expectNext(true)
                .verifyComplete();

        // Test refresh token
        StepVerifier.create(authService.refreshToken("test-refresh-token"))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("test-access-token", response.getAccessToken());
                    assertEquals("test-refresh-token", response.getRefreshToken());
                    assertEquals(username, response.getUsername());
                })
                .verifyComplete();
    }

    @Test
    void logout_Success() {
        // First login, then logout
        StepVerifier.create(authService.login(username, password)
                        .flatMap(response -> authService.logout(response.getAccessToken(), response.getRefreshToken())))
                .verifyComplete();

        // Verify tokens are removed
        StepVerifier.create(tokenService.isTokenActive("test-access-token"))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(tokenService.isTokenActive("test-refresh-token"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void login_InvalidCredentials_Failure() {
        when(passwordEncoder.matches("wrongpassword", hashedPassword)).thenReturn(false);

        StepVerifier.create(authService.login(username, "wrongpassword"))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Invalid password."))
                .verify();

        verify(kafkaProducerService, times(1))
                .sendMessage(eq("auth-account-lockout-topic"), contains("Account locked: " + username));
    }

    @Test
    void refreshToken_InvalidToken_Failure() {
        when(jwtTokenProvider.validateRefreshToken("invalid-token")).thenReturn(false);

        StepVerifier.create(authService.refreshToken("invalid-token"))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Invalid refresh token."))
                .verify();
    }
}

 */
