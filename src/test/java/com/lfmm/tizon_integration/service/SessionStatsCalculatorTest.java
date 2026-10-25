package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.config.TizonProperties.Thresholds;
import com.lfmm.tizon_integration.dto.SessionStats;
import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.FrameStatus;
import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.model.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** F3-API-03 / RF-08 CA4: reglas de SessionStats como función pura, con casos borde. */
class SessionStatsCalculatorTest {

    private static final Thresholds T = new Thresholds(10, 30);

    private static ScanSession session() {
        return ScanSession.builder().username("ana").build();
    }

    private static void frames(ScanSession s, int sano, int tizon, int noVeg, double confidence) {
        for (int i = 0; i < sano; i++) SessionStatsCalculator.apply(s, FrameStatus.DETECTED, ClassId.SANO, confidence, T);
        for (int i = 0; i < tizon; i++) SessionStatsCalculator.apply(s, FrameStatus.DETECTED, ClassId.TIZON_TARDIO, confidence, T);
        for (int i = 0; i < noVeg; i++) SessionStatsCalculator.apply(s, FrameStatus.NO_VEGETATION, null, null, T);
    }

    @Test
    void ceroFrames() {
        SessionStats stats = SessionStatsCalculator.stats(session());
        assertThat(stats.totalFrames()).isZero();
        assertThat(stats.affectionPct()).isZero();
        assertThat(stats.severity()).isEqualTo(Severity.NONE);
        assertThat(stats.avgConfidence()).isNull();
        assertThat(stats.dominantLabel()).isNull();
    }

    @Test
    void todosNoVegetationNoCuentanEnElDenominador() {
        ScanSession s = session();
        frames(s, 0, 0, 5, 0);
        SessionStats stats = SessionStatsCalculator.stats(s);
        assertThat(stats.totalFrames()).isEqualTo(5);
        assertThat(stats.noVegetationFrames()).isEqualTo(5);
        assertThat(stats.analyzedFrames()).isZero();
        assertThat(stats.affectionPct()).isZero();
        assertThat(stats.severity()).isEqualTo(Severity.NONE);
        assertThat(stats.avgConfidence()).isNull();
        assertThat(stats.dominantLabel()).isNull();
    }

    @Test
    void cienPorCientoAfectado() {
        ScanSession s = session();
        frames(s, 0, 4, 2, 0.9);
        SessionStats stats = SessionStatsCalculator.stats(s);
        assertThat(stats.affectionPct()).isEqualTo(100.0);
        assertThat(stats.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(stats.dominantLabel()).isEqualTo(ClassId.TIZON_TARDIO);
        assertThat(stats.totalFrames()).isEqualTo(6);
    }

    @Test
    void promedioDeConfianzaSoloSobreDetected() {
        ScanSession s = session();
        SessionStatsCalculator.apply(s, FrameStatus.DETECTED, ClassId.SANO, 0.8, T);
        SessionStatsCalculator.apply(s, FrameStatus.NO_VEGETATION, null, null, T);
        SessionStatsCalculator.apply(s, FrameStatus.DETECTED, ClassId.SANO, 0.6, T);
        assertThat(SessionStatsCalculator.stats(s).avgConfidence()).isCloseTo(0.7, within(1e-9));
    }

    @Test
    void empateDeClasesGanaLaMasSevera() {
        ScanSession s = session();
        frames(s, 2, 2, 0, 0.9);
        assertThat(SessionStatsCalculator.stats(s).dominantLabel()).isEqualTo(ClassId.TIZON_TARDIO);
    }

    @Test
    void tizonTempranoCuentaComoAfectadoYSeCuentaAparte() {
        ScanSession s = session();
        frames(s, 2, 1, 0, 0.9);
        for (int i = 0; i < 3; i++) SessionStatsCalculator.apply(s, FrameStatus.DETECTED, ClassId.TIZON_TEMPRANO, 0.9, T);
        SessionStats stats = SessionStatsCalculator.stats(s);
        assertThat(stats.affectedFrames()).isEqualTo(4);
        assertThat(stats.affectionPct()).isCloseTo(66.67, within(0.01));
        assertThat(stats.labelCounts()).containsExactly(
                java.util.Map.entry(ClassId.SANO, 2), java.util.Map.entry(ClassId.TIZON_TEMPRANO, 3),
                java.util.Map.entry(ClassId.TIZON_TARDIO, 1));
        assertThat(stats.dominantLabel()).isEqualTo(ClassId.TIZON_TEMPRANO);
    }

    @Test
    void empateEntreTizonesGanaElTardio() {
        ScanSession s = session();
        for (int i = 0; i < 2; i++) SessionStatsCalculator.apply(s, FrameStatus.DETECTED, ClassId.TIZON_TEMPRANO, 0.9, T);
        frames(s, 0, 2, 0, 0.9);
        assertThat(SessionStatsCalculator.stats(s).dominantLabel()).isEqualTo(ClassId.TIZON_TARDIO);
    }

    @Test
    void enfermedadPredominanteSoloEntreLosAfectados() {
        ScanSession s = session();
        frames(s, 5, 0, 0, 0.9);
        assertThat(SessionStatsCalculator.predominantDisease(s)).isNull();

        frames(s, 0, 1, 0, 0.9);
        for (int i = 0; i < 2; i++) SessionStatsCalculator.apply(s, FrameStatus.DETECTED, ClassId.TIZON_TEMPRANO, 0.9, T);
        assertThat(SessionStatsCalculator.stats(s).dominantLabel()).isEqualTo(ClassId.SANO);
        assertThat(SessionStatsCalculator.predominantDisease(s)).isEqualTo(ClassId.TIZON_TEMPRANO);

        frames(s, 0, 1, 0, 0.9);
        assertThat(SessionStatsCalculator.predominantDisease(s)).isEqualTo(ClassId.TIZON_TARDIO);
    }

    @Test
    void hojasDeOtrasPlantasNoCuentanEnElDenominador() {
        ScanSession s = session();
        frames(s, 1, 0, 0, 0.9);
        for (int i = 0; i < 3; i++) SessionStatsCalculator.apply(s, FrameStatus.NOT_POTATO, null, null, T);
        SessionStats stats = SessionStatsCalculator.stats(s);
        assertThat(stats.totalFrames()).isEqualTo(4);
        assertThat(stats.notPotatoFrames()).isEqualTo(3);
        assertThat(stats.analyzedFrames()).isEqualTo(1);
        assertThat(stats.affectionPct()).isZero();
    }

    @ParameterizedTest(name = "{0} % → {1}")
    @CsvSource({"0, NONE", "0.1, LOW", "10, LOW", "10.01, WARNING", "30, WARNING", "30.01, CRITICAL", "100, CRITICAL"})
    void severidadSegunUmbrales(double pct, Severity expected) {
        assertThat(SessionStatsCalculator.severity(pct, T)).isEqualTo(expected);
    }
}
