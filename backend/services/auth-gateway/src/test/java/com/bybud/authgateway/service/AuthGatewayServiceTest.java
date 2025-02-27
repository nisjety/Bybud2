package com.bybud.authgateway.service;

import com.bybud.entity.dto.UserCredentialsDTO;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import com.bybud.entity.response.BaseResponse;
import com.bybud.entity.response.JwtResponse;
import com.bybud.kafka.handler.AuthenticationEventHandler;
import com.bybud.security.config.JwtTokenProvider;
import com.bybud.security.service.ReactiveTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

public class AuthGatewayServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReactiveTokenService tokenService;

    @Mock
    private AuthenticationEventHandler eventHandler;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private AuthGatewayService authGatewayService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup WebClient builder
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // Setup WebClient GET request chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);

        // Mock the URI builder function - match exactly what's in the service
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);

        // CRITICAL FIX: Match the exact header method signature being used
        // This is likely the key issue - header with string key and varargs values
        when(requestHeadersSpec.header(anyString(), any(String[].class))).thenReturn(requestHeadersSpec);

        // Continue the chain
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        // Initialize the service with the mocked dependencies
        authGatewayService = new AuthGatewayService(
                webClientBuilder,
                userRepository,
                tokenService,
                eventHandler,
                jwtTokenProvider,
                passwordEncoder
        );
    }

    // Test for a successful login using the secure REST call.
    @Test
    public void testLogin_Success() {
        String usernameOrEmail = "testuser";
        String rawPassword = "password123";
        String hashedPassword = "hashedPassword";

        // Create UserCredentialsDTO with the response structure
        UserCredentialsDTO credentialsDTO = new UserCredentialsDTO();
        credentialsDTO.setId("user1");
        credentialsDTO.setUsername("testuser");
        credentialsDTO.setHashedPassword(hashedPassword);
        Set<RoleName> roles = new HashSet<>();
        roles.add(RoleName.CUSTOMER);
        credentialsDTO.setRoles(roles);

        // Create a dummy BaseResponse wrapping the UserCredentialsDTO
        BaseResponse<UserCredentialsDTO> dummyBaseResponse = BaseResponse.success("User credentials fetched successfully.", credentialsDTO);

        // Stub the WebClient chain to return the dummyBaseResponse
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(dummyBaseResponse));

        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

        // Mock the new reactive token generation
        String accessToken = "access.token.signature";
        String refreshToken = "refresh.token.signature";

        // Mock the roles list that will be passed to generateJwtTokenReactive
        List<String> rolesList = roles.stream()
                .map(RoleName::name)
                .collect(Collectors.toList());

        when(jwtTokenProvider.generateJwtTokenReactive(eq(credentialsDTO.getUsername()), any()))
                .thenReturn(Mono.just(accessToken));
        when(jwtTokenProvider.generateRefreshToken(credentialsDTO.getUsername())).thenReturn(refreshToken);

        when(tokenService.storeToken(accessToken, AuthGatewayService.ACCESS_TOKEN_TTL)).thenReturn(Mono.just(true));
        when(tokenService.storeToken(refreshToken, AuthGatewayService.REFRESH_TOKEN_TTL)).thenReturn(Mono.just(true));

        // Create a dummy ServerWebExchange
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        when(request.getRemoteAddress()).thenReturn(address);
        when(exchange.getRequest()).thenReturn(request);

        Mono<JwtResponse> resultMono = authGatewayService.login(usernameOrEmail, rawPassword, exchange);

        StepVerifier.create(resultMono)
                .assertNext(jwtResponse -> {
                    assertEquals(accessToken, jwtResponse.getAccessToken());
                    assertEquals(refreshToken, jwtResponse.getRefreshToken());
                    assertEquals(credentialsDTO.getId(), jwtResponse.getUserId());
                    assertEquals(credentialsDTO.getUsername(), jwtResponse.getUsername());
                    // Email and fullName will be null in the test because they're not part of the UserCredentialsDTO
                    assertEquals(credentialsDTO.getRoles(), jwtResponse.getRoles());
                })
                .verifyComplete();

        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventHandler, times(1)).publishUserAuthenticated(any()));
    }

    // Test login when the password is invalid.
    @Test
    public void testLogin_InvalidPassword() {
        String usernameOrEmail = "testuser";
        String rawPassword = "wrongPassword";
        String hashedPassword = "hashedPassword";

        // Create UserCredentialsDTO with the response structure
        UserCredentialsDTO credentialsDTO = new UserCredentialsDTO();
        credentialsDTO.setId("user1");
        credentialsDTO.setUsername("testuser");
        credentialsDTO.setHashedPassword(hashedPassword);
        Set<RoleName> roles = new HashSet<>();
        roles.add(RoleName.CUSTOMER);
        credentialsDTO.setRoles(roles);

        BaseResponse<UserCredentialsDTO> dummyBaseResponse = BaseResponse.success("User credentials fetched successfully.", credentialsDTO);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(dummyBaseResponse));

        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(false);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
        when(request.getRemoteAddress()).thenReturn(address);
        when(exchange.getRequest()).thenReturn(request);

        Mono<JwtResponse> resultMono = authGatewayService.login(usernameOrEmail, rawPassword, exchange);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                                "Invalid password.".equals(((ResponseStatusException) throwable).getReason())
                )
                .verify();

        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventHandler, times(1)).publishAccountLockout(any()));
    }

    // Test login when user is not found.
    @Test
    public void testLogin_UserNotFound() {
        String usernameOrEmail = "nonexistent";
        String rawPassword = "password123";

        // Stub the WebClient to return empty BaseResponse.
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.empty());

        ServerWebExchange exchange = mock(ServerWebExchange.class);

        Mono<JwtResponse> resultMono = authGatewayService.login(usernameOrEmail, rawPassword, exchange);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                                "Invalid credentials".equals(((ResponseStatusException) throwable).getReason())
                )
                .verify();
    }

    // Test for a successful refreshToken operation with reactive JWT generation
    @Test
    public void testRefreshToken_Success() {
        String oldRefreshToken = "old.refresh.token.signature";
        String username = "testuser";
        User dummyUser = new User();
        dummyUser.setId("user1");
        dummyUser.setUsername(username);
        dummyUser.setEmail("test@example.com");
        dummyUser.setFullName("Test User");
        dummyUser.setRoles(Collections.singleton(RoleName.CUSTOMER));

        when(jwtTokenProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(tokenService.isTokenActive(oldRefreshToken)).thenReturn(Mono.just(true));
        when(jwtTokenProvider.getSubjectFromJwt(oldRefreshToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(dummyUser));

        String newAccessToken = "new.access.token.signature";
        String newRefreshToken = "new.refresh.token.signature";

        // Mock the reactive token generation
        when(jwtTokenProvider.generateJwtTokenReactive(eq(username), any())).thenReturn(Mono.just(newAccessToken));
        when(jwtTokenProvider.generateRefreshToken(username)).thenReturn(newRefreshToken);

        when(tokenService.storeToken(newAccessToken, AuthGatewayService.ACCESS_TOKEN_TTL)).thenReturn(Mono.just(true));
        when(tokenService.storeToken(newRefreshToken, AuthGatewayService.REFRESH_TOKEN_TTL)).thenReturn(Mono.just(true));
        when(tokenService.removeToken(oldRefreshToken)).thenReturn(Mono.just(true));

        Mono<JwtResponse> resultMono = authGatewayService.refreshToken(oldRefreshToken);

        StepVerifier.create(resultMono)
                .assertNext(jwtResponse -> {
                    assertEquals(newAccessToken, jwtResponse.getAccessToken());
                    assertEquals(newRefreshToken, jwtResponse.getRefreshToken());
                    assertEquals(dummyUser.getId(), jwtResponse.getUserId());
                    assertEquals(dummyUser.getUsername(), jwtResponse.getUsername());
                    assertEquals(dummyUser.getEmail(), jwtResponse.getEmail());
                    assertEquals(dummyUser.getFullName(), jwtResponse.getFullName());
                    assertEquals(dummyUser.getRoles(), jwtResponse.getRoles());
                })
                .verifyComplete();

        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(tokenService).removeToken(oldRefreshToken);
                    verify(eventHandler, times(1)).publishTokenRefreshed(any());
                });
    }

    // Test refreshToken when the provided token is invalid.
    @Test
    public void testRefreshToken_InvalidToken() {
        String invalidRefreshToken = "invalid.token";
        when(jwtTokenProvider.validateRefreshToken(invalidRefreshToken)).thenReturn(false);

        Mono<JwtResponse> resultMono = authGatewayService.refreshToken(invalidRefreshToken);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                                "Invalid refresh token.".equals(((ResponseStatusException) throwable).getReason())
                )
                .verify();
    }

    // Test refreshToken when the token is no longer active.
    @Test
    public void testRefreshToken_InactiveToken() {
        String refreshToken = "refresh.token.signature";
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(tokenService.isTokenActive(refreshToken)).thenReturn(Mono.just(false));

        Mono<JwtResponse> resultMono = authGatewayService.refreshToken(refreshToken);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.UNAUTHORIZED &&
                                "Refresh token is no longer active.".equals(((ResponseStatusException) throwable).getReason())
                )
                .verify();
    }

    // Test for a successful logout.
    @Test
    public void testLogout_Success() {
        String accessToken = "access.token.signature";
        String refreshToken = "refresh.token.signature";
        String username = "testuser";

        when(jwtTokenProvider.getSubjectFromJwt(accessToken)).thenReturn(username);
        when(tokenService.removeToken(accessToken)).thenReturn(Mono.just(true));
        when(tokenService.removeToken(refreshToken)).thenReturn(Mono.just(true));

        Mono<Void> resultMono = authGatewayService.logout(accessToken, refreshToken);

        StepVerifier.create(resultMono)
                .verifyComplete();

        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(tokenService, times(1)).removeToken(accessToken);
                    verify(tokenService, times(1)).removeToken(refreshToken);
                    verify(eventHandler, times(1)).publishUserLogout(any());
                });
    }

    // Test for a successful token invalidation.
    @Test
    public void testInvalidateToken_Success() {
        String accessToken = "access.token.signature";
        String reason = "Test reason";
        String username = "testuser";

        when(jwtTokenProvider.getSubjectFromJwt(accessToken)).thenReturn(username);
        when(tokenService.removeToken(accessToken)).thenReturn(Mono.just(true));

        Mono<Void> resultMono = authGatewayService.invalidateToken(accessToken, reason);

        StepVerifier.create(resultMono)
                .verifyComplete();

        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(tokenService, times(1)).removeToken(accessToken);
                    verify(eventHandler, times(1)).publishTokenInvalidated(any());
                });
    }

    // Test for retrieving user details successfully.
    @Test
    public void testGetUserDetails_Success() {
        String usernameOrEmail = "testuser";
        User dummyUser = new User();
        dummyUser.setId("user1");
        dummyUser.setUsername("testuser");
        dummyUser.setEmail("test@example.com");
        dummyUser.setFullName("Test User");
        dummyUser.setRoles(Collections.singleton(RoleName.CUSTOMER));

        when(userRepository.findByUsername(usernameOrEmail)).thenReturn(Mono.just(dummyUser));
        when(userRepository.findByEmail(usernameOrEmail)).thenReturn(Mono.empty());

        Mono<com.bybud.entity.dto.UserDTO> resultMono = authGatewayService.getUserDetails(usernameOrEmail);

        StepVerifier.create(resultMono)
                .assertNext(userDTO -> {
                    assertEquals(dummyUser.getId(), userDTO.getId());
                    assertEquals(dummyUser.getUsername(), userDTO.getUsername());
                    assertEquals(dummyUser.getEmail(), userDTO.getEmail());
                    assertEquals(dummyUser.getFullName(), userDTO.getFullName());
                    assertEquals(dummyUser.getRoles(), userDTO.getRoles());
                })
                .verifyComplete();
    }
}