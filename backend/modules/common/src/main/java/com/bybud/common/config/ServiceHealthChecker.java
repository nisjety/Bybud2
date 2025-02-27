package com.bybud.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ServiceHealthChecker {

    private final WebClient webClient;
    private final String authGatewayUrl;
    private final String userServiceUrl;
    private final String deliveryServiceUrl;

    public ServiceHealthChecker(
            WebClient.Builder webClientBuilder,
            @Value("${service.urls.auth-gateway}") String authGatewayUrl,
            @Value("${service.urls.user-service}") String userServiceUrl,
            @Value("${service.urls.delivery-service}") String deliveryServiceUrl) {
        this.webClient = webClientBuilder.build();
        this.authGatewayUrl = authGatewayUrl;
        this.userServiceUrl = userServiceUrl;
        this.deliveryServiceUrl = deliveryServiceUrl;
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void checkHealth() {
        checkService(authGatewayUrl + "/api/auth/health", "AuthGateway");
        checkService(userServiceUrl + "/api/users/health", "UserService");
        checkService(deliveryServiceUrl + "/api/delivery/health", "DeliveryService");
    }

    private void checkService(String url, String serviceName) {
        webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> System.out.println(serviceName + " is available: " + response),
                        error -> System.err.println(serviceName + " is unavailable: " + error.getMessage())
                );
    }
}
