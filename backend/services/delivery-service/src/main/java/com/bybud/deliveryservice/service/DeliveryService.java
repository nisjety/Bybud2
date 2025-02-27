package com.bybud.deliveryservice.service;

import com.bybud.common.exception.DeliveryNotFoundException;
import com.bybud.entity.mapper.DeliveryMapper;
import com.bybud.entity.model.Delivery;
import com.bybud.entity.model.DeliveryStatus;
import com.bybud.entity.model.User;
import com.bybud.entity.repository.DeliveryRepository;
import com.bybud.entity.repository.UserRepository;
import com.bybud.entity.request.CreateDeliveryRequest;
import com.bybud.entity.response.DeliveryResponse;
import com.bybud.kafka.handler.DeliveryEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryEventHandler eventHandler;
    private final UserRepository userRepository; // Injected to look up user details

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            DeliveryMapper deliveryMapper,
            @Lazy DeliveryEventHandler eventHandler,
            UserRepository userRepository) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryMapper = deliveryMapper;
        this.eventHandler = eventHandler;
        this.userRepository = userRepository;
    }

    public Flux<DeliveryResponse> getAllDeliveries(String userId) {
        return deliveryRepository.findAll()
                .flatMap(this::enrichDeliveryResponse);
    }

    public Mono<DeliveryResponse> createDelivery(CreateDeliveryRequest request) {
        return Mono.fromCallable(() -> deliveryMapper.toEntity(request))
                .flatMap(deliveryRepository::save)
                .doOnNext(saved -> {
                    logger.info("Delivery created with ID: {}", saved.getId());
                    // Publish an event for delivery creation
                    eventHandler.publishDeliveryCreated(
                            new DeliveryEventHandler.DeliveryCreatedEvent(saved.getId(), saved.getCustomerId())
                    );
                })
                .flatMap(this::enrichDeliveryResponse);
    }

    public Flux<DeliveryResponse> getDeliveriesForCustomer(String customerId) {
        return deliveryRepository.findByCustomerId(customerId)
                .flatMap(this::enrichDeliveryResponse);
    }

    public Flux<DeliveryResponse> getDeliveriesForCourier(String courierId) {
        return deliveryRepository.findByCourierId(courierId)
                .flatMap(this::enrichDeliveryResponse);
    }

    public Mono<DeliveryResponse> acceptDelivery(String deliveryId, String courierId) {
        return deliveryRepository.findById(deliveryId)
                .switchIfEmpty(Mono.error(new DeliveryNotFoundException("Delivery not found with ID: " + deliveryId)))
                .flatMap(delivery -> {
                    if (delivery.getStatus() != DeliveryStatus.CREATED) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Delivery cannot be accepted. Current status: " + delivery.getStatus()));
                    }
                    delivery.setCourierId(courierId);
                    delivery.setStatus(DeliveryStatus.ASSIGNED);
                    return deliveryRepository.save(delivery);
                })
                .doOnNext(this::accept)
                .flatMap(this::enrichDeliveryResponse);
    }

    public Mono<DeliveryResponse> updateDeliveryStatus(String deliveryId, DeliveryStatus status, String userId) {
        if (userId == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User ID is required to update delivery status."));
        }
        return deliveryRepository.findById(deliveryId)
                .switchIfEmpty(Mono.error(new DeliveryNotFoundException("Delivery not found with ID: " + deliveryId)))
                .flatMap(delivery -> {
                    delivery.setStatus(status);
                    return deliveryRepository.save(delivery);
                })
                .doOnNext(updated -> {
                    logger.info("Delivery {} status updated to {}", updated.getId(), updated.getStatus());
                    eventHandler.publishDeliveryStatusUpdated(
                            new DeliveryEventHandler.DeliveryStatusUpdatedEvent(updated.getId(), updated.getStatus().name())
                    );
                })
                .flatMap(this::enrichDeliveryResponse);
    }

    private void accept(Delivery updated) {
        logger.info("Delivery {} status updated to {}", updated.getId(), updated.getStatus());
        eventHandler.publishDeliveryStatusUpdated(
                new DeliveryEventHandler.DeliveryStatusUpdatedEvent(updated.getId(), updated.getStatus().name())
        );
    }

    /**
     * Enriches a DeliveryResponse by looking up the customer's full name and courier's username
     * using the userRepository.
     */
    private Mono<DeliveryResponse> enrichDeliveryResponse(Delivery delivery) {
        Mono<User> customerMono = userRepository.findById(delivery.getCustomerId());
        Mono<User> courierMono = (delivery.getCourierId() != null)
                ? userRepository.findById(delivery.getCourierId())
                : Mono.empty();

        return Mono.zip(customerMono.defaultIfEmpty(new User()), courierMono.defaultIfEmpty(new User()))
                .map(tuple -> {
                    User customer = tuple.getT1();
                    User courier = tuple.getT2();
                    DeliveryResponse response = deliveryMapper.toResponse(delivery);
                    response.setCustomerName(customer.getFullName());
                    response.setCourierUsername(courier.getUsername());
                    return response;
                });
    }
}
