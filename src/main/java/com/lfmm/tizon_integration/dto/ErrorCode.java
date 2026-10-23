package com.lfmm.tizon_integration.dto;

/** Códigos de error del contrato (components.schemas.Error.code). */
public enum ErrorCode {
    VALIDATION_ERROR,
    INVALID_IMAGE,
    UNAUTHORIZED,
    BAD_CREDENTIALS,
    TOKEN_EXPIRED,
    FORBIDDEN,
    NOT_FOUND,
    SESSION_NOT_ACTIVE,
    USERNAME_TAKEN,
    PAYLOAD_TOO_LARGE,
    IA_UNAVAILABLE,
    IA_TIMEOUT,
    MODEL_NOT_READY,
    INTERNAL_ERROR
}
