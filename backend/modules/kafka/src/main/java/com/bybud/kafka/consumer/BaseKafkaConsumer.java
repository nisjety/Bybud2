package com.bybud.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Kafka consumers that handles common configuration
 */
public abstract class BaseKafkaConsumer {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Creates Kafka receiver options with proper configuration and error handling
     *
     * @param consumerConfigs The base consumer configuration
     * @param topic The Kafka topic to subscribe to
     * @return Configured ReceiverOptions
     */
    protected ReceiverOptions<String, String> createConsumerOptions(Map<String, Object> consumerConfigs, String topic) {
        // Create a new copy of the config to avoid modifying the shared bean
        Map<String, Object> props = new HashMap<>(consumerConfigs);

        // Check if bootstrap.servers is set, if not set it explicitly
        if (!props.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG) ||
                props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG) == null ||
                props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG).toString().isEmpty() ||
                props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG).equals("[]")) {
            // Set default bootstrap servers
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            logger.info("Bootstrap servers was empty or [], setting default: localhost:9092");
        }

        // Ensure group.id is set (it's required)
        if (!props.containsKey(ConsumerConfig.GROUP_ID_CONFIG) ||
                props.get(ConsumerConfig.GROUP_ID_CONFIG) == null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, getDefaultConsumerGroup() + "-" + System.currentTimeMillis());
            logger.info("Group ID was not set, using generated ID");
        }

        // Explicitly set the deserializers here
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Auto offset reset if not set
        if (!props.containsKey(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)) {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        }

        // Log the final configuration
        logger.info("Kafka consumer config for topic {}: bootstrap.servers={}, group.id={}",
                topic, props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG),
                props.get(ConsumerConfig.GROUP_ID_CONFIG));

        return ReceiverOptions.<String, String>create(props)
                .subscription(Collections.singleton(topic))
                .addAssignListener(partitions ->
                        logger.info("Topic {}: Assigned partitions: {}", topic, partitions))
                .addRevokeListener(partitions ->
                        logger.info("Topic {}: Revoked partitions: {}", topic, partitions));
    }

    /**
     * Get the default consumer group name for this consumer
     * Should be overridden by subclasses
     */
    protected abstract String getDefaultConsumerGroup();
}