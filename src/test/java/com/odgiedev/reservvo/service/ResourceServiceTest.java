package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.AvailabilityRuleRequest;
import com.odgiedev.reservvo.dto.request.ResourceRequest;
import com.odgiedev.reservvo.dto.request.UpdateActiveRequest;
import com.odgiedev.reservvo.dto.response.AvailabilityRuleResponse;
import com.odgiedev.reservvo.dto.response.ResourceResponse;
import com.odgiedev.reservvo.entity.*;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.AvailabilityRuleRepository;
import com.odgiedev.reservvo.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceService")
class ResourceServiceTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private AvailabilityRuleRepository availabilityRuleRepository;
    @Mock private ProviderService providerService;

    @InjectMocks private ResourceService resourceService;

    private User providerUser;
    private Provider provider;
    private Resource resource;

    @BeforeEach
    void setUp() {
        providerUser = User.builder()
                .id(UUID.randomUUID())
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("hash")
                .role(UserRole.PROVIDER)
                .build();

        provider = Provider.builder()
                .id(UUID.randomUUID())
                .user(providerUser)
                .businessName("Barbearia do João")
                .build();

        resource = Resource.builder()
                .id(UUID.randomUUID())
                .provider(provider)
                .name("Cadeira 1")
                .description("Barbeiro Pedro")
                .slotDurationMin(60)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar recurso com sucesso")
        void shouldCreateResourceSuccessfully() {
            ResourceRequest request = new ResourceRequest("Cadeira 1", "Barbeiro Pedro", 60);

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.save(any())).thenReturn(resource);

            ResourceResponse response = resourceService.create(request, providerUser);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Cadeira 1");
            assertThat(response.slotDurationMin()).isEqualTo(60);
            assertThat(response.active()).isTrue();

            verify(resourceRepository).save(any());
        }

        @Test
        @DisplayName("deve criar recurso ativo por padrão")
        void shouldCreateResourceActiveByDefault() {
            ResourceRequest request = new ResourceRequest("Cadeira 2", null, 30);

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ResourceResponse response = resourceService.create(request, providerUser);

            assertThat(response.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("listByProvider()")
    class ListByProvider {

        @Test
        @DisplayName("deve listar recursos ativos do provider")
        void shouldListActiveResources() {
            Resource resource2 = Resource.builder()
                    .id(UUID.randomUUID())
                    .provider(provider)
                    .name("Cadeira 2")
                    .slotDurationMin(60)
                    .active(true)
                    .build();

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findByProviderId(provider.getId()))
                    .thenReturn(List.of(resource, resource2));

            List<ResourceResponse> responses = resourceService.listByProvider(providerUser);

            assertThat(responses).hasSize(2);
            assertThat(responses).allMatch(ResourceResponse::active);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há recursos")
        void shouldReturnEmptyListWhenNoResources() {
            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findByProviderId(provider.getId()))
                    .thenReturn(List.of());

            List<ResourceResponse> responses = resourceService.listByProvider(providerUser);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar recurso com sucesso")
        void shouldUpdateResourceSuccessfully() {
            ResourceRequest request = new ResourceRequest("Cadeira VIP", "Novo barbeiro", 90);

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(resourceRepository.save(any())).thenReturn(resource);

            ResourceResponse response = resourceService.update(resource.getId(), request, providerUser);

            assertThat(response.name()).isEqualTo("Cadeira Vip");
            assertThat(response.slotDurationMin()).isEqualTo(90);
        }

        @Test
        @DisplayName("deve lançar exceção quando recurso não pertence ao provider")
        void shouldThrowWhenResourceNotOwnedByProvider() {
            Provider anotherProvider = Provider.builder()
                    .id(UUID.randomUUID())
                    .user(providerUser)
                    .businessName("Outro Negócio")
                    .build();

            Resource anotherResource = Resource.builder()
                    .id(UUID.randomUUID())
                    .provider(anotherProvider)
                    .name("Cadeira X")
                    .slotDurationMin(60)
                    .active(true)
                    .build();

            ResourceRequest request = new ResourceRequest("Novo Nome", null, 60);

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(anotherResource.getId()))
                    .thenReturn(Optional.of(anotherResource));

            assertThatThrownBy(() -> resourceService.update(anotherResource.getId(), request, providerUser))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Você não tem permissão sobre esse recurso");
        }

        @Test
        @DisplayName("deve lançar exceção quando recurso não encontrado")
        void shouldThrowWhenResourceNotFound() {
            ResourceRequest request = new ResourceRequest("Nome", null, 60);

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resourceService.update(UUID.randomUUID(), request, providerUser))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Recurso não encontrado");
        }
    }

    @Nested
    @DisplayName("updateActive()")
    class UpdateActive {

        @Test
        @DisplayName("deve desativar recurso quando active=false")
        void shouldDeactivateResource() {
            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(resourceRepository.save(any())).thenReturn(resource);

            resourceService.updateActive(resource.getId(), new UpdateActiveRequest(false), providerUser);

            assertThat(resource.getActive()).isFalse();
            verify(resourceRepository).save(resource);
        }

        @Test
        @DisplayName("deve ativar recurso quando active=true")
        void shouldActivateResource() {
            resource.setActive(false);

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(resourceRepository.save(any())).thenReturn(resource);

            resourceService.updateActive(resource.getId(), new UpdateActiveRequest(true), providerUser);

            assertThat(resource.getActive()).isTrue();
            verify(resourceRepository).save(resource);
        }
    }

    @Nested
    @DisplayName("setAvailabilityRules()")
    class SetAvailabilityRules {

        @Test
        @DisplayName("deve salvar regras de disponibilidade com sucesso")
        void shouldSaveAvailabilityRulesSuccessfully() {
            List<AvailabilityRuleRequest> rules = List.of(
                    new AvailabilityRuleRequest(1, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                    new AvailabilityRuleRequest(2, LocalTime.of(8, 0), LocalTime.of(20, 0))
            );

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<AvailabilityRuleResponse> responses = resourceService.setAvailabilityRules(
                    resource.getId(), rules, providerUser
            );

            assertThat(responses).hasSize(2);
            verify(availabilityRuleRepository).deleteByResourceId(resource.getId());
            verify(availabilityRuleRepository).saveAll(any());
        }

        @Test
        @DisplayName("deve lançar exceção para dias duplicados")
        void shouldThrowWhenDuplicateDays() {
            List<AvailabilityRuleRequest> rules = List.of(
                    new AvailabilityRuleRequest(1, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    new AvailabilityRuleRequest(1, LocalTime.of(14, 0), LocalTime.of(20, 0))
            );

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));

            assertThatThrownBy(() -> resourceService.setAvailabilityRules(
                    resource.getId(), rules, providerUser))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Não é permitido cadastrar o mesmo dia da semana mais de uma vez");
        }

        @Test
        @DisplayName("deve lançar exceção quando startTime é após endTime")
        void shouldThrowWhenStartTimeAfterEndTime() {
            List<AvailabilityRuleRequest> rules = List.of(
                    new AvailabilityRuleRequest(1, LocalTime.of(20, 0), LocalTime.of(8, 0))
            );

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));

            assertThatThrownBy(() -> resourceService.setAvailabilityRules(
                    resource.getId(), rules, providerUser))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Horário de início deve ser anterior ao horário de fim");
        }

        @Test
        @DisplayName("deve substituir regras existentes")
        void shouldReplaceExistingRules() {
            List<AvailabilityRuleRequest> rules = List.of(
                    new AvailabilityRuleRequest(3, LocalTime.of(9, 0), LocalTime.of(18, 0))
            );

            when(providerService.getEntityByUser(providerUser)).thenReturn(provider);
            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            resourceService.setAvailabilityRules(resource.getId(), rules, providerUser);

            verify(availabilityRuleRepository).deleteByResourceId(resource.getId());
        }
    }
}