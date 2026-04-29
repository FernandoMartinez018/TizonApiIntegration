package com.lfmm.tizon_integration.dto;

public record LoginResponse(
        String token,
        String username,
        long expiresIn
) {}