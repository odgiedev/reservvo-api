package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.ReservationRequest;
import com.odgiedev.reservvo.dto.response.PageResponse;
import com.odgiedev.reservvo.dto.response.ReservationResponse;
import com.odgiedev.reservvo.dto.response.ReservationStatsResponse;
import com.odgiedev.reservvo.entity.Reservation;
import com.odgiedev.reservvo.entity.Resource;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.enums.ReservationStatus;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.AvailabilityRuleRepository;
import com.odgiedev.reservvo.repository.ReservationRepository;
import com.odgiedev.reservvo.repository.ReservationRepository.StatusCount;
import com.odgiedev.reservvo.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final NotificationService notificationService;
    private final CacheManager cacheManager;

    @CacheEvict(value = "slots", key = "#request.resourceId() + '_' + #request.date()")
    public ReservationResponse create(ReservationRequest request, User client) {
        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new BusinessException("Recurso não encontrado"));

        if (!resource.getActive()) {
            throw new BusinessException("Recurso indisponível");
        }

        if (client.getRole() == UserRole.PROVIDER) {
            throw new BusinessException("Usuario PROVIDER não reserva");
        }

        if (request.date().equals(LocalDate.now()) && !request.startTime().isAfter(LocalTime.now())) {
            throw new BusinessException("Horário deve ser no futuro");
        }

        int dayOfWeek = request.date().getDayOfWeek().getValue() % 7;

        boolean dayAvailable = availabilityRuleRepository
                .findByResourceId(resource.getId())
                .stream()
                .anyMatch(rule -> rule.getDayOfWeek() == dayOfWeek);

        if (!dayAvailable) {
            throw new BusinessException("Recurso não disponível nesse dia da semana");
        }

        LocalTime endTime = request.startTime().plusMinutes(resource.getSlotDurationMin());

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
        log.info("Reserva criada | reservationId: {} | clientId: {} | resourceId: {} | data: {} {}",
                reservation.getId(), client.getId(), resource.getId(),
                reservation.getDate(), reservation.getStartTime());
        notificationService.sendConfirmation(reservation);

        return toResponse(reservation);
    }

    public PageResponse<ReservationResponse> listByClient(User client, int page, int size,
                                                           ReservationStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        var pageResult = status != null
                ? reservationRepository.findByClientIdAndStatus(client.getId(), status, pageable)
                : reservationRepository.findByClientId(client.getId(), pageable);
        return PageResponse.of(pageResult, this::toResponse);
    }

    public PageResponse<ReservationResponse> listByProvider(User providerUser, int page, int size,
                                                             ReservationStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        var pageResult = status != null
                ? reservationRepository.findByResourceProviderUserIdAndStatus(providerUser.getId(), status, pageable)
                : reservationRepository.findByResourceProviderUserId(providerUser.getId(), pageable);
        return PageResponse.of(pageResult, this::toResponse);
    }

    public ReservationStatsResponse stats(User user) {
        List<StatusCount> counts = user.getRole() == UserRole.CLIENT
                ? reservationRepository.countByStatusForClient(user.getId())
                : reservationRepository.countByStatusForProvider(user.getId());
        return buildStats(counts);
    }

    private ReservationStatsResponse buildStats(List<StatusCount> counts) {
        long confirmed = 0, completed = 0, cancelledByProvider = 0, cancelledByClient = 0;
        for (StatusCount sc : counts) {
            switch (sc.getStatus()) {
                case CONFIRMED -> confirmed = sc.getCount();
                case COMPLETED -> completed = sc.getCount();
                case CANCELLED_BY_PROVIDER -> cancelledByProvider = sc.getCount();
                case CANCELLED_BY_CLIENT -> cancelledByClient = sc.getCount();
            }
        }
        return new ReservationStatsResponse(confirmed, completed, cancelledByProvider, cancelledByClient,
                confirmed + completed + cancelledByProvider + cancelledByClient);
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
        log.info("Reserva cancelada pelo cliente | reservationId: {} | clientId: {}",
                reservationId, client.getId());

        notificationService.sendCancellation(reservation);

        evictSlotsCache(reservation);
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
        log.info("Reserva cancelada pelo provider | reservationId: {} | providerUserId: {}",
                reservationId, providerUser.getId());

        notificationService.sendCancellation(reservation);

        evictSlotsCache(reservation);
        return toResponse(reservation);
    }

    @Cacheable(value = "slots", key = "#resourceId + '_' + #date")
    public List<LocalTime> getAvailableSlots(UUID resourceId, LocalDate date) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException("Recurso não encontrado"));

        int dayOfWeek = date.getDayOfWeek().getValue() % 7;

        return availabilityRuleRepository.findByResourceId(resourceId)
                .stream()
                .filter(rule -> rule.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .map(rule -> {
                    List<LocalTime> slots = new ArrayList<>();

                    LocalTime current = rule.getStartTime();

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
                reservation.getClient().getPhone(),
                reservation.getResource().getProvider().getBusinessName(),
                reservation.getResource().getProvider().getPhone(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getNotes(),
                reservation.getCreatedAt()
        );
    }

    private void evictSlotsCache(Reservation reservation) {
        String cacheKey = reservation.getResource().getId() + "_" + reservation.getDate();
        Objects.requireNonNull(cacheManager.getCache("slots")).evict(cacheKey);
        log.debug("Cache 'slots' evicted | key: {}", cacheKey);
    }
}