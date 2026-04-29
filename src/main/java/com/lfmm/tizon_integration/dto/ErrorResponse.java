package com.lfmm.tizon_integration.dto;

public record ErrorResponse(
        String message,
        int status
) {}
