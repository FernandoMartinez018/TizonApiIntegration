package com.lfmm.tizon_integration.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserResponse user) {}
