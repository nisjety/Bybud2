package com.bybud.entity.model;

/**
 * Represents the current status of a delivery.
 */
public enum DeliveryStatus {
    PENDING,
    CREATED,
    ACCEPTED,     // Courier has accepted the delivery
    ASSIGNED,      // Courier assigned to the customer
    IN_PROGRESS,   // Courier is picking up or delivering the item
    COMPLETED,     // Delivery completed
    CANCELLED      // Delivery canceled
}
