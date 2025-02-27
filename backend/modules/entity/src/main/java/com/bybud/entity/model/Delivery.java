package com.bybud.entity.model;

import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "deliveries")
public class Delivery extends BaseEntity {

    private String customerId;
    private String courierId;
    private String deliveryDetails;
    private String deliveryAddress;
    private String pickupAddress;
    private DeliveryStatus status = DeliveryStatus.CREATED;
    private LocalDate deliveryDate;

    public Delivery() {}

    public Delivery(String customerId, String deliveryDetails, String pickupAddress, String deliveryAddress) {
        this.customerId = customerId;
        this.deliveryDetails = deliveryDetails;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.status = DeliveryStatus.CREATED;
    }

    // Getters and setters

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

    public DeliveryStatus getStatus() {
        return status;
    }
    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }
    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    @Override
    public String toString() {
        return "Delivery{" +
                "id='" + getId() + '\'' +
                ", customerId='" + customerId + '\'' +
                ", courierId='" + courierId + '\'' +
                ", deliveryDetails='" + deliveryDetails + '\'' +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                ", pickupAddress='" + pickupAddress + '\'' +
                ", status=" + status +
                ", deliveryDate=" + deliveryDate +
                '}';
    }
}

