package com.lfmm.tizon_integration.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado del análisis de un frame (DETECTED o NO_VEGETATION). Nunca guarda la imagen
 * ni errores técnicos (RF-07). Tabla {@code scan_results} (Flyway V1__baseline.sql).
 */
@Entity
@Table(name = "scan_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ScanSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FrameStatus status;

    /** Resultado principal; null si NO_VEGETATION. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClassId label;

    private Double confidence;

    @Column(name = "prob_sano")
    private Double probSano;

    @Column(name = "prob_tizon_tardio")
    private Double probTizonTardio;

    /** Regiones analizadas, serializadas como JSON (lista de Region del contrato). */
    @Column(name = "regions_json", nullable = false, columnDefinition = "TEXT")
    private String regionsJson;

    @Column(name = "frame_width", nullable = false)
    private int frameWidth;

    @Column(name = "frame_height", nullable = false)
    private int frameHeight;

    @Column(name = "model_version", nullable = false, length = 80)
    private String modelVersion;

    /** Tiempo total en Spring, incluida la IA. */
    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(name = "inference_ms", nullable = false)
    private int inferenceMs;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @CreationTimestamp
    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private Instant analyzedAt;
}
