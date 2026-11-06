package com.lfmm.tizon_integration.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** RNF-SEG-09: 5 fallos en 5 min bloquean 5 min; los fallos viejos no cuentan. */
class LoginAttemptServiceTest {

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-01T10:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService attempts = new LoginAttemptService(clock);

    @Test
    void bloqueaAlQuintoFalloYDesbloqueaTrasCincoMinutos() {
        for (int i = 0; i < 4; i++) attempts.onFailure("ana");
        assertThat(attempts.isBlocked("ana")).isFalse();
        attempts.onFailure("ana");
        assertThat(attempts.isBlocked("ana")).isTrue();
        assertThat(attempts.isBlocked("beto")).isFalse();

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));
        assertThat(attempts.isBlocked("ana")).isFalse();
    }

    @Test
    void losFallosFueraDeLaVentanaNoCuentan() {
        for (int i = 0; i < 4; i++) attempts.onFailure("ana");
        clock.advance(Duration.ofMinutes(6));
        attempts.onFailure("ana");
        assertThat(attempts.isBlocked("ana")).isFalse();
    }

    @Test
    void unLoginExitosoReiniciaElConteo() {
        for (int i = 0; i < 4; i++) attempts.onFailure("ana");
        attempts.onSuccess("ana");
        attempts.onFailure("ana");
        assertThat(attempts.isBlocked("ana")).isFalse();
    }
}
