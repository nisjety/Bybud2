/*
package com.bybud.authservice.service;

import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import com.bybud.entity.response.JwtResponse;
import com.bybud.kafka.producer.KafkaProducerService;
import com.bybud.security.config.JwtTokenProvider;
import com.bybud.security.service.ReactiveTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    // We'll inject a ReactiveTokenService separately (from the security module)
    private ReactiveTokenService tokenService;

    private User mockUser;
    private UserDTO mockUserDTO;

    private final String usernameOrEmail = "testUser";
    private final String rawPassword = "password123";
    private final String hashedPassword = "hashedPassword123";

    @BeforeEach
    void setUp() {
        // Create a simple ReactiveTokenService mock if needed.
        tokenService = mock(ReactiveTokenService.class);
        // Set the tokenService in authService (using reflection helper)
        setField(authService, "tokenService", tokenService);

        mockUser = new User();
        mockUser.setUsername("testUser");
        mockUser.setEmail("test@example.com");
        mockUser.setPassword(hashedPassword);
        mockUser.setFullName("Test User");
        mockUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        mockUser.setRoles(Set.of(RoleName.CUSTOMER));

        mockUserDTO = new UserDTO();
        mockUserDTO.setUsername("testUser");
        mockUserDTO.setFullName("Test User");
        mockUserDTO.setEmail("test@example.com");
    }

    @Test
    public void testLogin_Success() {
        // Configure repository to return the user
        when(userRepository.findByUsername(anyString())).thenReturn(Mono.just(mockUser));
        // For fallback, you can also stub findByEmail if needed
        // when(userRepository.findByEmail(anyString())).thenReturn(Mono.empty());

        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);
        // Setup JWT provider stubs
        when(jwtTokenProvider.generateJwtToken(anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refreshToken");
        // Stub token storage in Redis (DragonflyDB)
        when(tokenService.storeToken("accessToken", AuthService.ACCESS_TOKEN_TTL))
                .thenReturn(Mono.just(true));
        when(tokenService.storeToken("refreshToken", AuthService.REFRESH_TOKEN_TTL))
                .thenReturn(Mono.just(true));

        Mono<JwtResponse> result = authService.login(usernameOrEmail, rawPassword);
        StepVerifier.create(result)
                .assertNext(jwtResponse -> {
                    assertEquals("accessToken", jwtResponse.getAccessToken());
                    assertEquals("refreshToken", jwtResponse.getRefreshToken());
                    assertEquals("testUser", jwtResponse.getUsername());
                    assertEquals("test@example.com", jwtResponse.getEmail());
                })
                .verifyComplete();

        verify(userRepository, times(1)).findByUsername(usernameOrEmail);
        verify(passwordEncoder, times(1)).matches(rawPassword, hashedPassword);
        verify(tokenService, times(1)).storeToken("accessToken", AuthService.ACCESS_TOKEN_TTL);
        verify(tokenService, times(1)).storeToken("refreshToken", AuthService.REFRESH_TOKEN_TTL);
    }

    @Test
    public void testLogin_Failure_InvalidPassword() {
        when(userRepository.findByUsername(anyString())).thenReturn(Mono.just(mockUser));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(false);
        // tokenService should not be called because the password is invalid.
        Mono<JwtResponse> result = authService.login(usernameOrEmail, rawPassword);
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().equals("Invalid password."))
                .verify();

        verify(kafkaProducerService, times(1)).sendMessage(eq("auth-account-lockout-topic"), anyString());
        verify(tokenService, never()).storeToken(anyString(), any(Duration.class));
    }

    @Test
    public void testRefreshToken_Failure_InvalidRefreshToken() {
        // Stub JWT provider to return false on refresh token validation
        when(jwtTokenProvider.validateRefreshToken("invalidRefreshToken")).thenReturn(false);

        Mono<JwtResponse> result = authService.refreshToken("invalidRefreshToken");
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().equals("Invalid refresh token."))
                .verify();
    }

    @Test
    public void testGetUserDetails_Success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Mono.just(mockUser));
        Mono<UserDTO> result = authService.getUserDetails(usernameOrEmail);
        StepVerifier.create(result)
                .assertNext(userDTO -> {
                    assertEquals("testUser", userDTO.getUsername());
                    assertEquals("Test User", userDTO.getFullName());
                    assertEquals("test@example.com", userDTO.getEmail());
                })
                .verifyComplete();
    }

    // Helper method for setting a private field using reflection
    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

 */
