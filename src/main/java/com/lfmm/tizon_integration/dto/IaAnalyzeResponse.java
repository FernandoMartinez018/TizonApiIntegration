package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.FrameStatus;

import java.util.List;

/** Respuesta interna de la IA (contrato: IaAnalyzeResponse). */
public record IaAnalyzeResponse(
        FrameStatus status,
        ClassId label,
        Double confidence,
        Probabilities probabilities,
        List<Region> regions,
        int frameWidth,
        int frameHeight,
        String modelVersion,
        int inferenceMs
) {}
