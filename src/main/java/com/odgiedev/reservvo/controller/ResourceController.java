package com.odgiedev.reservvo.controller;

import com.odgiedev.reservvo.dto.request.AvailabilityRuleRequest;
import com.odgiedev.reservvo.dto.request.ResourceRequest;
import com.odgiedev.reservvo.dto.response.AvailabilityRuleResponse;
import com.odgiedev.reservvo.dto.response.ResourceResponse;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(request, user));
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resourceService.listByProvider(user));
    }

    @PutMapping("/{resourceId}")
    public ResponseEntity<ResourceResponse> update(
            @PathVariable UUID resourceId,
            @Valid @RequestBody ResourceRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resourceService.update(resourceId, request, user));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<ResourceResponse> deactivate(
            @PathVariable UUID resourceId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(resourceService.deactivate(resourceId, user));
    }

    @PutMapping("/{resourceId}/availability")
    public ResponseEntity<List<AvailabilityRuleResponse>> setAvailability(
            @PathVariable UUID resourceId,
            @Valid @RequestBody List<AvailabilityRuleRequest> rules,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resourceService.setAvailabilityRules(resourceId, rules, user));
    }

    @GetMapping("/{resourceId}/availability")
    public ResponseEntity<List<AvailabilityRuleResponse>> getAvailability(
            @PathVariable UUID resourceId) {
        return ResponseEntity.ok(resourceService.getAvailabilityRules(resourceId));
    }
}