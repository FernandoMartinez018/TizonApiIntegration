package com.lfmm.tizon_integration.repository;

import com.lfmm.tizon_integration.model.ScanSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ScanSessionRepository extends JpaRepository<ScanSession, UUID> {

    List<ScanSession> findByUsernameOrderByStartedAtDesc(String username);

    Optional<ScanSession> findByIdAndUsername(UUID id, String username);

//    List<ScanSession> findByUsernameAndStatusOrderByStartedAtDesc(
//            String username,
//            SessionStatus status
//    );

    @Query("SELECT COUNT(s) FROM ScanSession s " +
            "WHERE s.username = :username AND s.status = 'COMPLETED'")
    long countCompletedByUsername(@Param("username") String username);

    @Query("SELECT AVG(s.affectedFrames * 1.0 / NULLIF(s.totalFrames, 0)) " +
            "FROM ScanSession s " +
            "WHERE s.username = :username AND s.status = 'COMPLETED'")
    Double avgAffectionRateByUsername(@Param("username") String username);
}
