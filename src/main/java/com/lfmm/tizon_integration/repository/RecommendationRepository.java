package com.lfmm.tizon_integration.repository;

import com.lfmm.tizon_integration.model.Recommendation;
import com.lfmm.tizon_integration.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Integer> {

    List<Recommendation> findBySeverityOrderBySortOrder(Severity severity);
}
