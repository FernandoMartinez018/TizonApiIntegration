package com.lfmm.tizon_integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfmm.tizon_integration.dto.*;
import com.lfmm.tizon_integration.model.ScanResult;
import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.repository.ScanResultRepository;
import com.lfmm.tizon_integration.repository.ScanSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final PythonProxyService pythonProxy;
    private final ScanSessionRepository sessionRepo;
    private final ScanResultRepository resultRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalysisResult analyzeFrame(FrameRequest request, String username) {

        ScanSession session = resolveSession(request.sessionId(), username);

        PythonAnalyzeResponse pythonResponse =
                pythonProxy.analyze(request.frameB64(), session.getId().toString());

        ScanResult scanResult = buildAndSaveResult(pythonResponse, session);

        updateSessionStats(session, scanResult);

        return toAnalysisResult(pythonResponse, session.getId().toString());
    }

    @Transactional
    public SessionStartResponse startSession(String username) {
        ScanSession session = ScanSession.builder()
                .username(username)
                .status(ScanSession.SessionStatus.ACTIVE)
                .build();
        session = sessionRepo.save(session);

        log.info("Sesión iniciada: {} para usuario: {}", session.getId(), username);
        return new SessionStartResponse(session.getId().toString(), session.getStatus().name());
    }

    @Transactional
    public SessionSummary endSession(String sessionId, String username) {
        ScanSession session = findSessionOrThrow(sessionId, username);

        if (session.getStatus() != ScanSession.SessionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "La sesión ya está " + session.getStatus().name().toLowerCase() + ".");
        }

        session.setStatus(ScanSession.SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        session = sessionRepo.save(session);

        log.info("Sesión finalizada: {} | Frames: {} | Afectados: {}",
                session.getId(), session.getTotalFrames(), session.getAffectedFrames());

        return toSessionSummary(session);
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> getHistory(String username) {
        return sessionRepo.findByUsernameOrderByStartedAtDesc(username)
                .stream()
                .map(this::toSessionSummary)
                .toList();
    }

    private ScanSession resolveSession(String sessionId, String username) {
        if (sessionId != null && !sessionId.isBlank()) {
            try {
                UUID id = UUID.fromString(sessionId);
                var existing = sessionRepo.findByIdAndUsername(id, username);
                if (existing.isPresent() &&
                        existing.get().getStatus() == ScanSession.SessionStatus.ACTIVE) {
                    return existing.get();
                }
            } catch (IllegalArgumentException ex) {
                log.warn("sessionId inválido '{}' — creando sesión nueva.", sessionId);
            }
        }

        return sessionRepo.save(ScanSession.builder()
                .username(username)
                .status(ScanSession.SessionStatus.ACTIVE)
                .build());
    }

    private ScanResult buildAndSaveResult(PythonAnalyzeResponse res, ScanSession session) {
        var probs = res.probabilities();
        String bboxJson = serializeBboxes(res.bounding_boxes());

        ScanResult result = ScanResult.builder()
                .session(session)
                .label(res.label())
                .confidence(res.confidence())
                .probSano(           probs != null ? probs.getOrDefault("Sano", 0f) : 0f)
                .probTizonTemprano(  probs != null ? probs.getOrDefault("Tizon Temprano", 0f) : 0f)
                .probTizonTardio(    probs != null ? probs.getOrDefault("Tizon Tardio", 0f) : 0f)
                .boundingBoxesJson(bboxJson)
                .isVegetation(res.is_vegetation())
                .frameWidth(res.frame_width())
                .frameHeight(res.frame_height())
                .build();

        return resultRepo.save(result);
    }

    private void updateSessionStats(ScanSession session, ScanResult result) {
        int prevTotal = session.getTotalFrames();
        int newTotal  = prevTotal + 1;

        // Recalcular promedio de confianza de forma incremental
        float newAvg = ((session.getAvgConfidence() * prevTotal) + result.getConfidence())
                / newTotal;

        session.setTotalFrames(newTotal);
        session.setAvgConfidence(newAvg);

        if (result.isAffected()) {
            session.setAffectedFrames(session.getAffectedFrames() + 1);
            session.setDominantLabel(result.getLabel());
        }

        sessionRepo.save(session);
    }

    private ScanSession findSessionOrThrow(String sessionId, String username) {
        UUID id;
        try {
            id = UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El sessionId proporcionado no es válido.");
        }

        return sessionRepo.findByIdAndUsername(id, username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sesión no encontrada o no pertenece al usuario."));
    }

    private String serializeBboxes(Object bboxes) {
        if (bboxes == null) return "[]";
        try {
            return objectMapper.writeValueAsString(bboxes);
        } catch (JsonProcessingException ex) {
            log.warn("No se pudieron serializar las bounding boxes: {}", ex.getMessage());
            return "[]";
        }
    }


    private AnalysisResult toAnalysisResult(PythonAnalyzeResponse res, String sessionId) {
        return new AnalysisResult(
                res.label(),
                res.confidence(),
                res.probabilities(),
                res.bounding_boxes(),
                res.is_vegetation(),
                res.frame_width(),
                res.frame_height(),
                sessionId
        );
    }

    private SessionSummary toSessionSummary(ScanSession s) {
        return new SessionSummary(
                s.getId().toString(),
                s.getTotalFrames(),
                s.getAffectedFrames(),
                s.getAvgConfidence(),
                s.getDominantLabel(),
                s.getStatus().name(),
                s.affectionPct()
        );
    }
}
