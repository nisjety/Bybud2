package com.bybud.entity.response;

import com.bybud.entity.model.DeliveryStatus;
import java.time.LocalDateTime;

/**
 * Response DTO for delivery details.
 * Used in the reactive controllers as part of a Mono stream.
 */
public class DeliveryResponse {

    private String id;
    private String customerId;
    private String customerName;
    private String courierId;
    private String courierUsername;
    private String deliveryDetails;
    private String pickupAddress;
    private String deliveryAddress;
    private DeliveryStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Getters and Setters

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCourierId() {
        return courierId;
    }
    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    public String getCourierUsername() {
        return courierUsername;
    }
    public void setCourierUsername(String courierUsername) {
        this.courierUsername = courierUsername;
    }

    public String getDeliveryDetails() {
        return deliveryDetails;
    }
    public void setDeliveryDetails(String deliveryDetails) {
        this.deliveryDetails = deliveryDetails;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }
    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public DeliveryStatus getStatus() {
        return status;
    }
    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

}
