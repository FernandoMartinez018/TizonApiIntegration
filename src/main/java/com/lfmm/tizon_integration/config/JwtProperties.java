package com.lfmm.tizon_integration.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;

/**
 * Configuración del JWT. Se valida al arrancar: sin {@code JWT_SECRET} la API no inicia.
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET es obligatorio") String secret,
        @Positive long expirationMs
) {

    // Validación por método para que el error de arranque no imprima el secreto rechazado.
    @AssertTrue(message = "JWT_SECRET debe tener al menos 32 bytes")
    public boolean isSecretLongEnough() {
        return secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length >= 32;
    }
}
