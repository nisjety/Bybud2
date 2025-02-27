/*package com.bybud.authservice.integration;

import com.bybud.authservice.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@Import(TestRedisConfig.class)
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.timeout=30000",
        "spring.data.redis.connect-timeout=30000"
})
public abstract class AbstractRedisIntegrationTest {

    @Autowired
    protected ReactiveRedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUpRedis() {
        // Clear Redis and verify connection
        StepVerifier.create(connectionFactory.getReactiveConnection()
                        .serverCommands()
                        .flushAll()
                        .then(connectionFactory.getReactiveConnection().ping()))
                .expectNext("PONG")
                .verifyComplete();
    }
}

 */

