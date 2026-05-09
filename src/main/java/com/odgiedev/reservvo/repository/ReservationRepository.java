package com.odgiedev.reservvo.repository;

import com.odgiedev.reservvo.entity.Reservation;
import com.odgiedev.reservvo.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query(value = """
            SELECT DISTINCT r FROM Reservation r
            JOIN FETCH r.resource res
            JOIN FETCH res.provider prov
            JOIN FETCH r.client
            WHERE r.client.id = :clientId
            """,
            countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.client.id = :clientId")
    Page<Reservation> findByClientId(@Param("clientId") UUID clientId, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT r FROM Reservation r
            JOIN FETCH r.resource res
            JOIN FETCH res.provider prov
            JOIN FETCH r.client
            WHERE r.client.id = :clientId
              AND r.status = :status
            """,
            countQuery = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.client.id = :clientId AND r.status = :status
            """)
    Page<Reservation> findByClientIdAndStatus(
            @Param("clientId") UUID clientId,
            @Param("status") ReservationStatus status,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT r FROM Reservation r
            JOIN FETCH r.resource res
            JOIN FETCH res.provider prov
            JOIN FETCH r.client
            WHERE res.provider.user.id = :userId
            """,
            countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.resource.provider.user.id = :userId")
    Page<Reservation> findByResourceProviderUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT r FROM Reservation r
            JOIN FETCH r.resource res
            JOIN FETCH res.provider prov
            JOIN FETCH r.client
            WHERE res.provider.user.id = :userId
              AND r.status = :status
            """,
            countQuery = """
            SELECT COUNT(r) FROM Reservation r
            WHERE r.resource.provider.user.id = :userId AND r.status = :status
            """)
    Page<Reservation> findByResourceProviderUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") ReservationStatus status,
            Pageable pageable);

    @Query("""
        SELECT r.status AS status, COUNT(r) AS count
        FROM Reservation r
        WHERE r.client.id = :clientId
        GROUP BY r.status
    """)
    List<StatusCount> countByStatusForClient(@Param("clientId") UUID clientId);

    @Query("""
        SELECT r.status AS status, COUNT(r) AS count
        FROM Reservation r
        WHERE r.resource.provider.user.id = :userId
        GROUP BY r.status
    """)
    List<StatusCount> countByStatusForProvider(@Param("userId") UUID userId);

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

    @Modifying
    @Query("""
        UPDATE Reservation r
        SET r.status = com.odgiedev.reservvo.enums.ReservationStatus.COMPLETED,
            r.updatedAt = CURRENT_TIMESTAMP
        WHERE r.status = com.odgiedev.reservvo.enums.ReservationStatus.CONFIRMED
        AND (r.date < :today OR (r.date = :today AND r.endTime <= :now))
    """)
    int markExpiredAsCompleted(@Param("today") LocalDate today, @Param("now") LocalTime now);

    interface StatusCount {
        ReservationStatus getStatus();
        Long getCount();
    }
}
