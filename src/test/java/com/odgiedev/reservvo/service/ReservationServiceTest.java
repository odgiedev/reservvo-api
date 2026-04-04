package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.ReservationRequest;
import com.odgiedev.reservvo.entity.*;
import com.odgiedev.reservvo.enums.ReservationStatus;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.AvailabilityRuleRepository;
import com.odgiedev.reservvo.repository.ReservationRepository;
import com.odgiedev.reservvo.repository.ResourceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService")
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private AvailabilityRuleRepository availabilityRuleRepository;
    @Mock private NotificationService notificationService;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private ReservationService reservationService;

    private User client;
    private User providerUser;
    private Provider provider;
    private Resource resource;
    private AvailabilityRule mondayRule;

    @BeforeEach
    void setUp() {
        providerUser = User.builder()
                .id(UUID.randomUUID())
                .name("Prestador")
                .email("prestador@email.com")
                .passwordHash("hash")
                .role(UserRole.PROVIDER)
                .build();

        client = User.builder()
                .id(UUID.randomUUID())
                .name("Cliente")
                .email("cliente@email.com")
                .passwordHash("hash")
                .role(UserRole.CLIENT)
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
                .slotDurationMin(60)
                .active(true)
                .build();

        // segunda-feira (1) das 08:00 às 20:00
        mondayRule = AvailabilityRule.builder()
                .id(UUID.randomUUID())
                .resource(resource)
                .dayOfWeek(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(20, 0))
                .build();
    }

    // segunda-feira futura para os testes
    private LocalDate nextMonday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek().getValue() != 1) {
            date = date.plusDays(1);
        }
        return date;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar reserva com sucesso")
        void shouldCreateReservationSuccessfully() {
            LocalDate date = nextMonday();
            ReservationRequest request = new ReservationRequest(
                    resource.getId(), date, LocalTime.of(9, 0), "Quero degradê"
            );

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));
            when(reservationRepository.existsConflict(any(), any(), any(), any())).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.prePersist();
                return r;
            });

            var response = reservationService.create(request, client);

            assertThat(response).isNotNull();
            assertThat(response.resourceId()).isEqualTo(resource.getId());
            assertThat(response.clientId()).isEqualTo(client.getId());
            assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(response.endTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);

            verify(reservationRepository).save(any());
            verify(notificationService).sendConfirmation(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando recurso não encontrado")
        void shouldThrowWhenResourceNotFound() {
            ReservationRequest request = new ReservationRequest(
                    UUID.randomUUID(), nextMonday(), LocalTime.of(9, 0), null
            );

            when(resourceRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.create(request, client))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Recurso não encontrado");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando recurso está inativo")
        void shouldThrowWhenResourceInactive() {
            resource.setActive(false);
            ReservationRequest request = new ReservationRequest(
                    resource.getId(), nextMonday(), LocalTime.of(9, 0), null
            );

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));

            assertThatThrownBy(() -> reservationService.create(request, client))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Recurso indisponível");
        }

        @Test
        @DisplayName("deve lançar exceção quando dia da semana não disponível")
        void shouldThrowWhenDayNotAvailable() {

            LocalDate sunday = LocalDate.now().plusDays(1);
            while (sunday.getDayOfWeek().getValue() % 7 != 0) {
                sunday = sunday.plusDays(1);
            }

            ReservationRequest request = new ReservationRequest(
                    resource.getId(), sunday, LocalTime.of(9, 0), null
            );

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));

            assertThatThrownBy(() -> reservationService.create(request, client))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Recurso não disponível nesse dia da semana");
        }

        @Test
        @DisplayName("deve lançar exceção quando horário fora do período disponível")
        void shouldThrowWhenTimeOutOfRange() {
            LocalDate date = nextMonday();

            ReservationRequest request = new ReservationRequest(
                    resource.getId(), date, LocalTime.of(7, 0), null
            );

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));

            assertThatThrownBy(() -> reservationService.create(request, client))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Horário fora do período disponível");
        }

        @Test
        @DisplayName("deve lançar exceção quando horário já reservado")
        void shouldThrowWhenConflictExists() {
            LocalDate date = nextMonday();
            ReservationRequest request = new ReservationRequest(
                    resource.getId(), date, LocalTime.of(9, 0), null
            );

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));
            when(reservationRepository.existsConflict(any(), any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> reservationService.create(request, client))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Horário já reservado");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve calcular endTime corretamente baseado no slotDurationMin")
        void shouldCalculateEndTimeCorrectly() {
            resource.setSlotDurationMin(60);
            LocalDate date = nextMonday();
            ReservationRequest request = new ReservationRequest(
                    resource.getId(), date, LocalTime.of(9, 0), null
            );

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));
            when(reservationRepository.existsConflict(any(), any(), any(), any())).thenReturn(false);
            when(reservationRepository.save(any())).thenAnswer(inv -> {
                Reservation r = inv.getArgument(0);
                r.prePersist();
                return r;
            });

            var response = reservationService.create(request, client);

            assertThat(response.endTime()).isEqualTo(LocalTime.of(10, 0));
        }
    }

    @Nested
    @DisplayName("cancelByClient()")
    class CancelByClient {

        private Reservation confirmedReservation;

        @BeforeEach
        void setUp() {
            confirmedReservation = Reservation.builder()
                    .id(UUID.randomUUID())
                    .resource(resource)
                    .client(client)
                    .date(LocalDate.now().plusDays(3))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 0))
                    .status(ReservationStatus.CONFIRMED)
                    .build();
        }

        @Test
        @DisplayName("deve cancelar reserva com sucesso")
        void shouldCancelSuccessfully() {
            when(reservationRepository.findById(confirmedReservation.getId()))
                    .thenReturn(Optional.of(confirmedReservation));
            when(reservationRepository.save(any())).thenReturn(confirmedReservation);
            when(cacheManager.getCache("slots")).thenReturn(cache);

            var response = reservationService.cancelByClient(confirmedReservation.getId(), client);

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED_BY_CLIENT);
            verify(notificationService).sendCancellation(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando reserva não pertence ao cliente")
        void shouldThrowWhenReservationNotOwnedByClient() {
            User anotherClient = User.builder()
                    .id(UUID.randomUUID())
                    .name("Outro Cliente")
                    .email("outro@email.com")
                    .role(UserRole.CLIENT)
                    .build();

            when(reservationRepository.findById(confirmedReservation.getId()))
                    .thenReturn(Optional.of(confirmedReservation));

            assertThatThrownBy(() -> reservationService.cancelByClient(confirmedReservation.getId(), anotherClient))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Você não tem permissão para cancelar essa reserva");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando reserva já cancelada")
        void shouldThrowWhenAlreadyCancelled() {
            confirmedReservation.setStatus(ReservationStatus.CANCELLED_BY_CLIENT);

            when(reservationRepository.findById(confirmedReservation.getId()))
                    .thenReturn(Optional.of(confirmedReservation));

            assertThatThrownBy(() -> reservationService.cancelByClient(confirmedReservation.getId(), client))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Apenas reservas confirmadas podem ser canceladas");
        }

        @Test
        @DisplayName("deve lançar exceção quando reserva já passou")
        void shouldThrowWhenReservationInThePast() {
            confirmedReservation.setDate(LocalDate.now().minusDays(1));

            when(reservationRepository.findById(confirmedReservation.getId()))
                    .thenReturn(Optional.of(confirmedReservation));

            assertThatThrownBy(() -> reservationService.cancelByClient(confirmedReservation.getId(), client))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Não é possível cancelar uma reserva passada");
        }
    }

    @Nested
    @DisplayName("cancelByProvider()")
    class CancelByProvider {

        private Reservation confirmedReservation;

        @BeforeEach
        void setUp() {
            confirmedReservation = Reservation.builder()
                    .id(UUID.randomUUID())
                    .resource(resource)
                    .client(client)
                    .date(LocalDate.now().plusDays(3))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 0))
                    .status(ReservationStatus.CONFIRMED)
                    .build();
        }

        @Test
        @DisplayName("deve cancelar reserva pelo prestador com sucesso")
        void shouldCancelSuccessfully() {
            when(reservationRepository.findById(confirmedReservation.getId()))
                    .thenReturn(Optional.of(confirmedReservation));
            when(reservationRepository.save(any())).thenReturn(confirmedReservation);
            when(cacheManager.getCache("slots")).thenReturn(cache);

            var response = reservationService.cancelByProvider(confirmedReservation.getId(), providerUser);

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED_BY_PROVIDER);
            verify(notificationService).sendCancellation(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando prestador não é dono do recurso")
        void shouldThrowWhenNotResourceOwner() {
            User anotherProvider = User.builder()
                    .id(UUID.randomUUID())
                    .name("Outro Prestador")
                    .email("outro@email.com")
                    .role(UserRole.PROVIDER)
                    .build();

            when(reservationRepository.findById(confirmedReservation.getId()))
                    .thenReturn(Optional.of(confirmedReservation));

            assertThatThrownBy(() -> reservationService.cancelByProvider(confirmedReservation.getId(), anotherProvider))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Você não tem permissão para cancelar essa reserva");
        }
    }

    @Nested
    @DisplayName("getAvailableSlots()")
    class GetAvailableSlots {

        @Test
        @DisplayName("deve retornar todos os slots quando não há reservas")
        void shouldReturnAllSlotsWhenNoReservations() {
            LocalDate date = nextMonday();

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));
            when(reservationRepository.existsConflict(any(), any(), any(), any())).thenReturn(false);

            var slots = reservationService.getAvailableSlots(resource.getId(), date);

            // 08:00 até 20:00 com slots de 60min = 12 slots
            assertThat(slots).hasSize(12);
            assertThat(slots.getFirst()).isEqualTo(LocalTime.of(8, 0));
            assertThat(slots.get(11)).isEqualTo(LocalTime.of(19, 0));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando dia não disponível")
        void shouldReturnEmptyWhenDayNotAvailable() {
            LocalDate sunday = LocalDate.now().plusDays(1);
            while (sunday.getDayOfWeek().getValue() % 7 != 0) {
                sunday = sunday.plusDays(1);
            }

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));

            var slots = reservationService.getAvailableSlots(resource.getId(), sunday);

            assertThat(slots).isEmpty();
        }

        @Test
        @DisplayName("deve excluir slots já reservados")
        void shouldExcludeOccupiedSlots() {
            LocalDate date = nextMonday();

            when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
            when(availabilityRuleRepository.findByResourceId(resource.getId())).thenReturn(List.of(mondayRule));

            when(reservationRepository.existsConflict(eq(resource.getId()), eq(date), any(), any()))
                    .thenAnswer(invocation -> {
                        LocalTime startTime = invocation.getArgument(2);
                        return startTime.equals(LocalTime.of(9, 0));
                    });

            var slots = reservationService.getAvailableSlots(resource.getId(), date);

            assertThat(slots).doesNotContain(LocalTime.of(9, 0));
            assertThat(slots).hasSize(11);
        }

        @Test
        @DisplayName("deve lançar exceção quando recurso não encontrado")
        void shouldThrowWhenResourceNotFound() {
            when(resourceRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getAvailableSlots(UUID.randomUUID(), nextMonday()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Recurso não encontrado");
        }
    }
}