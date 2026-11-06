package com.lfmm.tizon_integration.repository;

import com.lfmm.tizon_integration.model.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScanResultRepository extends JpaRepository<ScanResult, UUID> {

    long countBySessionId(UUID sessionId);
}
