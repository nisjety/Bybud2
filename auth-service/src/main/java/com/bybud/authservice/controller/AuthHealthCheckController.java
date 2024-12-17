package com.bybud.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthHealthCheckController {
    @GetMapping("/health")
    public String healthCheck() {
        return "AuthService is running";
    }
}
