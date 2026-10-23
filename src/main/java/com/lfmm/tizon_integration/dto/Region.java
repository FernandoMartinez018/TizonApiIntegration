package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.ClassId;

/** Región de hoja analizada; coordenadas relativas (0–1) al frame enviado, ya rotado. */
public record Region(
        double x,
        double y,
        double width,
        double height,
        ClassId label,
        double confidence,
        Probabilities probabilities
) {}
