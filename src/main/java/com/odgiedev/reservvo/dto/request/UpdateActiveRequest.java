package com.odgiedev.reservvo.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveRequest(
        @NotNull(message = "Active é obrigatório")
        Boolean active
) {}