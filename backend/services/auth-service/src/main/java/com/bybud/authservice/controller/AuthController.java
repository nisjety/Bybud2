/*
package com.bybud.authservice.controller;

import com.bybud.authservice.service.AuthService;
import com.bybud.entity.dto.UserDTO;
import com.bybud.entity.request.LoginRequest;
import com.bybud.entity.response.BaseResponse;
import com.bybud.entity.response.JwtResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //Authenticates the user and generates a JWT (reactive).

    @PostMapping("/login")
    public Mono<ResponseEntity<BaseResponse<JwtResponse>>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest.getUsernameOrEmail(), loginRequest.getPassword())
                .map(jwtResponse ->
                        ResponseEntity.ok(BaseResponse.success("Login successful.", jwtResponse)))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.badRequest().body(BaseResponse.error("Login failed: " + e.getMessage()))
                ));
    }

    // Refreshes the JWT using the provided refresh token (reactive style).

    @PostMapping("/refresh")
    public Mono<ResponseEntity<BaseResponse<JwtResponse>>> refresh(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken)
                .map(jwtResponse ->
                        ResponseEntity.ok(BaseResponse.success("Token refreshed successfully.", jwtResponse)))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.badRequest().body(BaseResponse.error("Token refresh failed: " + e.getMessage()))
                ));
    }

    //Fetches user details by username or email (reactive).

    @GetMapping("/user")
    public Mono<ResponseEntity<BaseResponse<UserDTO>>> getUserDetails(@RequestParam String usernameOrEmail) {
        return authService.getUserDetails(usernameOrEmail)
                .map(userDTO ->
                        ResponseEntity.ok(BaseResponse.success("User details fetched successfully.", userDTO)))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.badRequest().body(BaseResponse.error("Failed to fetch user details: " + e.getMessage()))
                ));
    }

    @GetMapping("/api/auth/health")
    public Mono<String> healthCheck() {
        return Mono.just("AuthService is running");
    }
}

 */
