package com.bybud.entity.mapper;

import com.bybud.entity.model.Delivery;
import com.bybud.entity.model.DeliveryStatus;
import com.bybud.entity.request.CreateDeliveryRequest;
import com.bybud.entity.response.DeliveryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {

    private final ObjectMapper objectMapper;

    public DeliveryMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Delivery toEntity(CreateDeliveryRequest request) {
        Delivery delivery = new Delivery();
        delivery.setCustomerId(request.getCustomerId());
        delivery.setDeliveryDetails(request.getDeliveryDetails());
        delivery.setPickupAddress(request.getPickupAddress());
        delivery.setDeliveryAddress(request.getDeliveryAddress());
        delivery.setStatus(DeliveryStatus.CREATED);
        return delivery;
    }

    public DeliveryResponse toResponse(Delivery delivery) {
        DeliveryResponse response = new DeliveryResponse();
        response.setId(delivery.getId());
        response.setCustomerId(delivery.getCustomerId());
        response.setCustomerName(null); // To be set elsewhere if needed.
        response.setCourierId(delivery.getCourierId());
        response.setCourierUsername(null); // To be set elsewhere if needed.
        response.setDeliveryDetails(delivery.getDeliveryDetails());
        response.setPickupAddress(delivery.getPickupAddress());
        response.setDeliveryAddress(delivery.getDeliveryAddress());
        response.setStatus(delivery.getStatus());
        response.setCreatedDate(delivery.getCreatedDate());
        response.setUpdatedDate(delivery.getUpdatedDate());
        return response;
    }

    public String toJson(Delivery delivery) {
        try {
            return objectMapper.writeValueAsString(delivery);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize delivery to JSON", e);
        }
    }
}
