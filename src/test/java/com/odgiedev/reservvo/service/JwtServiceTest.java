package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-that-is-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("hash")
                .role(UserRole.CLIENT)
                .build();
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("deve gerar token não nulo")
        void shouldGenerateNonNullToken() {
            String token = jwtService.generateToken(user);
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("deve gerar token com três partes separadas por ponto")
        void shouldGenerateTokenWithThreeParts() {
            String token = jwtService.generateToken(user);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("deve gerar tokens diferentes para o mesmo usuário")
        void shouldGenerateDifferentTokensForSameUser() throws InterruptedException {
            String token1 = jwtService.generateToken(user);
            Thread.sleep(1000);
            String token2 = jwtService.generateToken(user);
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmail {

        @Test
        @DisplayName("deve extrair email corretamente do token")
        void shouldExtractEmailFromToken() {
            String token = jwtService.generateToken(user);
            String email = jwtService.extractEmail(token);
            assertThat(email).isEqualTo(user.getEmail());
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("deve retornar true para token válido")
        void shouldReturnTrueForValidToken() {
            String token = jwtService.generateToken(user);
            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false para token de outro usuário")
        void shouldReturnFalseForDifferentUser() {
            User anotherUser = User.builder()
                    .id(UUID.randomUUID())
                    .name("Maria")
                    .email("maria@email.com")
                    .passwordHash("hash")
                    .role(UserRole.CLIENT)
                    .build();

            String token = jwtService.generateToken(user);
            assertThat(jwtService.isTokenValid(token, anotherUser)).isFalse();
        }

        @Test
        @DisplayName("deve lançar exceção para token expirado")
        void shouldThrowForExpiredToken() {
            ReflectionTestUtils.setField(jwtService, "expiration", 100L);

            String token = jwtService.generateToken(user);

            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

            assertThrows(ExpiredJwtException.class, () -> {
                jwtService.isTokenValid(token, user);
            });
        }

        @Test
        @DisplayName("deve lançar exceção para token malformado")
        void shouldThrowForMalformedToken() {
            assertThatThrownBy(() -> jwtService.extractEmail("token.invalido.aqui"))
                    .isInstanceOf(Exception.class);
        }
    }
}