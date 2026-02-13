package com.odgiedev.reservvo.dto.response;

import java.util.UUID;

public record ResourceResponse(
        UUID id,
        UUID providerId,
        String name,
        String description,
        Integer slotDurationMin,
        Boolean active
) {}