package com.lfmm.tizon_integration.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Recomendación por severidad con su fuente. Sembrada por Flyway (V2__recommendations.sql). */
@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor
public class Recommendation {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false, length = 300)
    private String source;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
