package com.lfmm.tizon_integration.repository;

import com.lfmm.tizon_integration.model.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScanResultRepository extends JpaRepository<ScanResult, UUID> {

    List<ScanResult> findBySessionIdOrderByAnalyzedAtDesc(UUID sessionId);

    long countBySessionId(UUID sessionId);

    @Query("SELECT COUNT(r) FROM ScanResult r " +
            "WHERE r.session.id = :sessionId " +
            "AND r.label != 'Sano' AND r.label != 'Sin deteccion'")
    long countAffectedBySessionId(@Param("sessionId") UUID sessionId);

    @Query("SELECT AVG(r.confidence) FROM ScanResult r " +
            "WHERE r.session.id = :sessionId")
    Double avgConfidenceBySessionId(@Param("sessionId") UUID sessionId);
}
