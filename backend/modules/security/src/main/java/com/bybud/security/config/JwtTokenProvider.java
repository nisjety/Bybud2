package com.bybud.security.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecurityProperties securityProperties;

    public JwtTokenProvider(@NonNull SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        logger.info("JwtTokenProvider initialized with jwtSecret: {}, jwtExpirationMs: {}",
                securityProperties.getJwtSecret(),
                securityProperties.getJwtExpirationMs());
    }

    /**
     * --------------------------------------------------------------------------------
     * Synchronous Methods (for backward compatibility)
     * --------------------------------------------------------------------------------
     */

    public String generateJwtToken(@NonNull String username) {
        logger.debug("Generating JWT token for user: {}", username);
        // Default implementation without roles
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + securityProperties.getJwtExpirationMs()))
                .signWith(getSigningKey(securityProperties.getJwtSecret()))
                .compact();
    }

    /**
     * Generate JWT token with roles - synchronous version
     */
    public String generateJwtToken(@NonNull String username, @NonNull List<String> roles) {
        logger.debug("Generating JWT token for user: {} with roles: {}", username, roles);
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + securityProperties.getJwtExpirationMs()))
                .signWith(getSigningKey(securityProperties.getJwtSecret()))
                .compact();
    }

    public String getSubjectFromJwt(@NonNull String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey(securityProperties.getJwtSecret()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            logger.error("Error extracting subject from JWT: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateJwtToken(@NonNull String token) {
        try {
            logger.debug("Validating JWT token");
            Jwts.parser()
                    .verifyWith(getSigningKey(securityProperties.getJwtSecret()))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token has expired");
            return false;
        } catch (JwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String generateServiceJwtToken(@NonNull String serviceName) {
        logger.debug("Generating service JWT token for service: {}", serviceName);
        return Jwts.builder()
                .subject(serviceName)
                .claim("isService", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + securityProperties.getJwtExpirationMs()))
                .signWith(getSigningKey(securityProperties.getJwtSecret()))
                .compact();
    }

    public boolean validateServiceToken(@NonNull String token) {
        try {
            logger.debug("Validating service JWT token");
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey(securityProperties.getJwtSecret()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Boolean.TRUE.equals(claims.get("isService", Boolean.class));
        } catch (JwtException e) {
            logger.error("Service JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public List<String> getRolesFromJwt(@NonNull String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey(securityProperties.getJwtSecret()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check if roles claim exists
            if (!claims.containsKey("roles")) {
                logger.warn("JWT token does not contain roles claim");
                return Collections.singletonList("CUSTOMER"); // Default role
            }

            Object rolesObject = claims.get("roles");

            if (rolesObject instanceof List<?>) {
                return ((List<?>) rolesObject).stream()
                        .map(Object::toString)
                        .toList();
            } else if (rolesObject instanceof String) {
                // Handle CSV format
                String rolesStr = (String) rolesObject;
                return Arrays.asList(rolesStr.split(","));
            } else {
                // Default to CUSTOMER role if format is unexpected
                logger.warn("Unexpected roles format in JWT: {}", rolesObject);
                return Collections.singletonList("CUSTOMER");
            }
        } catch (Exception e) {
            logger.error("Error extracting roles from JWT: {}", e.getMessage());
            // Return default role instead of throwing exception
            return Collections.singletonList("CUSTOMER");
        }
    }

    public String generateJwtTokenWithRoles(@NonNull String username, @NonNull List<String> roles) {
        logger.debug("Generating JWT with roles for user: {}", username);
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + securityProperties.getJwtExpirationMs()))
                .signWith(getSigningKey(securityProperties.getJwtSecret()))
                .compact();
    }

    public String generateRefreshToken(@NonNull String username) {
        logger.debug("Generating refresh token for user: {}", username);
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + securityProperties.getJwtRefreshExpirationMs()))
                .signWith(getSigningKey(securityProperties.getJwtRefreshSecret()))
                .compact();
    }

    public boolean validateRefreshToken(@NonNull String token) {
        try {
            logger.debug("Validating refresh token");
            Jwts.parser()
                    .verifyWith(getSigningKey(securityProperties.getJwtRefreshSecret()))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            logger.error("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Reactive method to generate a JWT token with roles
     */
    public Mono<String> generateJwtTokenReactive(@NonNull String username, Mono<List<String>> rolesMono) {
        return rolesMono.map(roles -> {
            logger.debug("Generating JWT token for user: {} with roles: {}", username, roles);
            return Jwts.builder()
                    .subject(username)
                    .claim("roles", roles)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + securityProperties.getJwtExpirationMs()))
                    .signWith(getSigningKey(securityProperties.getJwtSecret()))
                    .compact();
        });
    }

    /**
     * Reactive wrapper for validateJwtToken.
     */
    public Mono<Boolean> validateJwtTokenReactive(@NonNull String token) {
        return Mono.fromCallable(() -> validateJwtToken(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Reactive wrapper for validateServiceToken.
     */
    public Mono<Boolean> validateServiceTokenReactive(@NonNull String token) {
        return Mono.fromCallable(() -> validateServiceToken(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Reactive wrapper to extract subject from JWT.
     */
    public Mono<String> getSubjectFromJwtReactive(@NonNull String token) {
        return Mono.fromCallable(() -> getSubjectFromJwt(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Reactive wrapper to extract roles from JWT.
     */
    public Mono<List<String>> getRolesFromJwtReactive(@NonNull String token) {
        return Mono.fromCallable(() -> getRolesFromJwt(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * --------------------------------------------------------------------------------
     * Signing Key
     * --------------------------------------------------------------------------------
     */

    private SecretKey getSigningKey(@NonNull String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
