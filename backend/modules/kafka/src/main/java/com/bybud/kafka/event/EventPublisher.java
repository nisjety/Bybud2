package com.bybud.kafka.event;

import reactor.core.publisher.Mono;

public interface EventPublisher {
    Mono<Void> publishEvent(String topic, Object event);
}