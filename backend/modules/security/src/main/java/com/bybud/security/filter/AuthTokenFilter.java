package com.bybud.security.filter;

import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import com.bybud.security.service.ReactiveTokenService;
import com.bybud.security.config.JwtTokenProvider;
import com.bybud.security.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthTokenFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveTokenService tokenService;
    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;

    public AuthTokenFilter(
            JwtTokenProvider jwtTokenProvider,
            ReactiveTokenService tokenService,
            SecurityProperties securityProperties,
            UserRepository userRepository // Add this dependency
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
        this.securityProperties = securityProperties;
        this.userRepository = userRepository;
        logger.info("AuthTokenFilter initialized with excluded paths: {}", securityProperties.getExcludedPaths());
    }

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String requestURI = exchange.getRequest().getURI().getPath();

        // Skip JWT processing for excluded paths
        if (shouldSkipAuthentication(requestURI)) {
            logger.debug("Skipping JWT for path: {}", requestURI);
            return chain.filter(exchange);
        }

        // Extract JWT from "Authorization" header (if present)
        String jwt = extractJwtFromRequest(exchange);
        if (jwt == null) {
            return chain.filter(exchange);
        }

        // First validate JWT signature and expiration using reactive method
        return jwtTokenProvider.validateJwtTokenReactive(jwt)
                .flatMap(isValid -> {
                    if (!isValid) {
                        logger.warn("JWT validation failed for token");
                        return unauthorizedResponse(exchange, "Invalid token");
                    }

                    // Check if token is active in Redis
                    return tokenService.isTokenActive(jwt)
                            .flatMap(isActive -> {
                                if (!isActive) {
                                    logger.warn("Token is not active in Redis");
                                    return unauthorizedResponse(exchange, "Token is not active");
                                }

                                // Check if token is blacklisted
                                return tokenService.isTokenBlacklisted(jwt)
                                        .flatMap(isBlacklisted -> {
                                            if (isBlacklisted) {
                                                logger.warn("Token is blacklisted");
                                                return unauthorizedResponse(exchange, "Token has been revoked");
                                            }

                                            // Process valid token
                                            return processValidToken(jwt, exchange, chain);
                                        });
                            });
                })
                .onErrorResume(e -> {
                    logger.error("Authentication error: {}", e.getMessage(), e);
                    return unauthorizedResponse(exchange, "Authentication failed");
                });
    }

    private Mono<Void> processValidToken(String jwt, ServerWebExchange exchange, WebFilterChain chain) {
        // Retrieve subject (username) and roles reactively
        Mono<String> usernameMono = jwtTokenProvider.getSubjectFromJwtReactive(jwt);
        Mono<List<String>> rolesMono = jwtTokenProvider.getRolesFromJwtReactive(jwt);

        return Mono.zip(usernameMono, rolesMono)
                .flatMap(tuple -> {
                    String username = tuple.getT1();
                    List<String> roles = tuple.getT2();
                    logger.info("Valid JWT token for user: {} with roles: {}", username, roles);

                    // Convert roles to authorities
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    // Look up the user to get their full name
                    return userRepository.findByUsername(username)
                            .defaultIfEmpty(new User()) // Fallback if user not found
                            .flatMap(user -> {
                                // Get full name from user or default to username
                                String fullName = user.getFullName() != null ? user.getFullName() : username;

                                // Add user information to request headers for downstream services
                                ServerWebExchange modifiedExchange = exchange.mutate()
                                        .request(exchange.getRequest().mutate()
                                                .header("X-User-Name", username)
                                                .header("X-User-Full-Name", fullName)
                                                .header("X-User-Id", user.getId() != null ? user.getId() : username)
                                                .header("X-User-Roles", String.join(",", roles))
                                                .build())
                                        .build();

                                // Set authentication context
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                                return chain.filter(modifiedExchange)
                                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                            });
                });
    }

    // Rest of the methods remain the same

    private boolean shouldSkipAuthentication(String requestUri) {
        List<String> excludedPaths = securityProperties.getExcludedPaths();
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            return false;
        }

        // Cleanup path before matching
        String normalizedPath = normalizeRequestPath(requestUri);

        // Check for exact matches
        if (excludedPaths.contains(normalizedPath)) {
            logger.debug("Path excluded by exact match: {}", normalizedPath);
            return true;
        }

        // Check for pattern matches (e.g., /api/path/**)
        for (String excludedPath : excludedPaths) {
            if (excludedPath.endsWith("/**")) {
                String prefix = excludedPath.substring(0, excludedPath.length() - 3);
                if (normalizedPath.startsWith(prefix)) {
                    logger.debug("Path excluded by pattern match: {} with prefix: {}", normalizedPath, prefix);
                    return true;
                }
            }
        }

        logger.debug("Path not excluded: {}", normalizedPath);
        return false;
    }

    private String normalizeRequestPath(String path) {
        // Remove query parameters if present
        int queryParamIndex = path.indexOf('?');
        if (queryParamIndex != -1) {
            path = path.substring(0, queryParamIndex);
        }

        // Remove trailing slash if present (except for root path "/")
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    private @Nullable String extractJwtFromRequest(@NonNull ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorizedResponse(@NonNull ServerWebExchange exchange, @NonNull String message) {
        logger.warn("Unauthorized: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Error-Message", message);
        return exchange.getResponse().setComplete();
    }
}
