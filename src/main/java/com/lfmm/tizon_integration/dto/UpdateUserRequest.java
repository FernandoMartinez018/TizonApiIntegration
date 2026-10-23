package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.AppUser.Role;

/** Cambios de rol o habilitación (ADMIN). Los campos nulos no se modifican. */
public record UpdateUserRequest(Role role, Boolean enabled) {}
