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
import com.bybud.kafka.config.KafkaTopicsConfig;
import com.bybud.kafka.handler.DeliveryEventHandler;
import com.bybud.kafka.producer.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

public class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private KafkaTopicsConfig kafkaTopicsConfig;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliveryEventHandler eventHandler;

    @InjectMocks
    private DeliveryService deliveryService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Test getAllDeliveries: mapping a Flux of deliveries to enriched responses
    @Test
    public void testGetAllDeliveries() {
        Delivery dummyDelivery = new Delivery();
        dummyDelivery.setId("delivery1");
        dummyDelivery.setCustomerId("customer1");
        // Assume no courier is assigned
        dummyDelivery.setCourierId(null);

        DeliveryResponse dummyResponse = new DeliveryResponse();
        dummyResponse.setId("delivery1");
        // Enriched values
        dummyResponse.setCustomerName("New User1");
        dummyResponse.setCourierUsername(null);

        // Stub the repository to return the delivery
        when(deliveryRepository.findAll()).thenReturn(Flux.just(dummyDelivery));
        // Stub the enrichment: customer lookup returns a User with fullName and username.
        User dummyCustomer = new User();
        dummyCustomer.setFullName("New User1");
        dummyCustomer.setUsername("newuser1");
        when(userRepository.findById("customer1")).thenReturn(Mono.just(dummyCustomer));
        // For courierId, since it's null, our enrichment code uses default empty user.
        // Stub mapper to produce the base response
        when(deliveryMapper.toResponse(dummyDelivery)).thenReturn(new DeliveryResponse() {{
            setId("delivery1");
        }});

        Flux<DeliveryResponse> flux = deliveryService.getAllDeliveries("anyUserId");

        StepVerifier.create(flux)
                .assertNext(response -> {
                    // Verify that enrichment populated the names
                    assert response.getId().equals("delivery1");
                    assert response.getCustomerName().equals("New User1");
                    assert response.getCourierUsername() == null;
                })
                .verifyComplete();
    }

    // Test createDelivery: ensuring mapping, saving, event publishing, and enrichment work
    @Test
    public void testCreateDelivery() {
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setCustomerId("customer1");
        // Set additional request properties if needed

        Delivery dummyDelivery = new Delivery();
        dummyDelivery.setId("delivery1");
        dummyDelivery.setCustomerId("customer1");
        dummyDelivery.setStatus(DeliveryStatus.CREATED);

        DeliveryResponse dummyResponse = new DeliveryResponse();
        dummyResponse.setId("delivery1");
        dummyResponse.setCustomerName("New User1");
        dummyResponse.setCourierUsername(null);

        // Stub mapping and saving
        when(deliveryMapper.toEntity(request)).thenReturn(dummyDelivery);
        when(deliveryRepository.save(dummyDelivery)).thenReturn(Mono.just(dummyDelivery));
        // Stub event publishing (we don't care about its return value here)
        when(kafkaTopicsConfig.getDeliveryCreatedTopic()).thenReturn("delivery.created.topic");
        when(deliveryMapper.toJson(dummyDelivery)).thenReturn("json");
        when(kafkaProducerService.sendMessage("delivery.created.topic", "json")).thenReturn(Mono.empty());
        // Stub mapper to response conversion
        when(deliveryMapper.toResponse(dummyDelivery)).thenReturn(new DeliveryResponse() {{
            setId("delivery1");
        }});
        // Stub user repository for enrichment
        User dummyCustomer = new User();
        dummyCustomer.setFullName("New User1");
        dummyCustomer.setUsername("newuser1");
        when(userRepository.findById("customer1")).thenReturn(Mono.just(dummyCustomer));

        Mono<DeliveryResponse> resultMono = deliveryService.createDelivery(request);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assert response.getId().equals("delivery1");
                    assert response.getCustomerName().equals("New User1");
                    assert response.getCourierUsername() == null;
                })
                .verifyComplete();

        // Wait until the asynchronous publishDeliveryCreated call is observed
        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventHandler, times(1))
                        .publishDeliveryCreated(any(DeliveryEventHandler.DeliveryCreatedEvent.class)));
    }

    // Test getDeliveriesForCustomer: retrieving and enriching deliveries for a customer
    @Test
    public void testGetDeliveriesForCustomer() {
        String customerId = "customer1";
        Delivery dummyDelivery = new Delivery();
        dummyDelivery.setId("delivery1");
        dummyDelivery.setCustomerId(customerId);

        DeliveryResponse dummyResponse = new DeliveryResponse();
        dummyResponse.setId("delivery1");
        dummyResponse.setCustomerName("New User1");

        when(deliveryRepository.findByCustomerId(customerId)).thenReturn(Flux.just(dummyDelivery));
        when(deliveryMapper.toResponse(dummyDelivery)).thenReturn(new DeliveryResponse() {{
            setId("delivery1");
        }});
        User dummyCustomer = new User();
        dummyCustomer.setFullName("New User1");
        dummyCustomer.setUsername("newuser1");
        when(userRepository.findById(customerId)).thenReturn(Mono.just(dummyCustomer));

        Flux<DeliveryResponse> flux = deliveryService.getDeliveriesForCustomer(customerId);

        StepVerifier.create(flux)
                .assertNext(response -> {
                    assert response.getId().equals("delivery1");
                    assert response.getCustomerName().equals("New User1");
                })
                .verifyComplete();
    }

    // Test getDeliveriesForCourier: retrieving and enriching deliveries for a courier
    @Test
    public void testGetDeliveriesForCourier() {
        String courierId = "courier1";
        Delivery dummyDelivery = new Delivery();
        dummyDelivery.setId("delivery1");
        dummyDelivery.setCourierId(courierId);
        // Assume a customer is also set for enrichment
        dummyDelivery.setCustomerId("customer1");

        DeliveryResponse dummyResponse = new DeliveryResponse();
        dummyResponse.setId("delivery1");
        dummyResponse.setCustomerName("New User1");
        dummyResponse.setCourierUsername("newuser1");

        when(deliveryRepository.findByCourierId(courierId)).thenReturn(Flux.just(dummyDelivery));
        when(deliveryMapper.toResponse(dummyDelivery)).thenReturn(new DeliveryResponse() {{
            setId("delivery1");
        }});
        // Stub both user lookups
        User dummyCustomer = new User();
        dummyCustomer.setFullName("New User1");
        dummyCustomer.setUsername("newuser1");
        when(userRepository.findById("customer1")).thenReturn(Mono.just(dummyCustomer));
        User dummyCourier = new User();
        dummyCourier.setFullName("New User Courier");
        dummyCourier.setUsername("newuser1");
        when(userRepository.findById(courierId)).thenReturn(Mono.just(dummyCourier));

        Flux<DeliveryResponse> flux = deliveryService.getDeliveriesForCourier(courierId);

        StepVerifier.create(flux)
                .assertNext(response -> {
                    assert response.getId().equals("delivery1");
                    assert response.getCustomerName().equals("New User1");
                    assert response.getCourierUsername().equals("newuser1");
                })
                .verifyComplete();
    }

    // Test acceptDelivery: successful scenario when delivery is in CREATED status
    @Test
    public void testAcceptDelivery_Success() {
        String deliveryId = "delivery1";
        String courierId = "courier1";

        Delivery dummyDelivery = new Delivery();
        dummyDelivery.setId(deliveryId);
        dummyDelivery.setStatus(DeliveryStatus.CREATED);
        dummyDelivery.setCourierId(null);
        dummyDelivery.setCustomerId("customer1");

        Delivery updatedDelivery = new Delivery();
        updatedDelivery.setId(deliveryId);
        updatedDelivery.setStatus(DeliveryStatus.ASSIGNED);
        updatedDelivery.setCourierId(courierId);
        updatedDelivery.setCustomerId("customer1");

        DeliveryResponse dummyResponse = new DeliveryResponse();
        dummyResponse.setId(deliveryId);
        // Expected enriched values:
        dummyResponse.setCustomerName("New User1");
        dummyResponse.setCourierUsername("newuser1");

        when(deliveryRepository.findById(deliveryId)).thenReturn(Mono.just(dummyDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(Mono.just(updatedDelivery));
        when(kafkaTopicsConfig.getDeliveryStatusUpdatedTopic()).thenReturn("delivery.status.updated.topic");
        when(deliveryMapper.toJson(updatedDelivery)).thenReturn("json");
        when(kafkaProducerService.sendMessage("delivery.status.updated.topic", "json")).thenReturn(Mono.empty());
        when(deliveryMapper.toResponse(updatedDelivery)).thenReturn(new DeliveryResponse() {{
            setId(deliveryId);
        }});
        // Stub user lookups for enrichment
        User dummyCustomer = new User();
        dummyCustomer.setFullName("New User1");
        dummyCustomer.setUsername("newuser1");
        when(userRepository.findById("customer1")).thenReturn(Mono.just(dummyCustomer));
        User dummyCourier = new User();
        dummyCourier.setFullName("Courier Name");
        dummyCourier.setUsername("newuser1");
        when(userRepository.findById(courierId)).thenReturn(Mono.just(dummyCourier));

        Mono<DeliveryResponse> resultMono = deliveryService.acceptDelivery(deliveryId, courierId);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assert response.getId().equals(deliveryId);
                    assert response.getCustomerName().equals("New User1");
                    assert response.getCourierUsername().equals("newuser1");
                })
                .verifyComplete();

        // Await asynchronous event publication for delivery status update
        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventHandler, times(1))
                        .publishDeliveryStatusUpdated(any(DeliveryEventHandler.DeliveryStatusUpdatedEvent.class)));
    }

    // Test acceptDelivery: failure when delivery status is not CREATED
    @Test
    public void testAcceptDelivery_InvalidStatus() {
        String deliveryId = "delivery1";
        String courierId = "courier1";

        Delivery dummyDelivery = new Delivery();
        dummyDelivery.setId(deliveryId);
        dummyDelivery.setStatus(DeliveryStatus.IN_PROGRESS);

        when(deliveryRepository.findById(deliveryId)).thenReturn(Mono.just(dummyDelivery));

        Mono<DeliveryResponse> resultMono = deliveryService.acceptDelivery(deliveryId, courierId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                                Objects.equals(((ResponseStatusException) throwable).getReason(),
                                        "Delivery cannot be accepted. Current status: " + dummyDelivery.getStatus())
                )
                .verify();
    }

    // Test updateDeliveryStatus: successful update when userId is provided and delivery exists
    @Test
    public void testUpdateDeliveryStatus_Success() {
        String deliveryId = "delivery1";
        String userId = "user1";
        DeliveryStatus newStatus = DeliveryStatus.COMPLETED;

        Delivery dummyDelivery = new Delivery();
        dummyDelivery.setId(deliveryId);
        dummyDelivery.setStatus(DeliveryStatus.IN_PROGRESS);
        dummyDelivery.setCustomerId("customer1");

        Delivery updatedDelivery = new Delivery();
        updatedDelivery.setId(deliveryId);
        updatedDelivery.setStatus(newStatus);
        updatedDelivery.setCustomerId("customer1");

        DeliveryResponse dummyResponse = new DeliveryResponse();
        dummyResponse.setId(deliveryId);
        dummyResponse.setCustomerName("New User1");
        // No courier assigned in this test
        dummyResponse.setCourierUsername(null);

        when(deliveryRepository.findById(deliveryId)).thenReturn(Mono.just(dummyDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(Mono.just(updatedDelivery));
        when(kafkaTopicsConfig.getDeliveryStatusUpdatedTopic()).thenReturn("delivery.status.updated.topic");
        when(deliveryMapper.toJson(updatedDelivery)).thenReturn("json");
        when(kafkaProducerService.sendMessage("delivery.status.updated.topic", "json")).thenReturn(Mono.empty());
        when(deliveryMapper.toResponse(updatedDelivery)).thenReturn(new DeliveryResponse() {{
            setId(deliveryId);
        }});
        // Stub enrichment for customer lookup
        User dummyCustomer = new User();
        dummyCustomer.setFullName("New User1");
        dummyCustomer.setUsername("newuser1");
        when(userRepository.findById("customer1")).thenReturn(Mono.just(dummyCustomer));

        Mono<DeliveryResponse> resultMono = deliveryService.updateDeliveryStatus(deliveryId, newStatus, userId);

        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assert response.getId().equals(deliveryId);
                    assert response.getCustomerName().equals("New User1");
                    assert response.getCourierUsername() == null;
                })
                .verifyComplete();

        // Await asynchronous event publication for delivery status update
        await().atMost(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventHandler, times(1))
                        .publishDeliveryStatusUpdated(any(DeliveryEventHandler.DeliveryStatusUpdatedEvent.class)));
    }

    // Test updateDeliveryStatus: error when userId is missing
    @Test
    public void testUpdateDeliveryStatus_MissingUserId() {
        String deliveryId = "delivery1";
        DeliveryStatus newStatus = DeliveryStatus.COMPLETED;

        Mono<DeliveryResponse> resultMono = deliveryService.updateDeliveryStatus(deliveryId, newStatus, null);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.BAD_REQUEST &&
                                Objects.equals(((ResponseStatusException) throwable).getReason(),
                                        "User ID is required to update delivery status.")
                )
                .verify();
    }

    // Test updateDeliveryStatus: error when delivery is not found
    @Test
    public void testUpdateDeliveryStatus_DeliveryNotFound() {
        String deliveryId = "nonexistent";
        String userId = "user1";
        DeliveryStatus newStatus = DeliveryStatus.COMPLETED;

        when(deliveryRepository.findById(deliveryId)).thenReturn(Mono.empty());

        Mono<DeliveryResponse> resultMono = deliveryService.updateDeliveryStatus(deliveryId, newStatus, userId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof DeliveryNotFoundException &&
                                throwable.getMessage().contains("Delivery not found with ID: " + deliveryId)
                )
                .verify();
    }

}

