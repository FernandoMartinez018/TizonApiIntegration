package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record AnalysisResult(
        String label,
        float confidence,
        Map<String, Float> probabilities,
        @JsonProperty("bounding_boxes") List<BoundingBox> boundingBoxes,
        @JsonProperty("is_vegetation")  boolean isVegetation,
        @JsonProperty("frame_width")    int frameWidth,
        @JsonProperty("frame_height")   int frameHeight,
        @JsonProperty("session_id")     String sessionId
) {
    /**
     * Resultado vacío para cuando Python no responde o el frame no es válido.
     */
    public static AnalysisResult empty(String sessionId) {
        return new AnalysisResult(
                "Sin deteccion",
                0f,
                Map.of("Sano", 0f, "Tizon Temprano", 0f, "Tizon Tardio", 0f),
                List.of(),
                false,
                0,
                0,
                sessionId
        );
    }

    /** True si el frame tiene tizón (temprano o tardío). */
    public boolean isAffected() {
        return label != null && label.contains("Tizon")
                && !label.equals("Sin deteccion");
    }

    /** Porcentaje de afectación basado en las probabilidades de tizón. */
    public float affectionPct() {
        if (probabilities == null) return 0f;
        float tardio   = probabilities.getOrDefault("Tizon Tardio", 0f);
        float temprano = probabilities.getOrDefault("Tizon Temprano", 0f);
        return (tardio + temprano) * 100f;
    }
}
