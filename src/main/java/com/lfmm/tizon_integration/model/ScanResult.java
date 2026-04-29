package com.lfmm.tizon_integration.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que representa el resultado de análisis de un frame individual.
 *
 * <p>Por cada frame enviado por el móvil se crea un registro aquí,
 * asociado a su {@link ScanSession} padre.
 *
 * <p>Las probabilidades se almacenan como columnas separadas
 * para facilitar consultas de agregación (promedio de Tizón Tardío, etc.).
 * Las bounding boxes se guardan como JSON serializado para no
 * necesitar una tabla adicional.
 *
 * <p>Tabla en PostgreSQL: {@code scan_results}
 */
@Entity
@Table(
        name = "scan_results",
        indexes = {
                @Index(name = "idx_result_session_id", columnList = "session_id"),
                @Index(name = "idx_result_analyzed_at", columnList = "analyzed_at")
        }
)
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

    /** Sesión de escaneo a la que pertenece este frame. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ScanSession session;

    // ── Resultado de clasificación ────────────────────────────────────────

    /** Clase predicha: Sano | Tizon Temprano | Tizon Tardio | Sin deteccion */
    @Column(nullable = false, length = 50)
    private String label;

    /** Confianza de la predicción dominante [0.0 – 1.0] */
    @Column(nullable = false)
    private float confidence;

    // Probabilidades por clase (columnas separadas para consultas analíticas)
    @Column(name = "prob_sano")
    private float probSano;

    @Column(name = "prob_tizon_temprano")
    private float probTizonTemprano;

    @Column(name = "prob_tizon_tardio")
    private float probTizonTardio;

    // ── Metadatos del frame ───────────────────────────────────────────────

    /**
     * Lista de bounding boxes serializada como JSON.
     * Ejemplo: [{"x":0.1,"y":0.2,"width":0.3,"height":0.4,"label":"Tizon Tardio",...}]
     */
    @Column(name = "bounding_boxes_json", columnDefinition = "TEXT")
    private String boundingBoxesJson;

    @Column(name = "is_vegetation")
    private boolean isVegetation;

    @Column(name = "frame_width")
    private int frameWidth;

    @Column(name = "frame_height")
    private int frameHeight;

    @CreationTimestamp
    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private LocalDateTime analyzedAt;

    // ── Métodos de dominio ────────────────────────────────────────────────

    /** True si este frame tiene tizón (temprano o tardío). */
    public boolean isAffected() {
        return label != null
                && label.contains("Tizon")
                && !label.equals("Sin deteccion");
    }
}
