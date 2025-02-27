package com.bybud.entity.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A generic response wrapper.
 * In the reactive controllers, wrapped in a Mono (e.g. Mono<BaseResponse<T>>).
 */
public class BaseResponse<T> {

    private final String status;
    private final String message;
    private final T data;

    @JsonCreator
    private BaseResponse(@JsonProperty("status") String status,
                         @JsonProperty("message") String message,
                         @JsonProperty("data") T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    private BaseResponse(String status, String message) {
        this(status, message, null);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>("SUCCESS", message, data);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>("ERROR", message);
    }

    public String getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    public T getData() {
        return data;
    }
}
