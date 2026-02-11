package com.odgiedev.reservvo.dto.request;

import com.odgiedev.reservvo.exception.validation.NotBlankOrSymbols;
import jakarta.validation.constraints.NotBlank;

public record ProviderRequest(
        @NotBlankOrSymbols(message = "Nome do negócio obrigatório")
        String businessName,

        String description,

        String category
) {}