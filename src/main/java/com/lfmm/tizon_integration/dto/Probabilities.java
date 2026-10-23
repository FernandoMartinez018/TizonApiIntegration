package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Claves en mayúsculas, iguales a ClassId (contrato). */
public record Probabilities(
        @JsonProperty("SANO") double sano,
        @JsonProperty("TIZON_TARDIO") double tizonTardio
) {}
