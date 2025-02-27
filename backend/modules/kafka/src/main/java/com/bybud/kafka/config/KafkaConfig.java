package com.bybud.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaAdmin;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaTopicsConfig.class)
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    // Hardcoded fallback in case config is not loaded
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";

    private final Environment env;

    public KafkaConfig(Environment env) {
        this.env = env;
        logger.info("Initializing KafkaConfig");
    }

    // Producer Configuration
    @Bean
    @Primary
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();

        // Use a default value if property is missing
        String bootstrapServers = env.getProperty("spring.kafka.bootstrap-servers");

        // Check if value is missing, empty, or []
        if (bootstrapServers == null || bootstrapServers.isBlank() || bootstrapServers.equals("[]")) {
            bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
            logger.warn("No bootstrap servers configured, using hardcoded default: {}", bootstrapServers);
        }

        logger.info("Kafka bootstrap servers: {}", bootstrapServers);

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        logger.debug("Producer config: {}", props);
        return props;
    }

    // Create a separate configuration map for admin to break circular dependency
    @Bean
    public Map<String, Object> adminConfigs() {
        Map<String, Object> props = new HashMap<>();
        String bootstrapServers = env.getProperty("spring.kafka.bootstrap-servers");

        // Check if value is missing, empty, or []
        if (bootstrapServers == null || bootstrapServers.isBlank() || bootstrapServers.equals("[]")) {
            bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
            logger.warn("No bootstrap servers configured for admin, using hardcoded default: {}", bootstrapServers);
        }

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return props;
    }

    @Bean
    public KafkaSender<String, String> kafkaSender(@Qualifier("producerConfigs") Map<String, Object> producerConfigs) {
        logger.info("Creating KafkaSender bean with bootstrap.servers: {}",
                producerConfigs.getOrDefault(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "NOT SET!"));

        // Make a defensive copy to ensure no modifications
        Map<String, Object> configs = new HashMap<>(producerConfigs);

        // Double-check bootstrap servers is set with a valid value
        Object bootstrapServers = configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        if (bootstrapServers == null || bootstrapServers.toString().isEmpty() ||
                bootstrapServers.toString().equals("[]")) {
            configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEFAULT_BOOTSTRAP_SERVERS);
            logger.warn("Bootstrap servers still not set in configs, using hardcoded default: {}",
                    DEFAULT_BOOTSTRAP_SERVERS);
        }

        // Double-check serializers are set properly
        if (!configs.containsKey(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)) {
            configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        }
        if (!configs.containsKey(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)) {
            configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        }

        SenderOptions<String, String> senderOptions = SenderOptions.<String, String>create(configs)
                .maxInFlight(1024);

        return KafkaSender.create(senderOptions);
    }

    // Consumer Base Configuration
    @Bean
    public Map<String, Object> baseConsumerConfig() {
        Map<String, Object> props = new HashMap<>();

        // Provide a default value if not set
        String bootstrapServers = env.getProperty("spring.kafka.bootstrap-servers");

        // Check if value is missing, empty, or []
        if (bootstrapServers == null || bootstrapServers.isBlank() || bootstrapServers.equals("[]")) {
            bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
            logger.warn("No bootstrap servers configured for consumer, using hardcoded default: {}", bootstrapServers);
        }

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Optionally, set a default group id if one is not provided in properties
        props.put(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("spring.kafka.consumer.group-id", "bybud-consumer-group"));
        return props;
    }

    // Consumer Configurations for Different Groups
    @Bean("authConsumerConfigs")
    public Map<String, Object> authConsumerConfigs(@Qualifier("baseConsumerConfig") Map<String, Object> baseConfig) {
        Map<String, Object> config = new HashMap<>(baseConfig);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-group-" + System.currentTimeMillis());
        return config;
    }

    @Bean("userConsumerConfigs")
    public Map<String, Object> userConsumerConfigs(@Qualifier("baseConsumerConfig") Map<String, Object> baseConfig) {
        Map<String, Object> config = new HashMap<>(baseConfig);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "user-group-" + System.currentTimeMillis());
        return config;
    }

    @Bean
    public KafkaAdmin kafkaAdmin(@Qualifier("adminConfigs") Map<String, Object> adminConfigs) {
        logger.info("Creating KafkaAdmin bean with bootstrap.servers: {}",
                adminConfigs.getOrDefault(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "NOT SET!"));
        return new KafkaAdmin(adminConfigs);
    }
}