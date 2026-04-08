package com.odgiedev.reservvo.controller;

import com.odgiedev.reservvo.BaseIntegrationTest;
import com.odgiedev.reservvo.dto.request.LoginRequest;
import com.odgiedev.reservvo.dto.request.RegisterRequest;
import com.odgiedev.reservvo.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@DisplayName("AuthController")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("deve registrar usuário e retornar 201 com token")
        void shouldRegisterAndReturn201() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "Novo Usuário", "novo@test.com", "123456", UserRole.CLIENT, "17999999999"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("novo@test.com"))
                    .andExpect(jsonPath("$.role").value("CLIENT"));
        }

        @Test
        @DisplayName("deve retornar 400 quando email já cadastrado")
        void shouldReturn400WhenEmailExists() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "Cliente Teste", "cliente@test.com", "123456", UserRole.CLIENT, null
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email já cadastrado"));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            String body = """
                    {
                        "name": "",
                        "email": "invalido",
                        "password": "123"
                    }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        @DisplayName("deve retornar 400 quando role inválida")
        void shouldReturn400WhenInvalidRole() throws Exception {
            String body = """
                    {
                        "name": "Teste",
                        "email": "teste@test.com",
                        "password": "123456",
                        "role": "ADMIN"
                    }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("deve fazer login e retornar 200 com token")
        void shouldLoginAndReturn200() throws Exception {
            LoginRequest request = new LoginRequest("cliente@test.com", "123456");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.email").value("cliente@test.com"))
                    .andExpect(jsonPath("$.role").value("CLIENT"));
        }

        @Test
        @DisplayName("deve retornar 401 quando credenciais inválidas")
        void shouldReturn401WhenBadCredentials() throws Exception {
            LoginRequest request = new LoginRequest("cliente@test.com", "senha_errada");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando body inválido")
        void shouldReturn400WhenInvalidBody() throws Exception {
            String body = """
                    {
                        "email": "nao-é-email",
                        "password": ""
                    }
                    """;

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        @DisplayName("deve retornar 404 quando rota não existe")
        void shouldReturn404WhenRouteNotFound() throws Exception {
            mockMvc.perform(post("/api/auth/rota-inexistente")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}