package com.lfmm.tizon_integration.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        String role,
        boolean enabled,
        Instant createdAt
) {}
