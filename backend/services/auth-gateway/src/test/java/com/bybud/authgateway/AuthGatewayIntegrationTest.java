package com.bybud.authgateway;

import com.bybud.entity.model.User;
import com.bybud.entity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class AuthGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @MockBean
    private ReactiveRedisTemplate<String, Object> redisObjectTemplate;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private final String testUsername = "testuser";
}