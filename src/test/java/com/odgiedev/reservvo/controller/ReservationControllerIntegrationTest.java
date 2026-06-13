package com.odgiedev.reservvo.controller;

import com.odgiedev.reservvo.BaseIntegrationTest;
import com.odgiedev.reservvo.entity.*;
import com.odgiedev.reservvo.enums.ReservationStatus;
import com.odgiedev.reservvo.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ReservationController")
@EnableAsync
@Transactional
@Rollback
@Sql(
        statements = "DELETE FROM reservations",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class ReservationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProviderRepository providerRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private AvailabilityRuleRepository availabilityRuleRepository;
    @Autowired private ReservationRepository reservationRepository;

    private Provider provider;
    private Resource resource;

    @BeforeEach
    void setUpReservation() {
        provider = providerRepository.save(Provider.builder()
                .user(providerUser)
                .businessName("Barbearia Teste")
                .slug("barbearia-teste")
                .category("Barbearia")
                .build());

        resource = resourceRepository.save(Resource.builder()
                .provider(provider)
                .name("Cadeira 1")
                .slotDurationMin(60)
                .active(true)
                .build());

        for (int day = 0; day <= 6; day++) {
            availabilityRuleRepository.save(AvailabilityRule.builder()
                    .resource(resource)
                    .dayOfWeek(day)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(20, 0))
                    .build());
        }
    }

    private LocalDate futureDate() {
        return LocalDate.now().plusDays(7);
    }

    private Reservation saveConfirmed(LocalTime start, LocalTime end) {
        return reservationRepository.save(Reservation.builder()
                .resource(resource)
                .client(clientUser)
                .date(futureDate())
                .startTime(start)
                .endTime(end)
                .status(ReservationStatus.CONFIRMED)
                .build());
    }

    @Nested
    @Transactional
    @DisplayName("POST /api/reservations")
    class CreateReservation {

        @Test
        @DisplayName("deve criar reserva e retornar 201")
        void shouldCreateReservationAndReturn201() throws Exception {
            String body = """
                    {
                        "resourceId": "%s",
                        "date": "%s",
                        "startTime": "09:00",
                        "notes": "Quero degradê"
                    }
                    """.formatted(resource.getId(), futureDate());

            mockMvc.perform(post("/api/reservations")
                            .header("Authorization", bearerToken(clientToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.startTime").value("09:00:00"))
                    .andExpect(jsonPath("$.endTime").value("10:00:00"))
                    .andExpect(jsonPath("$.notes").value("Quero degradê"));
        }

        @Test
        @DisplayName("deve retornar 400 quando horário já reservado")
        void shouldReturn400WhenConflict() throws Exception {
            String body = """
                    {
                        "resourceId": "%s",
                        "date": "%s",
                        "startTime": "09:00"
                    }
                    """.formatted(resource.getId(), futureDate());

            mockMvc.perform(post("/api/reservations")
                            .header("Authorization", bearerToken(clientToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/reservations")
                            .header("Authorization", bearerToken(clientToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Horário já reservado"));
        }

        @Test
        @DisplayName("deve retornar 401 quando não autenticado")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            String body = """
                    {
                        "resourceId": "%s",
                        "date": "%s",
                        "startTime": "09:00"
                    }
                    """.formatted(resource.getId(), futureDate());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando data no passado")
        void shouldReturn400WhenDateInPast() throws Exception {
            String body = """
                    {
                        "resourceId": "%s",
                        "date": "%s",
                        "startTime": "09:00"
                    }
                    """.formatted(resource.getId(), LocalDate.now().minusDays(1));

            mockMvc.perform(post("/api/reservations")
                            .header("Authorization", bearerToken(clientToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @Transactional
    @DisplayName("GET /api/reservations/client")
    class ListByClient {

        @Test
        @DisplayName("deve listar reservas do cliente")
        void shouldListClientReservations() throws Exception {
            saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));

            mockMvc.perform(get("/api/reservations/client")
                            .header("Authorization", bearerToken(clientToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].clientId").value(clientUser.getId().toString()));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há reservas")
        void shouldReturnEmptyListWhenNoReservations() throws Exception {
            mockMvc.perform(get("/api/reservations/client")
                            .header("Authorization", bearerToken(clientToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("deve retornar última página corretamente (3 itens, size=2, page=1)")
        void shouldReturnLastPage() throws Exception {
            saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));
            saveConfirmed(LocalTime.of(10, 0), LocalTime.of(11, 0));
            saveConfirmed(LocalTime.of(11, 0), LocalTime.of(12, 0));

            mockMvc.perform(get("/api/reservations/client")
                            .header("Authorization", bearerToken(clientToken))
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("deve filtrar por status CONFIRMED")
        void shouldFilterByStatus() throws Exception {
            saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));
            reservationRepository.save(Reservation.builder()
                    .resource(resource)
                    .client(clientUser)
                    .date(futureDate())
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .status(ReservationStatus.CANCELLED_BY_CLIENT)
                    .build());

            mockMvc.perform(get("/api/reservations/client")
                            .header("Authorization", bearerToken(clientToken))
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("sem filtro de status retorna todos")
        void shouldReturnAllStatusesWithoutFilter() throws Exception {
            saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));
            reservationRepository.save(Reservation.builder()
                    .resource(resource)
                    .client(clientUser)
                    .date(futureDate())
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .status(ReservationStatus.CANCELLED_BY_CLIENT)
                    .build());

            mockMvc.perform(get("/api/reservations/client")
                            .header("Authorization", bearerToken(clientToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    @Nested
    @Transactional
    @DisplayName("GET /api/reservations/stats")
    class Stats {

        @Test
        @DisplayName("deve retornar estatísticas do cliente")
        void shouldReturnClientStats() throws Exception {
            saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));
            saveConfirmed(LocalTime.of(10, 0), LocalTime.of(11, 0));
            reservationRepository.save(Reservation.builder()
                    .resource(resource)
                    .client(clientUser)
                    .date(futureDate())
                    .startTime(LocalTime.of(11, 0))
                    .endTime(LocalTime.of(12, 0))
                    .status(ReservationStatus.CANCELLED_BY_CLIENT)
                    .build());

            mockMvc.perform(get("/api/reservations/stats")
                            .header("Authorization", bearerToken(clientToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.confirmed").value(2))
                    .andExpect(jsonPath("$.cancelledByClient").value(1))
                    .andExpect(jsonPath("$.completed").value(0))
                    .andExpect(jsonPath("$.total").value(3));
        }

        @Test
        @DisplayName("deve retornar estatísticas do provider")
        void shouldReturnProviderStats() throws Exception {
            saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));
            reservationRepository.save(Reservation.builder()
                    .resource(resource)
                    .client(clientUser)
                    .date(futureDate())
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .status(ReservationStatus.COMPLETED)
                    .build());

            mockMvc.perform(get("/api/reservations/stats")
                            .header("Authorization", bearerToken(providerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.confirmed").value(1))
                    .andExpect(jsonPath("$.completed").value(1))
                    .andExpect(jsonPath("$.total").value(2));
        }

        @Test
        @DisplayName("deve retornar zeros quando não há reservas")
        void shouldReturnZerosWhenNoReservations() throws Exception {
            mockMvc.perform(get("/api/reservations/stats")
                            .header("Authorization", bearerToken(clientToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.confirmed").value(0))
                    .andExpect(jsonPath("$.total").value(0));
        }
    }

    @Nested
    @Transactional
    @DisplayName("PATCH /api/reservations/{id}/cancel/client")
    class CancelByClient {

        @Test
        @DisplayName("deve cancelar reserva e retornar status CANCELLED_BY_CLIENT")
        void shouldCancelReservationSuccessfully() throws Exception {
            Reservation reservation = saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));

            mockMvc.perform(patch("/api/reservations/{id}/cancel/client", reservation.getId())
                            .header("Authorization", bearerToken(clientToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED_BY_CLIENT"));
        }

        @Test
        @DisplayName("deve retornar 400 quando outro cliente tenta cancelar")
        void shouldReturn400WhenWrongClient() throws Exception {
            Reservation reservation = saveConfirmed(LocalTime.of(9, 0), LocalTime.of(10, 0));

            mockMvc.perform(patch("/api/reservations/{id}/cancel/client", reservation.getId())
                            .header("Authorization", bearerToken(providerToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Você não tem permissão para cancelar essa reserva"));
        }
    }

    @Nested
    @Transactional
    @DisplayName("GET /api/reservations/slots")
    class GetSlots {

        @Test
        @DisplayName("deve retornar slots disponíveis")
        void shouldReturnAvailableSlots() throws Exception {
            mockMvc.perform(get("/api/reservations/slots")
                            .header("Authorization", bearerToken(clientToken))
                            .param("resourceId", resource.getId().toString())
                            .param("date", futureDate().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(12));
        }

        @Test
        @DisplayName("deve retornar 400 quando resourceId inválido")
        void shouldReturn400WhenInvalidResourceId() throws Exception {
            mockMvc.perform(get("/api/reservations/slots")
                            .header("Authorization", bearerToken(clientToken))
                            .param("resourceId", "id-invalido")
                            .param("date", futureDate().toString()))
                    .andExpect(status().isBadRequest());
        }
    }
}
