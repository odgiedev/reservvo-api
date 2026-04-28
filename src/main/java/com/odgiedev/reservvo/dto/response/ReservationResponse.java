package com.odgiedev.reservvo.dto.response;

import com.odgiedev.reservvo.enums.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID resourceId,
        String resourceName,
        UUID clientId,
        String clientName,
        String clientPhone,
        String businessName,
        String providerPhone,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ReservationStatus status,
        String notes,
        LocalDateTime createdAt
) {}