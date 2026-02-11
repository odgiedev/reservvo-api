package com.odgiedev.reservvo.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationRequest(
        @NotNull(message = "Recurso obrigatório")
        UUID resourceId,

        @NotNull(message = "Data obrigatória")
        @Future(message = "Data deve ser no futuro")
        LocalDate date,

        @NotNull(message = "Horário de início obrigatório")
        LocalTime startTime,

        String notes
) {}