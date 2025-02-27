package com.bybud.entity.request;

import com.bybud.entity.model.DeliveryStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a delivery.
 * Designed for reactive endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateDeliveryRequest {

    @NotNull(message = "Delivery ID is required.")
    private String deliveryId;

    private String courierId;

    @NotNull(message = "Delivery status is required.")
    private DeliveryStatus status;

    // Getters and Setters
    public String getDeliveryId() {
        return deliveryId;
    }
    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getCourierId() {
        return courierId;
    }
    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }
    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }
}
