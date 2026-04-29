package com.lfmm.tizon_integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El usuario no puede estar vacío.")
        String username,

        @NotBlank(message = "La contraseña no puede estar vacía.")
        @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres.")
        String password

) {}
