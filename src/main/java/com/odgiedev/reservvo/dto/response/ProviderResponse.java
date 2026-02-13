package com.odgiedev.reservvo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        UUID userId,
        String businessName,
        String description,
        String category,
        LocalDateTime createdAt
) {}