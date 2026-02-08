package com.odgiedev.reservvo.repository;

import com.odgiedev.reservvo.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {
    Optional<Provider> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}