package com.lfmm.tizon_integration.config;

import com.lfmm.tizon_integration.dto.IaHealth;
import com.lfmm.tizon_integration.service.IaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Componente "ia" de /actuator/health (RNF-OBS-03): UP solo si la IA responde y su modelo cargó.
 * Con la IA caída el health compuesto queda DOWN (503).
 */
@Component("ia")
@RequiredArgsConstructor
public class IaHealthIndicator implements HealthIndicator {

    private final IaClient iaClient;

    @Override
    public Health health() {
        try {
            IaHealth h = iaClient.health();
            if (h != null && h.modelLoaded()) {
                return Health.up().withDetail("model_version", h.modelVersion()).build();
            }
            return Health.down().withDetail("reason", "modelo no cargado").build();
        } catch (RuntimeException ex) {
            return Health.down().withDetail("reason", "IA no responde").build();
        }
    }
}
