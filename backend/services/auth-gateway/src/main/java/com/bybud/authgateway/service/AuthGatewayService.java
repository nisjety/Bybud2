package com.bybud.authgateway.service;

import com.bybud.common.exception.UserNotFoundException;
import com.bybud.entity.dto.UserCredentialsDTO;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import com.bybud.entity.response.BaseResponse;
import com.bybud.entity.response.JwtResponse;
import com.bybud.kafka.handler.AuthenticationEventHandler;
import com.bybud.security.config.JwtTokenProvider;
import com.bybud.security.service.ReactiveTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(AuthGatewayService.class);

    private final UserRepository userRepository;
    private final ReactiveTokenService tokenService;
    private final AuthenticationEventHandler eventHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final WebClient webClient;

    // Inject the internal secret from application.yml
    @Value("${internal.secret}")
    private String internalSecret;

    // Token TTL values
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    public AuthGatewayService(
            WebClient.Builder webClientBuilder,
            UserRepository userRepository,
            ReactiveTokenService tokenService,
            @Lazy AuthenticationEventHandler eventHandler,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder) {
        // Configure the WebClient with the base URL for your user service.
        this.webClient = webClientBuilder.baseUrl("http://localhost:8083").build();
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.eventHandler = eventHandler;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        logger.info("AuthGatewayService initialized");
    }

    /**
     * Securely fetch user credentials (including hashed password) from the user service.
     * The user service exposes a secure endpoint (e.g., GET /api/users/credentials?usernameOrEmail=...)
     * that returns a minimal User representation needed for authentication.
     */
    private Mono<User> fetchUserCredentials(String usernameOrEmail) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/credentials")
                        .queryParam("usernameOrEmail", usernameOrEmail)
                        .build())
                .header("X-INTERNAL-SECRET", internalSecret)
                .retrieve()
                .onStatus(code -> {
                    HttpStatus status = HttpStatus.resolve(code.value());
                    return status != null && status.is5xxServerError();
                }, response -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error occurred")))
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserCredentialsDTO>>() {})
                .map(BaseResponse::getData)
                .map(credentialsDTO -> {
                    User user = new User();
                    user.setId(credentialsDTO.getId());
                    user.setUsername(credentialsDTO.getUsername());
                    user.setPassword(credentialsDTO.getHashedPassword());
                    user.setRoles(credentialsDTO.getRoles());
                    return user;
                });
    }

    /**
     * Login user - now handled by securely retrieving credentials from the user service.
     */
    public Mono<JwtResponse> login(String usernameOrEmail, String password, ServerWebExchange exchange) {
        logger.info("Processing login request for user: {}", usernameOrEmail);
        return fetchUserCredentials(usernameOrEmail)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(user -> {
                    logger.debug("User {} fetched with hashed password: {}", user.getUsername(), user.getPassword());
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        String ipAddress = exchange.getRequest().getRemoteAddress() != null
                                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                                : "unknown";
                        publishAccountLockoutEvent(user.getUsername(), ipAddress);
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password."));
                    }

                    logger.debug("User {} authenticated successfully, generating tokens", user.getUsername());

                    // Extract roles as strings for JWT
                    Mono<List<String>> rolesMono = Mono.just(
                            user.getRoles().stream()
                                    .map(RoleName::name)
                                    .collect(Collectors.toList())
                    );

                    // Generate access token with roles using the reactive method
                    return jwtTokenProvider.generateJwtTokenReactive(user.getUsername(), rolesMono)
                            .flatMap(accessToken -> {
                                String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

                                return tokenService.storeToken(accessToken, ACCESS_TOKEN_TTL)
                                        .doOnSuccess(v -> logger.debug("Access token stored successfully"))
                                        .then(tokenService.storeToken(refreshToken, REFRESH_TOKEN_TTL))
                                        .doOnSuccess(v -> logger.debug("Refresh token stored successfully"))
                                        .doOnError(e -> logger.error("Failed to store tokens: {}", e.getMessage()))
                                        .doOnSuccess(v -> {
                                            String tokenSignature = getTokenSignature(accessToken);
                                            publishUserAuthenticatedEvent(user, tokenSignature);
                                        })
                                        .thenReturn(new JwtResponse(
                                                accessToken,
                                                refreshToken,
                                                user.getId(),
                                                user.getUsername(),
                                                user.getEmail(),
                                                user.getFullName(),
                                                user.getRoles()
                                        ));
                            });
                })
                .doOnError(e -> logger.error("Login failed: {}", e.getMessage()));
    }


    /**
     * Refresh token - still handled directly by the gateway.
     */
    public Mono<JwtResponse> refreshToken(String refreshToken) {
        logger.info("Processing token refresh request");
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token."));
        }

        return tokenService.isTokenActive(refreshToken)
                .flatMap(isActive -> {
                    if (!isActive) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is no longer active."));
                    }
                    String username = jwtTokenProvider.getSubjectFromJwt(refreshToken);
                    logger.debug("Refresh token valid for user: {}", username);
                    return userRepository.findByUsername(username)
                            .switchIfEmpty(Mono.error(new UserNotFoundException("User not found for refresh token.")))
                            .flatMap(user -> {
                                // Extract roles for JWT
                                Mono<List<String>> rolesMono = Mono.just(
                                        user.getRoles().stream()
                                                .map(RoleName::name)
                                                .collect(Collectors.toList())
                                );

                                // Generate token with roles
                                return jwtTokenProvider.generateJwtTokenReactive(username, rolesMono)
                                        .flatMap(newAccessToken -> {
                                            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
                                            String oldTokenSignature = getTokenSignature(refreshToken);
                                            String newTokenSignature = getTokenSignature(newAccessToken);

                                            return tokenService.storeToken(newAccessToken, ACCESS_TOKEN_TTL)
                                                    .doOnSuccess(v -> logger.debug("New access token stored successfully"))
                                                    .then(tokenService.storeToken(newRefreshToken, REFRESH_TOKEN_TTL))
                                                    .doOnSuccess(v -> logger.debug("New refresh token stored successfully"))
                                                    .then(tokenService.removeToken(refreshToken))
                                                    .doOnSuccess(v -> logger.debug("Old refresh token removed successfully"))
                                                    .doOnSuccess(v -> publishTokenRefreshedEvent(user, oldTokenSignature, newTokenSignature))
                                                    .thenReturn(new JwtResponse(
                                                            newAccessToken,
                                                            newRefreshToken,
                                                            user.getId(),
                                                            user.getUsername(),
                                                            user.getEmail(),
                                                            user.getFullName(),
                                                            user.getRoles()
                                                    ));
                                        });
                            });
                })
                .doOnError(e -> logger.error("Token refresh failed: {}", e.getMessage()));
    }

    /**
     * Logout - handled directly by the gateway.
     */
    public Mono<Void> logout(String accessToken, String refreshToken) {
        logger.info("Processing logout request");
        String username = jwtTokenProvider.getSubjectFromJwt(accessToken);
        logger.debug("Logging out user: {}", username);
        Mono<Void> removeAccess = tokenService.removeToken(accessToken).then().cache();
        Mono<Void> removeRefresh = tokenService.removeToken(refreshToken).then().cache();
        return removeAccess.then(removeRefresh)
                .doOnSuccess(v -> {
                    String tokenSignature = getTokenSignature(accessToken);
                    // Create a minimal user instance for event publishing
                    User minimalUser = new User();
                    minimalUser.setUsername(username);
                    publishUserLogoutEvent(minimalUser, tokenSignature);
                })
                .then()
                .doOnSuccess(v -> logger.info("Logout successful for user: {}", username))
                .doOnError(e -> logger.error("Logout failed: {}", e.getMessage()));
    }

    /**
     * Invalidate token - handled directly by the gateway.
     */
    public Mono<Void> invalidateToken(String accessToken, String reason) {
        logger.info("Processing token invalidation request");
        String username = jwtTokenProvider.getSubjectFromJwt(accessToken);
        logger.debug("Invalidating token for user: {} with reason: {}", username, reason);
        Mono<Void> removeAccess = tokenService.removeToken(accessToken).then().cache();
        return removeAccess.doOnSuccess(v -> {
                    String tokenSignature = getTokenSignature(accessToken);
                    User minimalUser = new User();
                    minimalUser.setUsername(username);
                    publishTokenInvalidatedEvent(minimalUser, tokenSignature, reason);
                })
                .then()
                .doOnSuccess(v -> logger.info("Token invalidated successfully for user: {}", username))
                .doOnError(e -> logger.error("Token invalidation failed: {}", e.getMessage()));
    }

    /**
     * Get user details - still handled using local repository.
     */
    public Mono<UserDTO> getUserDetails(String usernameOrEmail) {
        logger.info("Fetching user details for: {}", usernameOrEmail);
        return Mono.firstWithSignal(
                        userRepository.findByUsername(usernameOrEmail),
                        userRepository.findByEmail(usernameOrEmail)
                )
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found.")))
                .map(this::mapToUserDTO)
                .doOnSuccess(user -> logger.debug("User details retrieved successfully for: {}", usernameOrEmail))
                .doOnError(e -> logger.error("Failed to get user details: {}", e.getMessage()));
    }

    /**
     * Get token signature (last part of JWT) for reference without exposing the full token.
     */
    private String getTokenSignature(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        // Only extract signature if it's a JWT token (contains two dots)
        if (token.contains(".")) {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                return parts[2];
            }
        }
        // If not a valid JWT format, return the whole token
        return token;
    }

    private UserDTO mapToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setFullName(user.getFullName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setActive(user.isActive());
        userDTO.setDateOfBirth(user.getDateOfBirth());
        userDTO.setRoles(user.getRoles());
        return userDTO;
    }

    // Event publishing methods

    private void publishAccountLockoutEvent(String username, String ipAddress) {
        Mono.fromRunnable(() ->
                        eventHandler.publishAccountLockout(
                                new AuthenticationEventHandler.AccountLockoutEvent(username, 1, ipAddress))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> logger.error("Failed to publish account lockout event: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
    }

    private void publishUserAuthenticatedEvent(User user, String tokenSignature) {
        Mono.fromRunnable(() ->
                        eventHandler.publishUserAuthenticated(
                                new AuthenticationEventHandler.UserAuthenticatedEvent(
                                        user.getId(),
                                        user.getUsername(),
                                        user.getRoles(),
                                        tokenSignature))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> logger.error("Failed to publish user authenticated event: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
    }

    private void publishTokenRefreshedEvent(User user, String oldTokenSignature, String newTokenSignature) {
        Mono.fromRunnable(() ->
                        eventHandler.publishTokenRefreshed(
                                new AuthenticationEventHandler.TokenRefreshedEvent(
                                        user.getId(),
                                        user.getUsername(),
                                        oldTokenSignature,
                                        newTokenSignature))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> logger.error("Failed to publish token refreshed event: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
    }

    private void publishUserLogoutEvent(User user, String tokenSignature) {
        Mono.fromRunnable(() ->
                        eventHandler.publishUserLogout(
                                new AuthenticationEventHandler.UserLogoutEvent(
                                        user.getId(),
                                        user.getUsername(),
                                        tokenSignature))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> logger.error("Failed to publish user logout event: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
    }

    private void publishTokenInvalidatedEvent(User user, String tokenSignature, String reason) {
        Mono.fromRunnable(() ->
                        eventHandler.publishTokenInvalidated(
                                new AuthenticationEventHandler.TokenInvalidatedEvent(
                                        user.getId(),
                                        user.getUsername(),
                                        tokenSignature,
                                        reason))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> logger.error("Failed to publish token invalidated event: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
    }
}
