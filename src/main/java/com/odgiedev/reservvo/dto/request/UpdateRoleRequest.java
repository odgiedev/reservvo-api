package com.odgiedev.reservvo.dto.request;

import com.odgiedev.reservvo.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "Role é obrigatório")
        UserRole role
)
{}
