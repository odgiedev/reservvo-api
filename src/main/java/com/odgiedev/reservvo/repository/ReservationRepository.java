package com.odgiedev.reservvo.repository;

import com.odgiedev.reservvo.entity.Reservation;
import com.odgiedev.reservvo.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByClientIdOrderByDateDescStartTimeDesc(UUID clientId);

    List<Reservation> findByResourceProviderIdOrderByDateDescStartTimeDesc(UUID providerId);
    List<Reservation> findByResourceProviderUserIdOrderByDateDescStartTimeDesc(UUID userId);

    // Verifica conflito de horário para evitar double booking
    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r
        WHERE r.resource.id = :resourceId
        AND r.date = :date
        AND r.status = 'CONFIRMED'
        AND r.startTime < :endTime
        AND r.endTime > :startTime
    """)
    boolean existsConflict(
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    List<Reservation> findByResourceIdAndDateAndStatus(
            UUID resourceId,
            LocalDate date,
            ReservationStatus status
    );
}