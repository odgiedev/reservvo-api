package com.odgiedev.reservvo.repository;

import com.odgiedev.reservvo.entity.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, UUID> {
    List<AvailabilityRule> findByResourceId(UUID resourceId);
    void deleteByResourceId(UUID resourceId);
}