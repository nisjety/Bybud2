package com.bybud.entity.repository;

import com.bybud.entity.model.Delivery;
import com.bybud.entity.model.DeliveryStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DeliveryRepository extends ReactiveMongoRepository<Delivery, String> {

    // Find deliveries by customerId (as String)
    Flux<Delivery> findByCustomerId(String customerId);

    // Find deliveries by courierId (as String)
    Flux<Delivery> findByCourierId(String courierId);

    // Find deliveries by status
    Flux<Delivery> findByStatus(DeliveryStatus status);

    // Find a delivery by delivery address
    Mono<Delivery> findByDeliveryAddress(String deliveryAddress);

    // Find a delivery by pickup address
    Mono<Delivery> findByPickupAddress(String pickupAddress);

    // Find a delivery by ID and status
    Mono<Delivery> findByIdAndStatus(String id, DeliveryStatus status);

    // Find deliveries by customerId and status
    Flux<Delivery> findByCustomerIdAndStatus(String customerId, DeliveryStatus status);

    // Find deliveries by courierId and status
    Flux<Delivery> findByCourierIdAndStatus(String courierId, DeliveryStatus status);

    // Check if a delivery is assigned to a given courier
    Mono<Boolean> existsByIdAndCourierId(String id, String courierId);
}
