package com.lfmm.tizon_integration.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Sesión de escaneo. Guarda contadores agregados para no recalcular sobre todos los frames.
 * Tabla {@code scan_sessions} (Flyway V1__baseline.sql).
 */
@Entity
@Table(name = "scan_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Usuario propietario de la sesión (username del JWT). */
    @Column(nullable = false, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    // ── Contadores (SessionStats del contrato) ────────────────────────────

    /** Frames persistidos: DETECTED + NO_VEGETATION + NOT_POTATO. */
    @Column(name = "total_frames", nullable = false)
    @Builder.Default
    private int totalFrames = 0;

    /** Frames DETECTED. */
    @Column(name = "analyzed_frames", nullable = false)
    @Builder.Default
    private int analyzedFrames = 0;

    /** Frames DETECTED con una clase afectada (TIZON_TEMPRANO + TIZON_TARDIO). */
    @Column(name = "affected_frames", nullable = false)
    @Builder.Default
    private int affectedFrames = 0;

    @Column(name = "early_blight_frames", nullable = false)
    @Builder.Default
    private int earlyBlightFrames = 0;

    @Column(name = "late_blight_frames", nullable = false)
    @Builder.Default
    private int lateBlightFrames = 0;

    @Column(name = "no_vegetation_frames", nullable = false)
    @Builder.Default
    private int noVegetationFrames = 0;

    /** Frames con hojas de otras plantas: no se analiza el tizón. */
    @Column(name = "not_potato_frames", nullable = false)
    @Builder.Default
    private int notPotatoFrames = 0;

    /** Promedio de confianza solo sobre frames DETECTED; null si no hay. */
    @Column(name = "avg_confidence")
    private Double avgConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "dominant_label", length = 20)
    private ClassId dominantLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.NONE;

    // ── Contexto ──────────────────────────────────────────────────────────

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lon")
    private Double locationLon;

    @Column(name = "location_accuracy_m")
    private Double locationAccuracyM;

    @Column(name = "field_name", length = 120)
    private String fieldName;

    // ── Tiempos ───────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "last_frame_at")
    private Instant lastFrameAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum SessionStatus {
        /** La cámara está activa y enviando frames. */
        ACTIVE,
        /** El usuario finalizó el escaneo. */
        COMPLETED,
        /** Descartada por el usuario o abandonada (sin frames por 30 min). */
        CANCELLED
    }
}
