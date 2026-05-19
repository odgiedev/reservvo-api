package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.LoginRequest;
import com.odgiedev.reservvo.dto.request.RegisterRequest;
import com.odgiedev.reservvo.dto.request.UpdateRoleRequest;
import com.odgiedev.reservvo.dto.response.AuthResponse;
import com.odgiedev.reservvo.dto.response.UpdateRoleResponse;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.UserRepository;
import com.odgiedev.reservvo.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.odgiedev.reservvo.util.StringUtils.capitalizeWords;
import static com.odgiedev.reservvo.util.StringUtils.normalizeEmail;
import static com.odgiedev.reservvo.util.StringUtils.normalizePhone;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ProviderService providerService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Tentativa de registro com email duplicado | email: {}", normalizedEmail);
            throw new BusinessException("Email já cadastrado");
        }

        User user = User.builder()
                .name(capitalizeWords(request.name()))
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .phone(normalizePhone(request.phone()))
                .build();

        userRepository.save(user);
        log.info("Usuário registrado | userId: {} | role: {}", user.getId(), user.getRole());

        if (requiresProvider(user.getRole())) {
            providerService.createDefaultForUser(user);
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        String token = jwtService.generateToken(user);
        log.info("Login bem-sucedido | userId: {}", user.getId());

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public UpdateRoleResponse updateRole(UpdateRoleRequest request, User user) {
        UserRole newRole = request.role();

        if (user.getRole() == newRole) {
            throw new BusinessException("Usuário já possui essa role");
        }

        UserRole oldRole = user.getRole();

        if (requiresProvider(newRole)) {
            providerService.createDefaultForUser(user);
        }

        user.setRole(newRole);
        userRepository.save(user);
        log.warn("Role alterada | userId: {} | de: {} | para: {}", user.getId(), oldRole, newRole);

        return new UpdateRoleResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private boolean requiresProvider(UserRole role) {
        return role == UserRole.PROVIDER || role == UserRole.BOTH;
    }
}
