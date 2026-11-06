package com.lfmm.tizon_integration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Cada 5 min cancela las sesiones ACTIVE sin frames durante 30 min (contrato, doc 03). */
@Component
@RequiredArgsConstructor
public class SessionCleanupJob {

    private final SessionService sessions;

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void cancelAbandonedSessions() {
        sessions.cancelInactive(Instant.now());
    }
}
