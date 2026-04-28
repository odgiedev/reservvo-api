package com.odgiedev.reservvo.dto.request;

import com.odgiedev.reservvo.exception.validation.NotBlankOrSymbols;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResourceRequest(
        @NotBlankOrSymbols(message = "Nome do recurso obrigatório")
        String name,

        String description,

        @NotNull(message = "Duração do slot obrigatória")
        @Min(value = 15, message = "Duração mínima de 15 minutos")
        Integer slotDurationMin
) {}