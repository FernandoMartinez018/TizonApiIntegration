package com.lfmm.tizon_integration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record StartSessionRequest(
        @Valid LocationDto location,
        @Size(max = 120, message = "El nombre de la finca o lote admite máximo 120 caracteres.") String fieldName
) {}
