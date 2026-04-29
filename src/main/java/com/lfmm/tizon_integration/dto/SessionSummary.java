package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionSummary(
        @JsonProperty("session_id")     String sessionId,
        @JsonProperty("total_frames")   int totalFrames,
        @JsonProperty("affected_frames") int affectedFrames,
        @JsonProperty("avg_confidence") float avgConfidence,
        @JsonProperty("dominant_label") String dominantLabel,
        String status,
        @JsonProperty("affection_pct")  float affectionPct
) {}
