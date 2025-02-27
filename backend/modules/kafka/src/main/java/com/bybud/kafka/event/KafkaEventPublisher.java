package com.bybud.kafka.event;

import com.bybud.kafka.producer.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(
            @Lazy KafkaProducerService kafkaProducerService,
            ObjectMapper objectMapper) {
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> publishEvent(String topic, Object event) {
        return Mono.fromCallable(() -> {
                    if (event instanceof String) {
                        return (String) event;
                    } else if (event instanceof Map) {
                        return objectMapper.writeValueAsString(event);
                    } else {
                        throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
                    }
                })
                .flatMap(message -> kafkaProducerService.sendMessage(topic, message));
    }
}