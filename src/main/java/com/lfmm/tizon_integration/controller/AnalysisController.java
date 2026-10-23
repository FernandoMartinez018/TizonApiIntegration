package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.dto.AnalysisResult;
import com.lfmm.tizon_integration.dto.FrameRequest;
import com.lfmm.tizon_integration.dto.SessionStartResponse;
import com.lfmm.tizon_integration.dto.SessionSummary;
import com.lfmm.tizon_integration.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/frame")
    public ResponseEntity<AnalysisResult> analyzeFrame(
            @Valid @RequestBody FrameRequest request,
            @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(analysisService.analyzeFrame(request, user.getUsername()));
    }

    @PostMapping("/sessions/start")
    public ResponseEntity<SessionStartResponse> startSession(
            @AuthenticationPrincipal UserDetails user) {

        log.info("Iniciando sesión para usuario: {}", user.getUsername());
        SessionStartResponse response = analysisService.startSession(user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<SessionSummary> endSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails user) {

        log.info("Finalizando sesión: {} para usuario: {}", sessionId, user.getUsername());
        SessionSummary summary = analysisService.endSession(sessionId, user.getUsername());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/sessions/history")
    public ResponseEntity<List<SessionSummary>> getHistory(
            @AuthenticationPrincipal UserDetails user) {

        List<SessionSummary> history = analysisService.getHistory(user.getUsername());
        return ResponseEntity.ok(history);
    }
}
