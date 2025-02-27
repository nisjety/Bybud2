package com.bybud.authgateway;

import com.bybud.authgateway.config.SecurityModuleConfig;
import com.bybud.kafka.config.KafkaTopicsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.bybud")
@EnableDiscoveryClient
@EnableConfigurationProperties(KafkaTopicsConfig.class)
@Import(SecurityModuleConfig.class)
@EnableReactiveMongoRepositories(basePackages = "com.bybud.entity.repository")
public class AuthGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthGatewayApplication.class, args);
    }
}