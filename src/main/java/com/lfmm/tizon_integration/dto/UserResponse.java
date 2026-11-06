package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.model.AppUser.Role;

import java.time.Instant;
import java.util.UUID;

/** Contrato: User. Nunca incluye la contraseña ni su hash. */
public record UserResponse(
        UUID id,
        String username,
        String fullName,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse of(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}
