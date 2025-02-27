package com.bybud.deliveryservice.controller;

import com.bybud.deliveryservice.service.DeliveryService;
import com.bybud.entity.model.DeliveryStatus;
import com.bybud.entity.request.CreateDeliveryRequest;
import com.bybud.entity.response.BaseResponse;
import com.bybud.entity.response.DeliveryResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);
    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /**
     * Creates a new delivery.
     * Accessible to customers and admins.
     */
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping
    public Mono<ResponseEntity<BaseResponse<DeliveryResponse>>> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request) {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(userId -> {
                    // Set the customer ID from the authenticated user if not provided
                    if (request.getCustomerId() == null) {
                        request.setCustomerId(userId);
                    }
                    // Verify user has permission to create for this customer ID
                    return hasPermissionForCustomer(userId, request.getCustomerId())
                            .flatMap(hasPermission -> {
                                if (!hasPermission) {
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.FORBIDDEN, "Cannot create deliveries for other customers"));
                                }
                                return deliveryService.createDelivery(request);
                            });
                })
                .map(response -> ResponseEntity.ok(
                        BaseResponse.success("Delivery created successfully.", response)))
                .doOnError(error -> logger.error("Error creating delivery: {}", error.getMessage()));
    }

    /**
     * Gets all deliveries for a specific customer.
     * Customers can only see their own deliveries, admins can see any customer's deliveries.
     */
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<List<DeliveryResponse>>>> getDeliveriesForCustomer(
            @PathVariable("customerId") String customerId) {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(userId -> hasPermissionForCustomer(userId, customerId))
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Cannot view deliveries for other customers"));
                    }
                    return deliveryService.getDeliveriesForCustomer(customerId)
                            .collectList()
                            .map(list -> ResponseEntity.ok(
                                    BaseResponse.success("Customer deliveries fetched successfully.", list)));
                })
                .doOnError(error -> logger.error("Error fetching customer deliveries: {}", error.getMessage()));
    }

    /**
     * Gets all deliveries in the system.
     * Only accessible to admins and couriers.
     */
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    @GetMapping
    public Mono<ResponseEntity<BaseResponse<List<DeliveryResponse>>>> getAllDeliveries() {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(userId -> deliveryService.getAllDeliveries(userId)
                        .collectList()
                        .map(list -> ResponseEntity.ok(
                                BaseResponse.success("All deliveries fetched successfully.", list))))
                .doOnError(error -> logger.error("Error fetching all deliveries: {}", error.getMessage()));
    }

    /**
     * Gets all deliveries for a specific courier.
     * Couriers can only see their own deliveries, admins can see any courier's deliveries.
     */
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    @GetMapping("/courier/{courierId}")
    public Mono<ResponseEntity<BaseResponse<List<DeliveryResponse>>>> getDeliveriesForCourier(
            @PathVariable("courierId") String courierId) {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(userId -> hasPermissionForCourier(userId, courierId))
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "Cannot view deliveries for other couriers"));
                    }
                    return deliveryService.getDeliveriesForCourier(courierId)
                            .collectList()
                            .map(list -> ResponseEntity.ok(
                                    BaseResponse.success("Courier deliveries fetched successfully.", list)));
                })
                .doOnError(error -> logger.error("Error fetching courier deliveries: {}", error.getMessage()));
    }

    /**
     * Allows a courier to accept a delivery.
     * Couriers can only accept deliveries that are unassigned.
     */
    @PreAuthorize("hasRole('COURIER')")
    @PutMapping("/{deliveryId}/accept")
    public Mono<ResponseEntity<BaseResponse<DeliveryResponse>>> acceptDelivery(
            @PathVariable("deliveryId") String deliveryId) {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(courierId -> deliveryService.acceptDelivery(deliveryId, courierId))
                .map(response -> ResponseEntity.ok(
                        BaseResponse.success("Delivery accepted successfully by courier.", response)))
                .doOnError(error -> logger.error("Error accepting delivery: {}", error.getMessage()));
    }

    /**
     * Updates the status of a delivery.
     * Couriers can update their assigned deliveries, customers can only request cancellations,
     * admins can update any delivery.
     */
    @PreAuthorize("hasAnyRole('COURIER', 'CUSTOMER', 'ADMIN')")
    @PutMapping("/{deliveryId}/status")
    public Mono<ResponseEntity<BaseResponse<DeliveryResponse>>> updateDeliveryStatus(
            @PathVariable("deliveryId") String deliveryId,
            @RequestParam("status") DeliveryStatus status) {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(userId -> deliveryService.updateDeliveryStatus(deliveryId, status, userId))
                .map(response -> ResponseEntity.ok(
                        BaseResponse.success("Delivery status updated successfully.", response)))
                .doOnError(error -> logger.error("Error updating delivery status: {}", error.getMessage()));
    }

    /**
     * Health check endpoint.
     * Not secured, accessible to anyone.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("DeliveryService is running"));
    }

    /**
     * Helper method to determine if a user has permission to access a specific entity.
     * Admins have access to all entities, users only to their own.
     *
     * @param userId The ID of the authenticated user
     * @param entityId The ID of the entity being accessed (customer or courier)
     * @return Mono<Boolean> indicating if access is permitted
     */
    private Mono<Boolean> hasPermission(String userId, String entityId) {
        // If the user is the entity owner, they have permission
        if (userId.equals(entityId)) {
            return Mono.just(true);
        }

        // Otherwise, check if they have ADMIN role
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
                    return Mono.just(isAdmin);
                })
                .defaultIfEmpty(false);
    }

    /**
     * Helper method to determine if a user has permission to access a customer's deliveries.
     */
    private Mono<Boolean> hasPermissionForCustomer(String userId, String customerId) {
        return hasPermission(userId, customerId);
    }

    /**
     * Helper method to determine if a user has permission to access a courier's deliveries.
     */
    private Mono<Boolean> hasPermissionForCourier(String userId, String courierId) {
        return hasPermission(userId, courierId);
    }
}
