package com.bybud.deliveryservice;

import com.bybud.kafka.config.KafkaTopicsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.bybud")
@EnableScheduling
@EnableDiscoveryClient
@EnableConfigurationProperties(KafkaTopicsConfig.class)
@EnableReactiveMongoRepositories(basePackages = {"com.bybud.entity.repository"})
public class DeliveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}

