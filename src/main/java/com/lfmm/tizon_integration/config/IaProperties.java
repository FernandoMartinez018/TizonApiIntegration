package com.lfmm.tizon_integration.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Conexión con el servicio de IA (red interna). Se valida al arrancar.
 */
@Validated
@ConfigurationProperties(prefix = "ia")
public record IaProperties(
        @NotBlank(message = "IA_BASE_URL es obligatorio") String baseUrl,
        @Positive int timeoutSeconds,
        @NotBlank(message = "INTERNAL_API_TOKEN es obligatorio") String internalToken
) {}
