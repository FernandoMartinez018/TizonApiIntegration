package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.ClassId;

import java.util.List;

/** Configuración funcional para la app: la app no define umbrales ni clases en su código. */
public record AppConfigResponse(
        List<ClassConfig> classes,
        SeverityThresholds severity,
        double alertThresholdPct,
        int frameIntervalMs,
        String modelVersion
) {
    public record ClassConfig(ClassId id, boolean affected) {}

    public record SeverityThresholds(double warningPct, double criticalPct) {}
}
