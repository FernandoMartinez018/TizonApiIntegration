package com.lfmm.tizon_integration.dto;

import com.lfmm.tizon_integration.model.Alert;
import com.lfmm.tizon_integration.model.Severity;

import java.time.Instant;
import java.util.UUID;

/** Contrato: Alert. */
public record AlertResponse(
        UUID alertId,
        UUID sessionId,
        String username,
        Severity severity,
        double affectionPct,
        LocationDto location,
        String fieldName,
        Alert.Status status,
        Instant createdAt,
        String acknowledgedBy
) {
    public static AlertResponse of(Alert a) {
        LocationDto location = a.getLocationLat() == null ? null
                : new LocationDto(a.getLocationLat(), a.getLocationLon(), a.getLocationAccuracyM());
        return new AlertResponse(a.getId(), a.getSessionId(), a.getUsername(), a.getSeverity(), a.getAffectionPct(),
                location, a.getFieldName(), a.getStatus(), a.getCreatedAt(), a.getAcknowledgedBy());
    }
}
