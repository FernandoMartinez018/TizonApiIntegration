package com.lfmm.tizon_integration.dto;

import java.util.Map;

public record BoundingBox(
        float x,
        float y,
        float width,
        float height,
        String label,
        float confidence,
        Map<String, Float> probabilities
) {}
