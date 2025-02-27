/*
package com.bybud.deliveryservice.integration;


import com.bybud.common.test.SharedTestContainers;
import com.bybud.entity.model.Delivery;
import com.bybud.entity.model.DeliveryStatus;
import com.bybud.entity.repository.DeliveryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.data.mongodb.database=delivery"
        }
)
class DeliveryRepositoryMongodbTest extends SharedTestContainers {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @BeforeEach
    void setup() {
        // Clear the collection before each test
        deliveryRepository.deleteAll().block();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        deliveryRepository.deleteAll().block();
    }

    @Test
    void saveAndFindByIdTest() {
        // Create a Delivery instance
        Delivery delivery = new Delivery();
        delivery.setCustomerId("customer123");
        delivery.setDeliveryDetails("Test package details");
        delivery.setPickupAddress("123 Pickup St");
        delivery.setDeliveryAddress("456 Delivery Ave");
        delivery.setStatus(DeliveryStatus.CREATED);
        // Assuming your Delivery entity has a field for delivery date
        delivery.setDeliveryDate(LocalDate.parse("2025-02-25"));

        // Save the delivery document in MongoDB
        StepVerifier.create(deliveryRepository.save(delivery))
                .assertNext(savedDelivery -> {
                    assertThat(savedDelivery.getId()).isNotNull();
                    assertThat(savedDelivery.getCustomerId()).isEqualTo("customer123");
                    assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.CREATED);
                })
                .verifyComplete();

        // Retrieve the document by its ID and verify its properties
        StepVerifier.create(deliveryRepository.findById(delivery.getId()))
                .assertNext(foundDelivery -> {
                    assertThat(foundDelivery.getId()).isEqualTo(delivery.getId());
                    assertThat(foundDelivery.getCustomerId()).isEqualTo("customer123");
                    assertThat(foundDelivery.getStatus()).isEqualTo(DeliveryStatus.CREATED);
                })
                .verifyComplete();
    }

    @Test
    void findByCustomerIdTest() {
        // Create two deliveries for the same customer
        Delivery delivery1 = new Delivery();
        delivery1.setCustomerId("customerABC");
        delivery1.setDeliveryDetails("Package A");
        delivery1.setPickupAddress("Address A");
        delivery1.setDeliveryAddress("Address B");
        delivery1.setStatus(DeliveryStatus.CREATED);
        delivery1.setDeliveryDate(LocalDate.now());

        Delivery delivery2 = new Delivery();
        delivery2.setCustomerId("customerABC");
        delivery2.setDeliveryDetails("Package B");
        delivery2.setPickupAddress("Address C");
        delivery2.setDeliveryAddress("Address D");
        delivery2.setStatus(DeliveryStatus.CREATED);
        delivery2.setDeliveryDate(LocalDate.now());

        // Save both deliveries
        StepVerifier.create(deliveryRepository.save(delivery1))
                .assertNext(saved -> assertThat(saved.getId()).isNotNull())
                .verifyComplete();

        StepVerifier.create(deliveryRepository.save(delivery2))
                .assertNext(saved -> assertThat(saved.getId()).isNotNull())
                .verifyComplete();

        // Query for deliveries by customerId and verify both are returned
        StepVerifier.create(deliveryRepository.findByCustomerId("customerABC"))
                .recordWith(java.util.ArrayList::new)
                .expectNextCount(2)
                .consumeRecordedWith(list -> {
                    assertThat(list).hasSize(2);
                    list.forEach(d -> assertThat(d.getCustomerId()).isEqualTo("customerABC"));
                })
                .verifyComplete();
    }
}
*/