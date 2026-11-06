package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.dto.FrameAnalysis;
import com.lfmm.tizon_integration.dto.FrameRequest;
import com.lfmm.tizon_integration.dto.SessionPage;
import com.lfmm.tizon_integration.dto.SessionSummary;
import com.lfmm.tizon_integration.dto.StartSessionRequest;
import com.lfmm.tizon_integration.model.ScanSession.SessionStatus;
import com.lfmm.tizon_integration.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessions;

    @PostMapping
    public ResponseEntity<SessionSummary> start(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody(required = false) StartSessionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(sessions.start(user.getUsername(), request));
    }

    @GetMapping
    public SessionPage list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) SessionStatus status) {

        return sessions.list(user, page, size, username, status);
    }

    @GetMapping("/{sessionId}")
    public SessionSummary get(@AuthenticationPrincipal UserDetails user, @PathVariable UUID sessionId) {
        return sessions.get(user, sessionId);
    }

    @PostMapping("/{sessionId}/frames")
    public FrameAnalysis analyzeFrame(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID sessionId,
            @Valid @RequestBody FrameRequest request) {

        return sessions.analyzeFrame(user.getUsername(), sessionId, request, MDC.get("request_id"));
    }

    @PostMapping("/{sessionId}/cancel")
    public SessionSummary cancel(@AuthenticationPrincipal UserDetails user, @PathVariable UUID sessionId) {
        return sessions.cancel(user.getUsername(), sessionId);
    }

    @PostMapping("/{sessionId}/end")
    public SessionSummary end(@AuthenticationPrincipal UserDetails user, @PathVariable UUID sessionId) {
        return sessions.end(user.getUsername(), sessionId);
    }
}
