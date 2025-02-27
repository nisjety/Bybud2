package com.bybud.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ApiGatewayException extends ResponseStatusException {
    public ApiGatewayException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public ApiGatewayException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
