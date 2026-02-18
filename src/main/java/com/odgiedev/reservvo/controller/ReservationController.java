package com.odgiedev.reservvo.controller;

import com.odgiedev.reservvo.dto.request.ReservationRequest;
import com.odgiedev.reservvo.dto.response.ReservationResponse;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, user));
    }

    @GetMapping("/client")
    public ResponseEntity<List<ReservationResponse>> listByClient(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.listByClient(user));
    }

    @GetMapping("/provider")
    public ResponseEntity<List<ReservationResponse>> listByProvider(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.listByProvider(user));
    }

    @PatchMapping("/{reservationId}/cancel/client")
    public ResponseEntity<ReservationResponse> cancelByClient(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.cancelByClient(reservationId, user));
    }

    @PatchMapping("/{reservationId}/cancel/provider")
    public ResponseEntity<ReservationResponse> cancelByProvider(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.cancelByProvider(reservationId, user));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reservationService.getAvailableSlots(resourceId, date));
    }
}