package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.Severity;

public record SessionStats(
        int totalFrames,
        int analyzedFrames,
        int affectedFrames,
        int noVegetationFrames,
        double affectionPct,
        Severity severity,
        Double avgConfidence,
        ClassId dominantLabel
) {}
