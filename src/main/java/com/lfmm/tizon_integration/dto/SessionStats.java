package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.Severity;

import java.util.Map;

public record SessionStats(
        int totalFrames,
        int analyzedFrames,
        int affectedFrames,
        int noVegetationFrames,
        int notPotatoFrames,
        double affectionPct,
        Severity severity,
        Double avgConfidence,
        ClassId dominantLabel,
        Map<ClassId, Integer> labelCounts
) {}
