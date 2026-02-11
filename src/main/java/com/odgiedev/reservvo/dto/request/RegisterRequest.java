package com.odgiedev.reservvo.dto.request;

import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.exception.validation.NotBlankOrSymbols;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlankOrSymbols(message = "Nome obrigatório")
        String name,

        @NotBlank(message = "Email obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Senha obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password,

        @NotNull(message = "Role obrigatória")
        UserRole role,

        String phone
) {}