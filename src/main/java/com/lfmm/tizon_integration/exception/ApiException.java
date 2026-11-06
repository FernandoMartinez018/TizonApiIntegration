package com.lfmm.tizon_integration.exception;

import com.lfmm.tizon_integration.dto.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Error de negocio o técnico que se responde con el cuerpo Error del contrato. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    public ApiException(HttpStatus status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "No encontramos la sesión solicitada.");
    }
}
