package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.config.TizonProperties;
import com.lfmm.tizon_integration.dto.AlertResponse;
import com.lfmm.tizon_integration.dto.ErrorCode;
import com.lfmm.tizon_integration.exception.ApiException;
import com.lfmm.tizon_integration.model.Alert;
import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.repository.AlertRepository;
import com.lfmm.tizon_integration.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alertas (RF-11). Regla (contrato, doc 03): al finalizar una sesión con affection_pct ≥ umbral y
 * analyzed_frames ≥ mínimo se crea una alerta OPEN; como máximo una por sesión.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alerts;
    private final TizonProperties props;

    /** Pura: ¿la sesión cumple la regla de alerta? */
    public static boolean meetsRule(int analyzedFrames, double affectionPct, double thresholdPct, int minFrames) {
        return analyzedFrames >= minFrames && affectionPct >= thresholdPct;
    }

    @Transactional
    public Optional<Alert> createIfNeeded(ScanSession s) {
        double pct = SessionStatsCalculator.affectionPct(s.getAffectedFrames(), s.getAnalyzedFrames());
        if (!meetsRule(s.getAnalyzedFrames(), pct, props.alertThresholdPct(), props.alertMinFrames())) {
            return Optional.empty();
        }
        Optional<Alert> existing = alerts.findBySessionId(s.getId());
        if (existing.isPresent()) return existing;

        Alert alert = alerts.save(Alert.builder()
                .sessionId(s.getId())
                .username(s.getUsername())
                .severity(s.getSeverity())
                .affectionPct(pct)
                .locationLat(s.getLocationLat())
                .locationLon(s.getLocationLon())
                .locationAccuracyM(s.getLocationAccuracyM())
                .fieldName(s.getFieldName())
                .build());
        log.atInfo().addKeyValue("session_id", s.getId()).addKeyValue("alert_id", alert.getId())
                .addKeyValue("affection_pct", pct).log("Alerta creada");
        return Optional.of(alert);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> alertIdFor(UUID sessionId) {
        return alerts.findBySessionId(sessionId).map(Alert::getId);
    }

    /** AGRICULTOR ve las suyas; TECNICO y ADMIN, todas. */
    @Transactional(readOnly = true)
    public List<AlertResponse> list(UserDetails user, Alert.Status status, Instant since) {
        String owner = Roles.isStaff(user) ? null : user.getUsername();
        return alerts.search(owner, status, since == null ? Instant.EPOCH : since).stream()
                .map(AlertResponse::of).toList();
    }

    /** Solo TECNICO o ADMIN (regla en SecurityConfig). Idempotente. */
    @Transactional
    public AlertResponse acknowledge(String username, UUID alertId) {
        Alert alert = alerts.findById(alertId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "La alerta no existe."));
        if (alert.getStatus() == Alert.Status.OPEN) {
            alert.setStatus(Alert.Status.ACKNOWLEDGED);
            alert.setAcknowledgedBy(username);
            alert.setAcknowledgedAt(Instant.now());
            log.info("Alerta atendida alert_id={} por={}", alertId, username);
        }
        return AlertResponse.of(alert);
    }
}
