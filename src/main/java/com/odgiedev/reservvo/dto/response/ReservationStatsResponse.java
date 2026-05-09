package com.odgiedev.reservvo.dto.response;

public record ReservationStatsResponse(
        long confirmed,
        long completed,
        long cancelledByProvider,
        long cancelledByClient,
        long total
) {}
