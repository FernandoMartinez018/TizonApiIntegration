package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.config.TizonProperties.Thresholds;
import com.lfmm.tizon_integration.dto.SessionStats;
import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.FrameStatus;
import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.model.Severity;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Reglas de SessionStats del contrato, como funciones puras (sin Spring ni base de datos).
 * affection_pct usa solo los frames DETECTED como denominador; afectado = TIZON_TEMPRANO o TIZON_TARDIO.
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

    /** Frames DETECTED por clase principal. */
    public static Map<ClassId, Integer> labelCounts(ScanSession s) {
        Map<ClassId, Integer> counts = new EnumMap<>(ClassId.class);
        counts.put(ClassId.SANO, s.getAnalyzedFrames() - s.getEarlyBlightFrames() - s.getLateBlightFrames());
        counts.put(ClassId.TIZON_TEMPRANO, s.getEarlyBlightFrames());
        counts.put(ClassId.TIZON_TARDIO, s.getLateBlightFrames());
        return counts;
    }

    /** Clase más frecuente entre los DETECTED; en empate gana la más severa (orden de ClassId). */
    public static ClassId dominantLabel(Map<ClassId, Integer> counts) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .max(Map.Entry.<ClassId, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /** Suma un frame ya persistido a los contadores de la sesión. */
    public static void apply(ScanSession s, FrameStatus status, ClassId label, Double confidence, Thresholds t) {
        s.setTotalFrames(s.getTotalFrames() + 1);
        if (status == FrameStatus.NO_VEGETATION) {
            s.setNoVegetationFrames(s.getNoVegetationFrames() + 1);
            return;
        }
        if (status == FrameStatus.NOT_POTATO) {
            s.setNotPotatoFrames(s.getNotPotatoFrames() + 1);
            return;
        }
        int analyzed = s.getAnalyzedFrames() + 1;
        if (confidence != null) {
            double prevAvg = s.getAvgConfidence() == null ? 0.0 : s.getAvgConfidence();
            s.setAvgConfidence((prevAvg * (analyzed - 1) + confidence) / analyzed);
        }
        s.setAnalyzedFrames(analyzed);
        if (label == ClassId.TIZON_TEMPRANO) s.setEarlyBlightFrames(s.getEarlyBlightFrames() + 1);
        if (label == ClassId.TIZON_TARDIO) s.setLateBlightFrames(s.getLateBlightFrames() + 1);
        if (label != null && label.affected()) s.setAffectedFrames(s.getAffectedFrames() + 1);
        s.setDominantLabel(dominantLabel(labelCounts(s)));
        s.setSeverity(severity(affectionPct(s.getAffectedFrames(), analyzed), t));
    }

    public static SessionStats stats(ScanSession s) {
        return new SessionStats(
                s.getTotalFrames(),
                s.getAnalyzedFrames(),
                s.getAffectedFrames(),
                s.getNoVegetationFrames(),
                s.getNotPotatoFrames(),
                affectionPct(s.getAffectedFrames(), s.getAnalyzedFrames()),
                s.getSeverity(),
                s.getAvgConfidence(),
                s.getDominantLabel(),
                labelCounts(s));
    }
}
