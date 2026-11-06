package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;

/** Cuerpo único de error del contrato. {@code message} va en español, apto para el usuario. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        ErrorCode code,
        String message,
        OffsetDateTime timestamp,
        @JsonProperty("request_id") String requestId,
        Map<String, String> fields
) {
    public static ApiError of(HttpStatus status, ErrorCode code, String message) {
        return of(status, code, message, null);
    }

    public static ApiError of(HttpStatus status, ErrorCode code, String message, Map<String, String> fields) {
        return new ApiError(status.value(), code, message, OffsetDateTime.now(), MDC.get("request_id"), fields);
    }
}
