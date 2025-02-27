/*package com.bybud.authservice;

import com.bybud.kafka.config.KafkaTopicsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication(
        scanBasePackages = "com.bybud",
        exclude = {
                WebMvcAutoConfiguration.class,
                SecurityAutoConfiguration.class
        }
)
@EnableScheduling
@EnableDiscoveryClient
@EnableConfigurationProperties(KafkaTopicsConfig.class)
@EnableReactiveMongoRepositories(basePackages = "com.bybud.entity.repository")
@ComponentScan(basePackages = {
        "com.bybud.authservice",
        "com.bybud.common",
        "com.bybud.security",
        "com.bybud.kafka",
        "com.bybud.entity.repository"
})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

 */






