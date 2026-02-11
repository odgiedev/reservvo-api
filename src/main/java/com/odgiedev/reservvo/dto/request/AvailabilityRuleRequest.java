package com.odgiedev.reservvo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record AvailabilityRuleRequest(
        @NotNull(message = "Dia da semana obrigatório")
        @Min(value = 0, message = "Dia inválido")
        @Max(value = 6, message = "Dia inválido")
        Integer dayOfWeek,

        @NotNull(message = "Horário de início obrigatório")
        LocalTime startTime,

        @NotNull(message = "Horário de fim obrigatório")
        LocalTime endTime
) {}