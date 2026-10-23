package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.FrameStatus;

import java.util.List;
import java.util.UUID;

public record FrameAnalysis(
        UUID frameId,
        UUID sessionId,
        FrameStatus status,
        ClassId label,
        Double confidence,
        Probabilities probabilities,
        List<Region> regions,
        int frameWidth,
        int frameHeight,
        String modelVersion,
        int latencyMs,
        SessionStats sessionStats
) {}
