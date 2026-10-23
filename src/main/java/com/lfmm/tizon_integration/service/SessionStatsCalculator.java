package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.config.TizonProperties.Thresholds;
import com.lfmm.tizon_integration.dto.SessionStats;
import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.FrameStatus;
import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.model.Severity;

/**
 * Reglas de SessionStats del contrato, como funciones puras (sin Spring ni base de datos).
 * affection_pct usa solo los frames DETECTED como denominador.
 */
public final class SessionStatsCalculator {

    private SessionStatsCalculator() {}

    public static double affectionPct(int affectedFrames, int analyzedFrames) {
        return analyzedFrames == 0 ? 0.0 : affectedFrames * 100.0 / analyzedFrames;
    }

    public static Severity severity(double affectionPct, Thresholds t) {
        if (affectionPct <= 0) return Severity.NONE;
        if (affectionPct <= t.warningPct()) return Severity.LOW;
        if (affectionPct <= t.criticalPct()) return Severity.WARNING;
        return Severity.CRITICAL;
    }

    /** Clase más frecuente entre los DETECTED; en empate gana la más severa. */
    public static ClassId dominantLabel(int affectedFrames, int analyzedFrames) {
        if (analyzedFrames == 0) return null;
        int healthy = analyzedFrames - affectedFrames;
        return affectedFrames >= healthy ? ClassId.TIZON_TARDIO : ClassId.SANO;
    }

    /** Suma un frame ya persistido a los contadores de la sesión. */
    public static void apply(ScanSession s, FrameStatus status, ClassId label, Double confidence, Thresholds t) {
        s.setTotalFrames(s.getTotalFrames() + 1);
        if (status == FrameStatus.NO_VEGETATION) {
            s.setNoVegetationFrames(s.getNoVegetationFrames() + 1);
            return;
        }
        int analyzed = s.getAnalyzedFrames() + 1;
        if (confidence != null) {
            double prevAvg = s.getAvgConfidence() == null ? 0.0 : s.getAvgConfidence();
            s.setAvgConfidence((prevAvg * (analyzed - 1) + confidence) / analyzed);
        }
        s.setAnalyzedFrames(analyzed);
        if (label == ClassId.TIZON_TARDIO) s.setAffectedFrames(s.getAffectedFrames() + 1);
        s.setDominantLabel(dominantLabel(s.getAffectedFrames(), analyzed));
        s.setSeverity(severity(affectionPct(s.getAffectedFrames(), analyzed), t));
    }

    public static SessionStats stats(ScanSession s) {
        return new SessionStats(
                s.getTotalFrames(),
                s.getAnalyzedFrames(),
                s.getAffectedFrames(),
                s.getNoVegetationFrames(),
                affectionPct(s.getAffectedFrames(), s.getAnalyzedFrames()),
                s.getSeverity(),
                s.getAvgConfidence(),
                s.getDominantLabel());
    }
}
