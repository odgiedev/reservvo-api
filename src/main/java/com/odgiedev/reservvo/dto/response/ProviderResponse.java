package com.odgiedev.reservvo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        UUID userId,
        String businessName,
        String slug,
        String description,
        String category,
        String phone,
        LocalDateTime createdAt
) {}