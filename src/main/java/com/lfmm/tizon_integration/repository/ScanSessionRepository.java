package com.lfmm.tizon_integration.repository;

import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.model.ScanSession.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScanSessionRepository extends JpaRepository<ScanSession, UUID> {

    /** Filtros opcionales: null = sin filtro. */
    @Query("SELECT s FROM ScanSession s " +
            "WHERE (:username IS NULL OR s.username = :username) " +
            "AND (:status IS NULL OR s.status = :status)")
    Page<ScanSession> search(@Param("username") String username,
                             @Param("status") SessionStatus status,
                             Pageable pageable);
}
