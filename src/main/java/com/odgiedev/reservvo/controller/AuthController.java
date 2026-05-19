package com.odgiedev.reservvo.controller;

import com.odgiedev.reservvo.dto.request.LoginRequest;
import com.odgiedev.reservvo.dto.request.RegisterRequest;
import com.odgiedev.reservvo.dto.request.UpdateRoleRequest;
import com.odgiedev.reservvo.dto.response.AuthResponse;
import com.odgiedev.reservvo.dto.response.UpdateRoleResponse;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PatchMapping("/role")
    public ResponseEntity<UpdateRoleResponse> updateRole(@Valid @RequestBody UpdateRoleRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.updateRole(request, user));
    }
}