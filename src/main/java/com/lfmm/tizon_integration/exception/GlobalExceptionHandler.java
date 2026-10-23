package com.lfmm.tizon_integration.exception;

import com.lfmm.tizon_integration.dto.ApiError;
import com.lfmm.tizon_integration.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Todas las respuestas de error usan el cuerpo Error del contrato (code + message en español). */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        if (ex.getStatus().is5xxServerError()) log.warn("{}: {}", ex.getCode(), ex.getMessage());
        return body(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(toSnakeCase(error.getField()), error.getDefaultMessage());
        }
        return body(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Revisa los datos enviados.", fields);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Revisa los datos enviados.", null);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiError> handleBadCredentials(AuthenticationException ex) {
        // RF-02 CA2: no se indica si falló el usuario, la contraseña o la habilitación.
        return body(HttpStatus.UNAUTHORIZED, ErrorCode.BAD_CREDENTIALS, "Usuario o contraseña incorrectos.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return body(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Debes iniciar sesión para continuar.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "No tienes permiso para esta acción.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        // Excepciones estándar de Spring MVC (404 de ruta, 405, 415…) traen su propio código.
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            if (status.is4xxClientError()) {
                return status == HttpStatus.NOT_FOUND
                        ? body(status, ErrorCode.NOT_FOUND, "El recurso no existe.", null)
                        : body(status, ErrorCode.VALIDATION_ERROR, "Petición no válida.", null);
            }
        }
        log.error("Error no manejado: {}", ex.getMessage(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Ocurrió un error inesperado. Intenta más tarde.", null);
    }

    private static ResponseEntity<ApiError> body(HttpStatus status, ErrorCode code, String message,
                                                 Map<String, String> fields) {
        return ResponseEntity.status(status).body(ApiError.of(status, code, message, fields));
    }

    /** Los nombres de campo se devuelven igual que en el JSON (snake_case). */
    private static String toSnakeCase(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
