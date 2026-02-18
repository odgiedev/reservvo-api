package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.ReservationRequest;
import com.odgiedev.reservvo.dto.response.ReservationResponse;
import com.odgiedev.reservvo.entity.AvailabilityRule;
import com.odgiedev.reservvo.entity.Reservation;
import com.odgiedev.reservvo.entity.Resource;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.enums.ReservationStatus;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.AvailabilityRuleRepository;
import com.odgiedev.reservvo.repository.ReservationRepository;
import com.odgiedev.reservvo.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final NotificationService notificationService;

    public ReservationResponse create(ReservationRequest request, User client) {
        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new BusinessException("Recurso não encontrado"));

        if (!resource.getActive()) {
            throw new BusinessException("Recurso indisponível");
        }

        // valida se o dia da semana está disponível
        int dayOfWeek = request.date().getDayOfWeek().getValue() % 7;

        boolean dayAvailable = availabilityRuleRepository
                .findByResourceId(resource.getId())
                .stream()
                .anyMatch(rule -> rule.getDayOfWeek() == dayOfWeek);

        if (!dayAvailable) {
            throw new BusinessException("Recurso não disponível nesse dia da semana");
        }

        // calcula o endTime baseado no slotDurationMin do recurso
        LocalTime endTime = request.startTime().plusMinutes(resource.getSlotDurationMin());

        // valida se o horário está dentro da disponibilidade
        boolean timeAvailable = availabilityRuleRepository
                .findByResourceId(resource.getId())
                .stream()
                .filter(rule -> rule.getDayOfWeek() == dayOfWeek)
                .anyMatch(rule ->
                        !request.startTime().isBefore(rule.getStartTime()) &&
                                !endTime.isAfter(rule.getEndTime())
                );

        if (!timeAvailable) {
            throw new BusinessException("Horário fora do período disponível");
        }

        // valida conflito com outras reservas
        if (reservationRepository.existsConflict(resource.getId(), request.date(), request.startTime(), endTime)) {
            throw new BusinessException("Horário já reservado");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .client(client)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(endTime)
                .notes(request.notes())
                .build();

        reservationRepository.save(reservation);
        //notificationService.sendConfirmation(reservation);

        return toResponse(reservation);
    }

    public List<ReservationResponse> listByClient(User client) {
        return reservationRepository
                .findByClientIdOrderByDateDescStartTimeDesc(client.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<ReservationResponse> listByProvider(User providerUser) {
        return reservationRepository
                .findByResourceProviderUserIdOrderByDateDescStartTimeDesc(providerUser.getId())
                .stream().map(this::toResponse).toList();
    }

    public ReservationResponse cancelByClient(UUID reservationId, User client) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva não encontrada"));

        if (!reservation.getClient().getId().equals(client.getId())) {
            throw new BusinessException("Você não tem permissão para cancelar essa reserva");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException("Apenas reservas confirmadas podem ser canceladas");
        }

        if (reservation.getDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível cancelar uma reserva passada");
        }

        reservation.setStatus(ReservationStatus.CANCELLED_BY_CLIENT);
        reservationRepository.save(reservation);
        //notificationService.sendCancellation(reservation);

        return toResponse(reservation);
    }

    public ReservationResponse cancelByProvider(UUID reservationId, User providerUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva não encontrada"));

        if (!reservation.getResource().getProvider().getUser().getId().equals(providerUser.getId())) {
            throw new BusinessException("Você não tem permissão para cancelar essa reserva");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException("Apenas reservas confirmadas podem ser canceladas");
        }

        reservation.setStatus(ReservationStatus.CANCELLED_BY_PROVIDER);
        reservationRepository.save(reservation);
        //notificationService.sendCancellation(reservation);

        return toResponse(reservation);
    }

    public List<LocalTime> getAvailableSlots(UUID resourceId, LocalDate date) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException("Recurso não encontrado"));

        int dayOfWeek = date.getDayOfWeek().getValue() % 7;

        return availabilityRuleRepository.findByResourceId(resourceId)
                .stream()
                .filter(rule -> rule.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .map(rule -> {
                    System.out.println(rule.getId());
                    List<LocalTime> slots = new ArrayList<>();
                    LocalTime current = rule.getStartTime(); //8
                    while (!current.plusMinutes(resource.getSlotDurationMin()).isAfter(rule.getEndTime())) {
                        LocalTime slotEnd = current.plusMinutes(resource.getSlotDurationMin());
                        boolean conflict = reservationRepository.existsConflict(
                                resourceId, date, current, slotEnd);
                        if (!conflict) slots.add(current);
                        current = slotEnd;
                    }
                    return slots;
                })
                .orElse(List.of());
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getClient().getId(),
                reservation.getClient().getName(),
                reservation.getResource().getProvider().getBusinessName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getNotes(),
                reservation.getCreatedAt()
        );
    }
}