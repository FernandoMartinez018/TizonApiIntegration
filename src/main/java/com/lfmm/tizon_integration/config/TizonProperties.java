package com.lfmm.tizon_integration.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Reglas funcionales (umbrales de severidad, alertas y ritmo de envío). Única fuente:
 * la app las obtiene de GET /api/config (RNF-MAN-03).
 */
@Validated
@ConfigurationProperties(prefix = "tizon")
public record TizonProperties(
        @NotNull @Valid Thresholds severity,
        @PositiveOrZero double alertThresholdPct,
        @Positive int alertMinFrames,
        @Positive int frameIntervalMs,
        @Positive int sessionInactivityMinutes
) {
    /** NONE = 0 · LOW = (0, warning] · WARNING = (warning, critical] · CRITICAL = > critical */
    public record Thresholds(@PositiveOrZero double warningPct, @PositiveOrZero double criticalPct) {}
}
