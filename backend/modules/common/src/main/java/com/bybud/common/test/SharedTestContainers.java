package com.bybud.common.test;


import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public abstract class SharedTestContainers {
    private static final Logger log = LoggerFactory.getLogger(SharedTestContainers.class);
    private static final int MAX_WAIT_TIME_SECONDS = 120;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private static final String MONGODB_URI = "mongodb://localhost:27017";
    private static final String KAFKA_URI = "localhost:9092";
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // MongoDB configuration
        registry.add("spring.data.mongodb.uri", () -> MONGODB_URI);
        registry.add("spring.data.mongodb.database", () -> "user");
        registry.add("spring.data.mongodb.repositories.type", () -> "reactive");


        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", () -> KAFKA_URI);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> KAFKA_URI);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> KAFKA_URI);
        registry.add("spring.kafka.consumer.group-id", () ->
                "test-group-" + System.currentTimeMillis());
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.producer.properties.enable.idempotence", () -> "true");
        registry.add("spring.kafka.producer.properties.max.in.flight.requests.per.connection", () -> "5");

        // Redis configuration
        registry.add("spring.redis.host", () -> REDIS_HOST);
        registry.add("spring.redis.port", () -> REDIS_PORT);
        registry.add("spring.redis.timeout", () -> "2000");
    }

    @BeforeAll
    public static void waitForServices() {
        log.info("Starting to check external services...");
        waitForMongoDB();
        waitForKafka();
        waitForRedis();
        log.info("All external services are ready");
    }

    private static void waitForMongoDB() {
        try {
            await()
                    .atMost(MAX_WAIT_TIME_SECONDS, TimeUnit.SECONDS)
                    .pollInterval(POLL_INTERVAL)
                    .ignoreExceptions()
                    .until(() -> {
                        try {
                            log.debug("Attempting to connect to MongoDB...");

                            MongoClient mongoClient = MongoClients.create(MONGODB_URI);
                            ReactiveMongoTemplate mongoTemplate = new ReactiveMongoTemplate(mongoClient, "user");

                            return mongoTemplate.executeCommand(new Document("ping", 1))
                                    .doOnNext(result -> log.debug("Ping result: {}", result))
                                    .map(doc -> doc.getDouble("ok") == 1.0)
                                    .doOnError(error -> log.error("Error during ping: ", error))
                                    .onErrorReturn(false)
                                    .block(TIMEOUT);
                        } catch (Exception e) {
                            log.error("MongoDB connection attempt failed", e);
                            return false;
                        }
                    });
            log.info("MongoDB connection established successfully");
        } catch (Exception e) {
            log.error("Failed to establish MongoDB connection", e);
            throw new RuntimeException("Failed to establish MongoDB connection", e);
        }
    }

    private static void waitForKafka() {
        try {
            await()
                    .atMost(MAX_WAIT_TIME_SECONDS, TimeUnit.SECONDS)
                    .pollInterval(POLL_INTERVAL)
                    .ignoreExceptions()
                    .until(() -> {
                        try (AdminClient admin = AdminClient.create(Map.of(
                                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_URI,
                                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000"))) {
                            return admin.listTopics()
                                    .names()
                                    .get(5, TimeUnit.SECONDS) != null;
                        } catch (Exception e) {
                            log.warn("Kafka connection attempt failed: {}", e.getMessage());
                            return false;
                        }
                    });
            log.info("Kafka connection established successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to establish Kafka connection", e);
        }
    }

    private static void waitForRedis() {
        try {
            await()
                    .atMost(MAX_WAIT_TIME_SECONDS, TimeUnit.SECONDS)
                    .pollInterval(POLL_INTERVAL)
                    .ignoreExceptions()
                    .until(() -> {
                        try {
                            RedisStandaloneConfiguration config =
                                    new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT);
                            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
                            factory.afterPropertiesSet();

                            StringRedisSerializer serializer = new StringRedisSerializer();
                            RedisSerializationContext<String, String> serializationContext =
                                    RedisSerializationContext.<String, String>newSerializationContext()
                                            .key(serializer)
                                            .value(serializer)
                                            .hashKey(serializer)
                                            .hashValue(serializer)
                                            .build();

                            ReactiveRedisTemplate<String, String> template =
                                    new ReactiveRedisTemplate<>(factory, serializationContext);

                            return template.opsForValue()
                                    .set("health-check", "OK")
                                    .then(template.opsForValue().get("health-check"))
                                    .map("OK"::equals)
                                    .defaultIfEmpty(false)
                                    .block(TIMEOUT);
                        } catch (Exception e) {
                            log.warn("Redis connection attempt failed: {}", e.getMessage());
                            return false;
                        }
                    });
            log.info("Redis connection established successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to establish Redis connection", e);
        }
    }
}