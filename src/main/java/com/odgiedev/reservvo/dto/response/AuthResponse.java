package com.odgiedev.reservvo.dto.response;

import com.odgiedev.reservvo.enums.UserRole;
import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String name,
        String email,
        UserRole role
) {}