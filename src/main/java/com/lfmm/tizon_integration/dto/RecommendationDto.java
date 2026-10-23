package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.Severity;

public record RecommendationDto(Severity severity, String text, String source) {}
