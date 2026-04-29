package com.lfmm.tizon_integration.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST,
                "Validación fallida. Revisa los campos enviados.",
                null
        );
        body.put("fields", fieldErrors);

        log.warn("Error de validación: {}", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {

        log.warn("Intento de login fallido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorBody(HttpStatus.UNAUTHORIZED,
                        "Usuario o contraseña incorrectos.", null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex) {

        log.warn("Error de autenticación: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorBody(HttpStatus.UNAUTHORIZED,
                        "No autenticado. Proporciona un token JWT válido.", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Error de argumento: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(buildErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex) {

        log.warn("Error de estado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorBody(HttpStatus.CONFLICT, ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Error no manejado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error interno del servidor.", null));
    }

    private Map<String, Object> buildErrorBody(
            HttpStatus status, String message, String detail) {

        Map<String, Object> body = new HashMap<>();
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("timestamp", LocalDateTime.now().toString());
        if (detail != null) body.put("detail", detail);
        return body;
    }
}
