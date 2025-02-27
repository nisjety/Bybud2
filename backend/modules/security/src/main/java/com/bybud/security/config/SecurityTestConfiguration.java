package com.bybud.security.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.bybud.security", "com.bybud.entity"})
@EnableMongoRepositories(basePackages = "com.bybud.entity.repository")
public class SecurityTestConfiguration {
    // This configuration now enables MongoDB repositories and scans the entity package
}