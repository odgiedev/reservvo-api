package com.odgiedev.reservvo.repository;

import com.odgiedev.reservvo.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    List<Resource> findByProviderIdAndActiveTrue(UUID providerId);
}