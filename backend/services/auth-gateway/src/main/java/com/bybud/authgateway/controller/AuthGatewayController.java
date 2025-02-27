package com.bybud.authgateway.controller;

import com.bybud.authgateway.service.AuthGatewayService;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.request.LoginRequest;
import com.bybud.entity.response.BaseResponse;
import com.bybud.entity.response.JwtResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller in the API Gateway
 * Handles authentication requests directly in the gateway
 */
@RestController
@RequestMapping("/api/auth")
public class AuthGatewayController {
    private static final Logger logger = LoggerFactory.getLogger(AuthGatewayController.class);

    private final AuthGatewayService authGatewayService;

    public AuthGatewayController(AuthGatewayService authGatewayService) {
        this.authGatewayService = authGatewayService;
    }

    /**
     * Login endpoint in the gateway
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<BaseResponse<JwtResponse>>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            ServerWebExchange exchange) {

        return authGatewayService.login(loginRequest.getUsernameOrEmail(), loginRequest.getPassword(), exchange)
                .map(jwtResponse ->
                        ResponseEntity.ok(BaseResponse.success("Login successful.", jwtResponse)))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.badRequest().body(BaseResponse.error("Login failed: " + e.getMessage()))
                ));
    }

    /**
     * Token refresh endpoint in the gateway
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<BaseResponse<JwtResponse>>> refresh(@RequestParam String refreshToken) {
        return authGatewayService.refreshToken(refreshToken)
                .map(jwtResponse ->
                        ResponseEntity.ok(BaseResponse.success("Token refreshed successfully.", jwtResponse)))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.badRequest().body(BaseResponse.error("Token refresh failed: " + e.getMessage()))
                ));
    }

    /**
     * User details endpoint in the gateway
     */
    @GetMapping("/user")
    public Mono<ResponseEntity<BaseResponse<UserDTO>>> getUserDetails(@RequestParam String usernameOrEmail) {
        return authGatewayService.getUserDetails(usernameOrEmail)
                .map(userDTO ->
                        ResponseEntity.ok(BaseResponse.success("User details fetched successfully.", userDTO)))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.badRequest().body(BaseResponse.error("Failed to fetch user details: " + e.getMessage()))
                ));
    }

    /**
     * Logout endpoint in the gateway
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<BaseResponse<Void>>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String refreshToken) {

        // Check if authorization header is valid
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            BaseResponse<Void> errorResponse = BaseResponse.error("Invalid authorization header");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        String accessToken = authHeader.substring(7);

        // Process logout and return appropriate response
        return authGatewayService.logout(accessToken, refreshToken)
                .then(Mono.defer(() -> {
                    BaseResponse<Void> successResponse = BaseResponse.success("Logout successful.", null);
                    return Mono.just(ResponseEntity.ok(successResponse));
                }))
                .onErrorResume(e -> {
                    BaseResponse<Void> errorResponse = BaseResponse.error("Logout failed: " + e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    /**
     * Token invalidation endpoint (for security purposes)
     */
    @PostMapping("/invalidate")
    public Mono<ResponseEntity<BaseResponse<Void>>> invalidateToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false, defaultValue = "Security measure") String reason) {

        // Check if authorization header is valid
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            BaseResponse<Void> errorResponse = BaseResponse.error("Invalid authorization header");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        String accessToken = authHeader.substring(7);

        // Process token invalidation
        return authGatewayService.invalidateToken(accessToken, reason)
                .then(Mono.defer(() -> {
                    BaseResponse<Void> successResponse = BaseResponse.success("Token invalidated successfully.", null);
                    return Mono.just(ResponseEntity.ok(successResponse));
                }))
                .onErrorResume(e -> {
                    BaseResponse<Void> errorResponse = BaseResponse.error("Token invalidation failed: " + e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-gateway");
        response.put("timestamp", System.currentTimeMillis());

        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * CORS test endpoint - Simple text response
     */
    @GetMapping("/cors-test")
    public Mono<ResponseEntity<String>> corsTest() {
        logger.debug("CORS test endpoint accessed");
        return Mono.just(ResponseEntity.ok("CORS is working if you can see this message"));
    }

    /**
     * CORS test endpoint - JSON response
     */
    @GetMapping("/cors-test-json")
    public Mono<ResponseEntity<BaseResponse<Map<String, Object>>>> corsTestJson() {
        logger.debug("CORS test JSON endpoint accessed");

        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "CORS is working with JSON data");
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("cors_enabled", true);

        return Mono.just(ResponseEntity.ok(
                BaseResponse.success("CORS test successful", testData)));
    }
}
