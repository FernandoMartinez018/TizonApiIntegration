package com.lfmm.tizon_integration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FrameRequest(
        @NotBlank(message = "El frame en Base64 no puede estar vacío.")
        @Size(min = 100, message = "El Base64 parece demasiado corto para ser una imagen válida.")
        String frameB64,

        @Valid LocationDto location
) {}
