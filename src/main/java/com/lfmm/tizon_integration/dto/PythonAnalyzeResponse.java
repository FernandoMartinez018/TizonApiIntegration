package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record PythonAnalyzeResponse(
        String label,
        float confidence,
        Map<String, Float> probabilities,
        @JsonProperty("bounding_boxes") List<BoundingBox> bounding_boxes,
        @JsonProperty("is_vegetation")  boolean is_vegetation,
        @JsonProperty("frame_width")    int frame_width,
        @JsonProperty("frame_height")   int frame_height
) {
    public static PythonAnalyzeResponse empty() {
        return new PythonAnalyzeResponse(
                "Sin deteccion", 0f,
                Map.of("Sano", 0f, "Tizon Temprano", 0f, "Tizon Tardio", 0f),
                List.of(), false, 0, 0
        );
    }
}
