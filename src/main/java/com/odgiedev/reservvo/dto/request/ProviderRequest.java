package com.odgiedev.reservvo.dto.request;

import com.odgiedev.reservvo.exception.validation.NotBlankOrSymbols;
import com.odgiedev.reservvo.exception.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ProviderRequest(
        @NotBlankOrSymbols(message = "Nome do negócio obrigatório")
        String businessName,

        @NotBlankOrSymbols(message = "Slug obrigatório")
        String slug,
        String description,
        String category,

        @ValidPhone
        String phone
) {}