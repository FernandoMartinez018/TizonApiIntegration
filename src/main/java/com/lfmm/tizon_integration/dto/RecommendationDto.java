package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.ClassId;

public record RecommendationDto(ClassId disease, String text, String source) {}
