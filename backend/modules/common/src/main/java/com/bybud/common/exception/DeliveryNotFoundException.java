package com.bybud.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DeliveryNotFoundException extends ResponseStatusException {

    public DeliveryNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
