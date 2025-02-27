package com.bybud.entity.dto;

import com.bybud.entity.model.DeliveryStatus;

import java.time.LocalDateTime;

/**
 * DTO representing delivery information.
 * This class works seamlessly in reactive pipelines.
 */
public class DeliveryDTO {

    private String id;
    private String deliveryDetails;
    private String deliveryAddress;
    private String pickupAddress;
    private String customerId;
    private String courierId;
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

    public String getDeliveryDetails() {
        return deliveryDetails;
    }
    public void setDeliveryDetails(String deliveryDetails) {
        this.deliveryDetails = deliveryDetails;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }
    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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
