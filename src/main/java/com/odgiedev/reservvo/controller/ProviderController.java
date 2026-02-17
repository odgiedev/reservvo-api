package com.odgiedev.reservvo.controller;

import com.odgiedev.reservvo.dto.request.ProviderRequest;
import com.odgiedev.reservvo.dto.response.ProviderResponse;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.service.ProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @PostMapping
    public ResponseEntity<ProviderResponse> create(
            @Valid @RequestBody ProviderRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<ProviderResponse> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(providerService.getByUser(user));
    }

    @PutMapping
    public ResponseEntity<ProviderResponse> update(
            @Valid @RequestBody ProviderRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(providerService.update(request, user));
    }
}