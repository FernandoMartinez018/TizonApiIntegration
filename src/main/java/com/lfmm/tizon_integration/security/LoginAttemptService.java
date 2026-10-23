package com.lfmm.tizon_integration.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RNF-SEG-09: 5 intentos fallidos por usuario en 5 minutos bloquean ese usuario 5 minutos.
 * shortcut: en memoria (un solo nodo, < 50 usuarios); si la API escala a varias réplicas, mover a la BD.
 */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(5);
    static final Duration BLOCK = Duration.ofMinutes(5);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockedUntil = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String username) {
        Instant until = blockedUntil.get(username);
        if (until == null) return false;
        if (clock.instant().isBefore(until)) return true;
        blockedUntil.remove(username);
        return false;
    }

    public void onFailure(String username) {
        Instant now = clock.instant();
        Deque<Instant> recent = failures.computeIfAbsent(username, k -> new ArrayDeque<>());
        synchronized (recent) {
            recent.addLast(now);
            while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(WINDOW))) recent.pollFirst();
            if (recent.size() >= MAX_FAILURES) {
                blockedUntil.put(username, now.plus(BLOCK));
                recent.clear();
            }
        }
    }

    public void onSuccess(String username) {
        failures.remove(username);
        blockedUntil.remove(username);
    }
}
