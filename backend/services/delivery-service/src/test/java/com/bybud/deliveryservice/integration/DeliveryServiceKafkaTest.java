/*
package com.bybud.deliveryservice.integration;

import com.bybud.common.test.SharedTestContainers;
import com.bybud.entity.repository.DeliveryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        properties = {
                "bybud.security.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                "spring.data.mongodb.database=delivery",
                "bybud.kafka.topics.delivery-created=delivery_created_test",
                "bybud.kafka.topics.delivery-status-updated=delivery_status_updated_test",
                "auth-service.url=http://localhost:8081",
                "delivery-service.url=http://localhost:8082",
                "user-service.url=http://localhost:8083"
        }
)
@AutoConfigureWebTestClient
@WithMockUser(roles = "ADMIN")
@Import(DeliveryServiceKafkaTest.TestConfig.class)
class DeliveryServiceKafkaTest extends SharedTestContainers {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers("/api/**").permitAll()
                            .anyExchange().authenticated()
                    )
                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DeliveryRepository deliveryRepository;

    private KafkaMessageListenerContainer<String, String> container;
    private final BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();

    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int KAFKA_TIMEOUT_SECONDS = 15;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        deliveryRepository.deleteAll().block();
        records.clear();

        // Listen on the "delivery_created_test" topic for creation events
        setupKafkaContainer("delivery_created_test");
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        deliveryRepository.deleteAll().block();
    }

    private void setupKafkaContainer(String topic) {
        if (container != null && container.isRunning()) {
            container.stop();
        }

        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS);
        props.put("group.id", "test-group-" + UUID.randomUUID());
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", StringDeserializer.class);
        props.put("auto.offset.reset", "earliest");

        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener((MessageListener<String, String>) records::add);

        container = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(props),
                containerProps
        );
        container.start();

        // Wait for the container to be running
        await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(1))
                .until(container::isRunning);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDelivery_ShouldPublishKafkaEvent() {
        String deliveryRequest = """
            {
                "customerId": "customer123",
                "deliveryDetails": "Test package",
                "pickupAddress": "123 Pickup St",
                "deliveryAddress": "456 Delivery Ave",
                "deliveryDate": "2025-02-25"
            }
            """;

        // Create a delivery
        webTestClient.post()
                .uri("/api/delivery")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(deliveryRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.customerId").isEqualTo("customer123")
                .jsonPath("$.data.status").isEqualTo("CREATED")
                .jsonPath("$.data.id").exists();

        // Verify the creation event in Kafka
        await()
                .atMost(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
                    return record != null && record.value().contains("customer123");
                });

        // Verify Mongo
        StepVerifier.create(deliveryRepository.findByCustomerId("customer123"))
                .assertNext(delivery -> {
                    assertThat(delivery.getCustomerId()).isEqualTo("customer123");
                    assertThat(delivery.getStatus().toString()).isEqualTo("CREATED");
                })
                .verifyComplete();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateDeliveryStatus_ShouldPublishKafkaEvent() {
        // 1. Create a test delivery first
        String deliveryId = createTestDelivery();

        // 2. Switch Kafka listener to the "delivery_status_updated_test" topic
        setupKafkaContainer("delivery_status_updated_test");

        // 3. The controller requires 'status' and 'userId' as query parameters
        webTestClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/delivery/{id}/status")
                        .queryParam("status", "IN_PROGRESS")
                        .queryParam("userId", "adminUser") // Required by the method signature
                        .build(deliveryId)
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                // If the response is wrapped with "data", we look there
                .jsonPath("$.data.status").isEqualTo("IN_PROGRESS");

        // 4. Verify the Kafka message
        await()
                .atMost(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
                    return record != null
                            && record.value().contains(deliveryId)
                            && record.value().contains("IN_PROGRESS");
                });

        // 5. Verify Mongo status is updated
        StepVerifier.create(deliveryRepository.findById(deliveryId))
                .assertNext(delivery -> {
                    assertThat(delivery.getStatus().toString()).isEqualTo("IN_PROGRESS");
                })
                .verifyComplete();
    }

    // Helper method to create a test delivery and return its ID from the response

    private String createTestDelivery() {
        String deliveryRequest = """
            {
                "customerId": "customer123",
                "deliveryDetails": "Test package",
                "pickupAddress": "123 Pickup St",
                "deliveryAddress": "456 Delivery Ave",
                "deliveryDate": "2025-02-25"
            }
            """;

        // Perform the POST request
        String responseBody = webTestClient.post()
                .uri("/api/delivery")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(deliveryRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        // Parse the JSON to extract the ID
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            return root.get("data").get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse delivery response", e);
        }
    }
}
*/