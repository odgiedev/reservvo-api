package com.odgiedev.reservvo.dto.response;

import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityRuleResponse(
        UUID id,
        UUID resourceId,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {}