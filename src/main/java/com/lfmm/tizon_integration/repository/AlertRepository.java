package com.lfmm.tizon_integration.repository;

import com.lfmm.tizon_integration.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findBySessionId(UUID sessionId);

    /** username/status null = sin filtro; since nunca es null (se usa Instant.EPOCH). */
    @Query("SELECT a FROM Alert a " +
            "WHERE (:username IS NULL OR a.username = :username) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND a.createdAt > :since " +
            "ORDER BY a.createdAt DESC")
    List<Alert> search(@Param("username") String username,
                       @Param("status") Alert.Status status,
                       @Param("since") Instant since);
}
