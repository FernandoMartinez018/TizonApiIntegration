package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Claves en mayúsculas, iguales a ClassId (contrato). tizonTemprano es null con el modelo anterior de 2 clases.
 * Con el modelo de 4 salidas la suma puede ser menor que 1 (el resto es la salida interna NO_PAPA).
 */
public record Probabilities(
        @JsonProperty("SANO") double sano,
        @JsonProperty("TIZON_TEMPRANO") Double tizonTemprano,
        @JsonProperty("TIZON_TARDIO") double tizonTardio
) {}
