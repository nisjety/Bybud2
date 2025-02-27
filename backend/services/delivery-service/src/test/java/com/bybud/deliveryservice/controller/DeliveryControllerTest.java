package com.bybud.deliveryservice.controller;

import com.bybud.deliveryservice.service.DeliveryService;
import com.bybud.entity.model.DeliveryStatus;
import com.bybud.entity.request.CreateDeliveryRequest;
import com.bybud.entity.response.DeliveryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private DeliveryController deliveryController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        // Bind directly to the controller, apply the springSecurity() configurer, and register JSON codecs.
        webTestClient = WebTestClient.bindToController(deliveryController)
                .apply(springSecurity())
                .configureClient()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder());
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder());
                })
                .build();
    }

    @Test
    void createDelivery_Success() {
        // Arrange
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setCustomerId("customer123");
        request.setPickupAddress("123 Pickup St");
        request.setDeliveryAddress("456 Delivery Ave");
        request.setDeliveryDetails("Package contains electronics");
        request.setDeliveryDate(LocalDate.now().plusDays(1));

        DeliveryResponse mockResponse = new DeliveryResponse();
        mockResponse.setId("delivery123");
        mockResponse.setStatus(DeliveryStatus.CREATED);

        when(deliveryService.createDelivery(any(CreateDeliveryRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        // Act & Assert
        webTestClient
                .mutateWith(mockAuthentication(
                        new UsernamePasswordAuthenticationToken("customer123", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                ))
                .post()
                .uri("/api/delivery")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("delivery123")
                .jsonPath("$.data.status").isEqualTo("CREATED");
    }

    @Test
    void getDeliveriesForCustomer_Success() {
        // Arrange
        String customerId = "customer123";
        DeliveryResponse mockDelivery = new DeliveryResponse();
        mockDelivery.setId("delivery123");
        mockDelivery.setStatus(DeliveryStatus.PENDING);

        when(deliveryService.getDeliveriesForCustomer(eq(customerId)))
                .thenReturn(Flux.just(mockDelivery));

        // Act & Assert
        webTestClient
                .mutateWith(mockAuthentication(
                        new UsernamePasswordAuthenticationToken("customer123", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                ))
                .get()
                .uri("/api/delivery/customer/{customerId}", customerId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo("delivery123")
                .jsonPath("$.data[0].status").isEqualTo("PENDING");
    }

    @Test
    void getAllDeliveries_Success() {
        // Arrange
        String adminId = "admin123";
        DeliveryResponse mockDelivery = new DeliveryResponse();
        mockDelivery.setId("delivery123");
        mockDelivery.setStatus(DeliveryStatus.PENDING);

        when(deliveryService.getAllDeliveries(eq(adminId)))
                .thenReturn(Flux.just(mockDelivery));

        // Act & Assert
        webTestClient
                .mutateWith(mockAuthentication(
                        new UsernamePasswordAuthenticationToken("admin123", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                ))
                .get()
                .uri("/api/delivery")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo("delivery123")
                .jsonPath("$.data[0].status").isEqualTo("PENDING");
    }

    @Test
    void acceptDelivery_Success() {
        // Arrange
        String deliveryId = "delivery123";
        String courierId = "courier123";
        DeliveryResponse mockResponse = new DeliveryResponse();
        mockResponse.setId(deliveryId);
        mockResponse.setStatus(DeliveryStatus.ACCEPTED);
        mockResponse.setCourierId(courierId);

        when(deliveryService.acceptDelivery(eq(deliveryId), eq(courierId)))
                .thenReturn(Mono.just(mockResponse));

        // Act & Assert
        webTestClient
                .mutateWith(mockAuthentication(
                        new UsernamePasswordAuthenticationToken("courier123", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_COURIER")))
                ))
                .put()
                .uri("/api/delivery/{deliveryId}/accept", deliveryId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(deliveryId)
                .jsonPath("$.data.status").isEqualTo("ACCEPTED")
                .jsonPath("$.data.courierId").isEqualTo(courierId);
    }

    @Test
    void updateDeliveryStatus_Success() {
        // Arrange
        String deliveryId = "delivery123";
        DeliveryStatus status = DeliveryStatus.IN_PROGRESS;

        DeliveryResponse mockResponse = new DeliveryResponse();
        mockResponse.setId(deliveryId);
        mockResponse.setStatus(status);

        when(deliveryService.updateDeliveryStatus(eq(deliveryId), eq(status), eq("user123")))
                .thenReturn(Mono.just(mockResponse));

        // Act & Assert
        webTestClient
                .mutateWith(mockAuthentication(
                        new UsernamePasswordAuthenticationToken("user123", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                ))
                .put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/delivery/{deliveryId}/status")
                        .queryParam("status", status)
                        .build(deliveryId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo("delivery123")
                .jsonPath("$.data.status").isEqualTo("IN_PROGRESS");
    }
}
