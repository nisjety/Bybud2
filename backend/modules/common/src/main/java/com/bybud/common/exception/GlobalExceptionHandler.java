package com.bybud.common.exception;

import com.bybud.entity.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${app.debug:false}")
    private boolean debug;

    /**
     * Handle UserNotFoundException in a reactive way.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return logAndBuildResponse(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage());
    }

    /**
     * Handle IllegalArgumentException in a reactive way.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return logAndBuildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /**
     * Handle ResponseStatusException (WebFlux equivalent of ResponseStatusException).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Unexpected error occurred";
        return logAndBuildResponse((HttpStatus) ex.getStatusCode(), ex.getStatusCode().toString(), message);
    }

    /**
     * Handle general exceptions.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ErrorResponse> handleGeneralException(Exception ex) {
        String message = debug ? ex.getMessage() : "An unexpected error occurred.";
        return logAndBuildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", message);
    }

    /**
     * Handle API Gateway exceptions.
     */
    @ExceptionHandler(ApiGatewayException.class)
    public Mono<ErrorResponse> handleApiGatewayException(ApiGatewayException ex) {
        return logAndBuildResponse(HttpStatus.BAD_GATEWAY, "API Gateway Error", ex.getMessage());
    }

    /**
     * Logs the error and returns a reactive Mono<ErrorResponse>.
     */
    private Mono<ErrorResponse> logAndBuildResponse(HttpStatus status, String error, String message) {
        logger.error("{}: {}", error, message);

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message
        );

        return Mono.just(response);
    }
}
