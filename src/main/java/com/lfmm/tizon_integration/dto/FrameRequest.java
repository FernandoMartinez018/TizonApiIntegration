package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FrameRequest(

        @NotBlank(message = "El frame en Base64 no puede estar vacío.")
        @Size(min = 100, message = "El Base64 parece demasiado corto para ser una imagen válida.")
        @JsonProperty("frame_b64")
        String frameB64,

        @JsonProperty("session_id")
        String sessionId

) {}
