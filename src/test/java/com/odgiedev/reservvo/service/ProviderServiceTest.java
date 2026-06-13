package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.ProviderRequest;
import com.odgiedev.reservvo.dto.response.ProviderResponse;
import com.odgiedev.reservvo.entity.Provider;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderService")
class ProviderServiceTest {

    @Mock private ProviderRepository providerRepository;

    @InjectMocks private ProviderService providerService;

    private User user;
    private Provider provider;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("hash")
                .role(UserRole.PROVIDER)
                .build();

        provider = Provider.builder()
                .id(UUID.randomUUID())
                .user(user)
                .businessName("Barbearia do João")
                .description("Cortes modernos")
                .category("Barbearia")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar negócio com sucesso")
        void shouldCreateProviderSuccessfully() {
            ProviderRequest request = new ProviderRequest(
                    "Barbearia do João", "barbearia-do-joao", "Cortes modernos", "Barbearia", null
            );

            when(providerRepository.existsByUserId(user.getId())).thenReturn(false);
            when(providerRepository.save(any())).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.prePersist();
                return p;
            });

            ProviderResponse response = providerService.create(request, user);

            assertThat(response).isNotNull();
            assertThat(response.businessName()).isEqualTo("Barbearia Do João");
            assertThat(response.description()).isEqualTo("Cortes modernos");
            assertThat(response.category()).isEqualTo("Barbearia");
            assertThat(response.userId()).isEqualTo(user.getId());

            verify(providerRepository).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando usuário já possui negócio")
        void shouldThrowWhenProviderAlreadyExists() {
            ProviderRequest request = new ProviderRequest(
                    "Outro Negócio", "outro-negocio", null, null, null
            );

            when(providerRepository.existsByUserId(user.getId())).thenReturn(true);

            assertThatThrownBy(() -> providerService.create(request, user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Usuário já possui um negócio cadastrado");

            verify(providerRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve criar negócio sem descrição e categoria")
        void shouldCreateProviderWithoutOptionalFields() {
            ProviderRequest request = new ProviderRequest("Barbearia", "barbearia-slug", null, null, null);

            when(providerRepository.existsByUserId(user.getId())).thenReturn(false);
            when(providerRepository.save(any())).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.prePersist();
                return p;
            });

            ProviderResponse response = providerService.create(request, user);

            assertThat(response.description()).isNull();
            assertThat(response.category()).isNull();
        }
    }

    @Nested
    @DisplayName("getByUser()")
    class GetByUser {

        @Test
        @DisplayName("deve retornar negócio do usuário")
        void shouldReturnProviderForUser() {
            when(providerRepository.findByUserId(user.getId())).thenReturn(Optional.of(provider));

            ProviderResponse response = providerService.getByUser(user);

            assertThat(response).isNotNull();
            assertThat(response.businessName()).isEqualTo(provider.getBusinessName());
            assertThat(response.userId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("deve lançar exceção quando negócio não encontrado")
        void shouldThrowWhenProviderNotFound() {
            when(providerRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getByUser(user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Negócio não encontrado");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar negócio com sucesso")
        void shouldUpdateProviderSuccessfully() {
            ProviderRequest request = new ProviderRequest(
                    "Barbearia Atualizada", "barbearia-atualizada", "Nova descrição", "Beleza", null
            );

            when(providerRepository.findByUserId(user.getId())).thenReturn(Optional.of(provider));
            when(providerRepository.save(any())).thenReturn(provider);

            ProviderResponse response = providerService.update(request, user);

            assertThat(response.businessName()).isEqualTo("Barbearia Atualizada");
            assertThat(response.description()).isEqualTo("Nova descrição");
            assertThat(response.category()).isEqualTo("Beleza");

            verify(providerRepository).save(provider);
        }

        @Test
        @DisplayName("deve lançar exceção ao atualizar negócio inexistente")
        void shouldThrowWhenProviderNotFoundOnUpdate() {
            ProviderRequest request = new ProviderRequest("Nome", "slug", null, null, null);

            when(providerRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.update(request, user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Negócio não encontrado");

            verify(providerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getEntityByUser()")
    class GetEntityByUser {

        @Test
        @DisplayName("deve retornar entidade provider")
        void shouldReturnProviderEntity() {
            when(providerRepository.findByUserId(user.getId())).thenReturn(Optional.of(provider));

            Provider result = providerService.getEntityByUser(user);

            assertThat(result).isEqualTo(provider);
            assertThat(result.getUser().getId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("deve lançar exceção quando negócio não encontrado")
        void shouldThrowWhenNotFound() {
            when(providerRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getEntityByUser(user))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Negócio não encontrado");
        }
    }
}