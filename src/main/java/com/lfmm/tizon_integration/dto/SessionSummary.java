package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.lfmm.tizon_integration.model.ScanSession.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contrato: SessionStats + datos de la sesión (allOf). */
public record SessionSummary(
        UUID sessionId,
        String username,
        SessionStatus status,
        Instant startedAt,
        Instant endedAt,
        LocationDto location,
        String fieldName,
        UUID alertId,
        List<RecommendationDto> recommendations,
        @JsonUnwrapped SessionStats stats
) {}
