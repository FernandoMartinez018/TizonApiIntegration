package com.lfmm.tizon_integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfmm.tizon_integration.config.TizonProperties;
import com.lfmm.tizon_integration.dto.*;
import com.lfmm.tizon_integration.exception.ApiException;
import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.ScanResult;
import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.model.ScanSession.SessionStatus;
import com.lfmm.tizon_integration.repository.RecommendationRepository;
import com.lfmm.tizon_integration.repository.ScanResultRepository;
import com.lfmm.tizon_integration.repository.ScanSessionRepository;
import com.lfmm.tizon_integration.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida de las sesiones de escaneo (RF-04). El backend nunca crea sesiones de forma
 * implícita; solo el dueño envía frames o finaliza; un recurso ajeno responde 404.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    /** 2 MB decodificados ≈ 2.796.203 caracteres en Base64 (RNF-SEG-10). */
    static final int MAX_FRAME_B64_CHARS = 2_796_203;

    private final ScanSessionRepository sessionRepo;
    private final ScanResultRepository resultRepo;
    private final RecommendationRepository recommendationRepo;
    private final AlertService alertService;
    private final IaClient iaClient;
    private final TizonProperties props;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    @Transactional
    public SessionSummary start(String username, StartSessionRequest req) {
        ScanSession session = ScanSession.builder().username(username).build();
        if (req != null) {
            applyLocation(session, req.location());
            session.setFieldName(StringUtils.hasText(req.fieldName()) ? req.fieldName().trim() : null);
        }
        session = sessionRepo.save(session);
        log.info("Sesión iniciada session_id={} username={}", session.getId(), username);
        return toSummary(session);
    }

    @Transactional(readOnly = true)
    public SessionSummary get(UserDetails user, UUID sessionId) {
        ScanSession session = sessionRepo.findById(sessionId)
                .filter(s -> Roles.isStaff(user) || s.getUsername().equals(user.getUsername()))
                .orElseThrow(ApiException::notFound);
        return toDetail(session);
    }

    @Transactional(readOnly = true)
    public SessionPage list(UserDetails user, int page, int size, String username, SessionStatus status) {
        String filter;
        if (Roles.isStaff(user)) {
            filter = StringUtils.hasText(username) ? username : null;
        } else if (username == null || username.equals(user.getUsername())) {
            filter = user.getUsername();
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                    "Solo un técnico o administrador puede ver sesiones de otros usuarios.");
        }
        Page<ScanSession> result = sessionRepo.search(filter, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt")));
        return new SessionPage(result.map(this::toSummary).getContent(), page, size, result.getTotalElements());
    }

    /**
     * Analiza un frame. La llamada a la IA ocurre fuera de la transacción; si falla, se propaga
     * el error y no se persiste nada (RF-07 CA1).
     */
    public FrameAnalysis analyzeFrame(String username, UUID sessionId, FrameRequest req, String requestId) {
        long start = System.nanoTime();
        if (req.frameB64().length() > MAX_FRAME_B64_CHARS) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE,
                    "La imagen supera el tamaño máximo de 2 MB.");
        }
        tx.executeWithoutResult(status -> requireActive(findOwned(username, sessionId)));

        IaAnalyzeResponse ia = iaClient.analyze(req.frameB64(), requestId);
        int latencyMs = (int) ((System.nanoTime() - start) / 1_000_000);

        return tx.execute(status -> persistFrame(username, sessionId, req, ia, latencyMs, requestId));
    }

    @Transactional
    public SessionSummary end(String username, UUID sessionId) {
        ScanSession session = requireActive(findOwned(username, sessionId));
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(Instant.now());
        alertService.createIfNeeded(session);   // RF-11 CA1: una alerta por sesión si supera el umbral
        log.info("Sesión finalizada session_id={} total_frames={} affected_frames={}",
                session.getId(), session.getTotalFrames(), session.getAffectedFrames());
        return toDetail(session);
    }

    /** RF-04 CA6 "Descartar": la sesión queda CANCELLED (no genera alerta). */
    @Transactional
    public SessionSummary cancel(String username, UUID sessionId) {
        ScanSession session = requireActive(findOwned(username, sessionId));
        session.setStatus(SessionStatus.CANCELLED);
        session.setEndedAt(Instant.now());
        log.info("Sesión descartada session_id={}", session.getId());
        return toSummary(session);
    }

    /** Sesiones abandonadas: ACTIVE sin frames durante tizon.session-inactivity-minutes → CANCELLED. */
    @Transactional
    public int cancelInactive(Instant now) {
        int cancelled = sessionRepo.cancelInactive(now.minus(Duration.ofMinutes(props.sessionInactivityMinutes())), now);
        if (cancelled > 0) log.info("Sesiones abandonadas canceladas: {}", cancelled);
        return cancelled;
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private FrameAnalysis persistFrame(String username, UUID sessionId, FrameRequest req,
                                       IaAnalyzeResponse ia, int latencyMs, String requestId) {
        // Se vuelve a validar: la sesión pudo finalizar mientras la IA respondía.
        ScanSession session = requireActive(findOwned(username, sessionId));
        if (session.getLocationLat() == null) applyLocation(session, req.location());

        List<Region> regions = ia.regions() == null ? List.of() : ia.regions();
        ScanResult result = resultRepo.save(ScanResult.builder()
                .session(session)
                .status(ia.status())
                .label(ia.label())
                .confidence(ia.confidence())
                .probSano(ia.probabilities() == null ? null : ia.probabilities().sano())
                .probTizonTemprano(ia.probabilities() == null ? null : ia.probabilities().tizonTemprano())
                .probTizonTardio(ia.probabilities() == null ? null : ia.probabilities().tizonTardio())
                .regionsJson(toJson(regions))
                .frameWidth(ia.frameWidth())
                .frameHeight(ia.frameHeight())
                .modelVersion(ia.modelVersion())
                .latencyMs(latencyMs)
                .inferenceMs(ia.inferenceMs())
                .requestId(requestId)
                .build());

        SessionStatsCalculator.apply(session, ia.status(), ia.label(), ia.confidence(), props.severity());
        session.setLastFrameAt(Instant.now());

        // Campos estructurados en el log JSON (RNF-OBS-02); request_id ya va en el MDC.
        log.atInfo().addKeyValue("session_id", sessionId).addKeyValue("status", ia.status())
                .addKeyValue("latency_ms", latencyMs).addKeyValue("inference_ms", ia.inferenceMs())
                .log("Frame analizado");

        return new FrameAnalysis(result.getId(), sessionId, ia.status(), ia.label(), ia.confidence(),
                ia.probabilities(), regions, ia.frameWidth(), ia.frameHeight(), ia.modelVersion(),
                latencyMs, SessionStatsCalculator.stats(session));
    }

    private ScanSession findOwned(String username, UUID sessionId) {
        return sessionRepo.findById(sessionId)
                .filter(s -> s.getUsername().equals(username))
                .orElseThrow(ApiException::notFound);
    }

    private static ScanSession requireActive(ScanSession session) {
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.SESSION_NOT_ACTIVE,
                    "Este escaneo ya terminó. Inicia uno nuevo.");
        }
        return session;
    }

    private static void applyLocation(ScanSession session, LocationDto location) {
        if (location == null) return;
        session.setLocationLat(location.latitude());
        session.setLocationLon(location.longitude());
        session.setLocationAccuracyM(location.accuracyM());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudieron serializar las regiones", ex);
        }
    }

    /** Resumen para listas: sin recomendaciones. */
    private SessionSummary toSummary(ScanSession s) {
        return summary(s, List.of(), null);
    }

    /** Detalle y resumen final: con las recomendaciones de la enfermedad que predominó (RF-08 CA3, P-05). */
    private SessionSummary toDetail(ScanSession s) {
        ClassId disease = SessionStatsCalculator.predominantDisease(s);
        List<RecommendationDto> recommendations = disease == null ? List.of()
                : recommendationRepo.findByDiseaseOrderBySortOrder(disease).stream()
                        .map(r -> new RecommendationDto(r.getDisease(), r.getText(), r.getSource())).toList();
        return summary(s, recommendations, alertService.alertIdFor(s.getId()).orElse(null));
    }

    private SessionSummary summary(ScanSession s, List<RecommendationDto> recommendations, UUID alertId) {
        LocationDto location = s.getLocationLat() == null ? null
                : new LocationDto(s.getLocationLat(), s.getLocationLon(), s.getLocationAccuracyM());
        return new SessionSummary(s.getId(), s.getUsername(), s.getStatus(), s.getStartedAt(),
                s.getEndedAt(), location, s.getFieldName(), alertId, recommendations,
                SessionStatsCalculator.stats(s));
    }
}
