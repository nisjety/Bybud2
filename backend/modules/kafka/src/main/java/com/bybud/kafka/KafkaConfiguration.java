package com.bybud.kafka;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ComponentScan(basePackages = {
        "com.bybud.kafka.config",
        "com.bybud.kafka.handler",
        "com.bybud.kafka.producer"
})
public class KafkaConfiguration {
}