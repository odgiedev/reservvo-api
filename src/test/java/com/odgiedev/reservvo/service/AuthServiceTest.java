package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.LoginRequest;
import com.odgiedev.reservvo.dto.request.RegisterRequest;
import com.odgiedev.reservvo.dto.response.AuthResponse;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.UserRepository;
import com.odgiedev.reservvo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(UUID.randomUUID())
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("hashed_password")
                .role(UserRole.CLIENT)
                .phone("17999999999")
                .build();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("deve registrar usuário com sucesso")
        void shouldRegisterUserSuccessfully() {
            RegisterRequest request = new RegisterRequest(
                    "João Silva", "joao@email.com", "123456", UserRole.CLIENT, "17999999999"
            );

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.prePersist();
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("jwt_token");

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt_token");
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.name()).isEqualTo(request.name());
            assertThat(response.role()).isEqualTo(UserRole.CLIENT);

            verify(userRepository).save(any());
            verify(passwordEncoder).encode("123456");
            verify(jwtService).generateToken(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando email já cadastrado")
        void shouldThrowWhenEmailAlreadyExists() {
            RegisterRequest request = new RegisterRequest(
                    "João Silva", "joao@email.com", "123456", UserRole.CLIENT, null
            );

            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Email já cadastrado");

            verify(userRepository, never()).save(any());
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("deve hashear a senha antes de salvar")
        void shouldHashPasswordBeforeSaving() {
            RegisterRequest request = new RegisterRequest(
                    "João Silva", "joao@email.com", "123456", UserRole.CLIENT, null
            );

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode("123456")).thenReturn("hashed_123456");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.prePersist();
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("token");

            authService.register(request);

            verify(passwordEncoder).encode("123456");

            verify(userRepository).save(argThat(u ->
                    u.getPasswordHash().equals("hashed_123456") &&
                            !u.getPasswordHash().equals("123456")
            ));
        }

        @Test
        @DisplayName("deve registrar usuário com role PROVIDER")
        void shouldRegisterProviderRole() {
            RegisterRequest request = new RegisterRequest(
                    "Maria", "maria@email.com", "123456", UserRole.PROVIDER, null
            );

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.prePersist();
                return u;
            });
            when(jwtService.generateToken(any())).thenReturn("token");

            AuthResponse response = authService.register(request);

            assertThat(response.role()).isEqualTo(UserRole.PROVIDER);
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("deve fazer login com sucesso")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest("joao@email.com", "123456");

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(existingUser)).thenReturn("jwt_token");

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt_token");
            assertThat(response.email()).isEqualTo(existingUser.getEmail());
            assertThat(response.userId()).isEqualTo(existingUser.getId());

            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
            );
        }

        @Test
        @DisplayName("deve lançar exceção quando credenciais inválidas")
        void shouldThrowWhenBadCredentials() {
            LoginRequest request = new LoginRequest("joao@email.com", "senha_errada");

            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando usuário não encontrado após autenticação")
        void shouldThrowWhenUserNotFoundAfterAuth() {
            LoginRequest request = new LoginRequest("joao@email.com", "123456");

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Usuário não encontrado");
        }

        @Test
        @DisplayName("deve retornar role correta no response")
        void shouldReturnCorrectRole() {
            LoginRequest request = new LoginRequest("joao@email.com", "123456");

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(existingUser)).thenReturn("token");

            AuthResponse response = authService.login(request);

            assertThat(response.role()).isEqualTo(UserRole.CLIENT);
        }
    }
}