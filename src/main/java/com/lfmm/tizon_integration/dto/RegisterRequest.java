package com.lfmm.tizon_integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Ingresa un usuario.")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{4,40}$",
                message = "El usuario debe tener de 4 a 40 caracteres: letras, números, punto, guion o guion bajo.")
        String username,

        @NotBlank(message = "Ingresa una contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        String password,

        @NotBlank(message = "Ingresa tu nombre completo.")
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres.")
        String fullName
) {}
