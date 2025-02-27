/*
package com.bybud.authservice.integration;

import com.bybud.authservice.config.TestRedisConfig;
import com.bybud.entity.model.RoleName;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import com.bybud.kafka.producer.KafkaProducerService;
import com.bybud.security.config.JwtTokenProvider;
import com.bybud.security.service.ReactiveTokenService;
import com.bybud.authservice.service.AuthService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        // Exclude the reactive Redis auto-configuration to avoid duplicate beans:
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
})
@Import({TestRedisConfig.class})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.consumer.group-id=test-group",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
public class AuthServiceFullIntegrationTest extends AbstractRedisIntegrationTest {

    // --- Dependencies ---
    @Autowired
    private AuthService authService;

    @Autowired
    private ReactiveTokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private KafkaProducerService kafkaProducerService;

    // --- Kafka Consumer Setup for Lockout Messages ---
    @Autowired
    private ConsumerFactory<String, String> kafkaConsumerFactory;

    private KafkaMessageListenerContainer<String, String> container;
    private BlockingQueue<ConsumerRecord<String, String>> records;

    // This topic name must match what AuthService sends to in handleAccountLockout()
    private static final String LOCKOUT_TOPIC = "auth-account-lockout-topic";

    /**
     * Test configuration to override the Kafka ConsumerFactory bean for our tests.
     *
    @TestConfiguration
    static class KafkaTestConfig {
        @Bean
        @Primary
        public ConsumerFactory<String, String> kafkaConsumerFactory() {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            return new DefaultKafkaConsumerFactory<>(props);
        }
    }

    // --- Test Setup ---
    @BeforeEach
    void setUp() {
        // Set up Kafka consumer container to capture lockout messages.
        records = new LinkedBlockingQueue<>();
        ContainerProperties containerProperties = new ContainerProperties(LOCKOUT_TOPIC);
        containerProperties.setGroupId("test-consumer-group");
        container = new KafkaMessageListenerContainer<>(kafkaConsumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, String>) record -> records.add(record));
        container.start();
        // Wait for the container to start (up to 10 seconds).
        long timeout = System.currentTimeMillis() + 10000;
        while (!container.isRunning() && System.currentTimeMillis() < timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Prepare a test user.
        User testUser = new User();
        testUser.setId("user123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setFullName("Test User");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUser.setRoles(Set.of(RoleName.CUSTOMER));

        // --- Mocks for Successful Login ---
        when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateJwtToken("testuser")).thenReturn("test-access-token");
        when(jwtTokenProvider.generateRefreshToken("testuser")).thenReturn("test-refresh-token");
        when(jwtTokenProvider.validateRefreshToken("test-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getSubjectFromJwt("test-refresh-token")).thenReturn("testuser");
    }

    @AfterEach
    void tearDown() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    // --- Tests ---

    @Test
    void loginAndRefreshToken_Success() {
        // Verify that a valid login returns a correct JwtResponse.
        StepVerifier.create(authService.login("testuser", "password123"))
                .assertNext(jwtResponse -> {
                    assertNotNull(jwtResponse);
                    assertEquals("test-access-token", jwtResponse.getAccessToken());
                    assertEquals("test-refresh-token", jwtResponse.getRefreshToken());
                    assertEquals("testuser", jwtResponse.getUsername());
                    assertEquals("test@example.com", jwtResponse.getEmail());
                    assertTrue(jwtResponse.getRoles().contains(RoleName.CUSTOMER));
                })
                .verifyComplete();

        // Verify that using the refresh token returns a new valid JwtResponse.
        StepVerifier.create(authService.refreshToken("test-refresh-token"))
                .assertNext(jwtResponse -> {
                    assertNotNull(jwtResponse);
                    assertEquals("test-access-token", jwtResponse.getAccessToken());
                    assertEquals("test-refresh-token", jwtResponse.getRefreshToken());
                    assertEquals("testuser", jwtResponse.getUsername());
                })
                .verifyComplete();
    }

    @Test
    void logout_Success() {
        // Verify that logout completes successfully.
        StepVerifier.create(authService.login("testuser", "password123")
                        .flatMap(jwtResponse -> authService.logout(jwtResponse.getAccessToken(), jwtResponse.getRefreshToken())))
                .verifyComplete();
    }

    @Test
    void getUserDetails_Success() {
        // Verify that fetching user details returns a proper UserDTO.
        StepVerifier.create(authService.getUserDetails("testuser"))
                .assertNext(userDTO -> {
                    assertNotNull(userDTO);
                    assertEquals("testuser", userDTO.getUsername());
                    assertEquals("Test User", userDTO.getFullName());
                    assertEquals("test@example.com", userDTO.getEmail());
                    assertTrue(userDTO.getRoles().contains(RoleName.CUSTOMER));
                })
                .verifyComplete();
    }

    @Test
    void loginFailure_ShouldSendLockoutMessage() throws InterruptedException {
        // For a login failure, force the password check to fail.
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        StepVerifier.create(authService.login("testuser", "wrongpassword"))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Invalid password."))
                .verify();

        // Poll Kafka for the lockout message.
        ConsumerRecord<String, String> record = records.poll(10, TimeUnit.SECONDS);
        assertNotNull(record, "Expected a lockout message to be sent");
        assertEquals(LOCKOUT_TOPIC, record.topic());
        assertTrue(record.value().contains("Account locked: testuser"));
    }
}

*/
