package com.lfmm.tizon_integration.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(
        name = "scan_sessions",
        indexes = {
                @Index(name = "idx_session_username", columnList = "username"),
                @Index(name = "idx_session_status",   columnList = "status")
        }
)
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

    /** Estado actual de la sesión. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    /** Total de frames analizados en la sesión. */
    @Column(name = "total_frames")
    @Builder.Default
    private int totalFrames = 0;

    /**
     * Frames con detección de tizón (Temprano o Tardío).
     * Excluye frames "Sin deteccion" y "Sano".
     */
    @Column(name = "affected_frames")
    @Builder.Default
    private int affectedFrames = 0;

    /** Confianza promedio ponderada de todas las predicciones. */
    @Column(name = "avg_confidence")
    @Builder.Default
    private float avgConfidence = 0f;

    /** Etiqueta más reciente predicha (del último frame con detección). */
    @Column(name = "dominant_label", length = 50)
    private String dominantLabel;

    /** Resultados individuales de cada frame. Lazy para evitar N+1. */
    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ScanResult> results = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Métodos de dominio ────────────────────────────────────────────────

    /**
     * Porcentaje de frames afectados sobre el total analizado.
     *
     * @return valor entre 0.0 y 100.0
     */
    public float affectionPct() {
        if (totalFrames == 0) return 0f;
        return (affectedFrames / (float) totalFrames) * 100f;
    }

    // ── Enum de estado ────────────────────────────────────────────────────

    public enum SessionStatus {
        /** La cámara está activa y enviando frames. */
        ACTIVE,
        /** El usuario detuvo el escaneo voluntariamente. */
        COMPLETED,
        /** La sesión fue cancelada (timeout, error, etc.). */
        CANCELLED
    }
}
