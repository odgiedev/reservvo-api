package com.odgiedev.reservvo;

import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.enums.UserRole;
import com.odgiedev.reservvo.repository.UserRepository;
import com.odgiedev.reservvo.security.JwtService;
import com.odgiedev.reservvo.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ses.SesClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtService jwtService;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @MockitoBean protected SesClient sesClient;
    @MockitoBean protected NotificationService notificationService;
    @MockitoBean protected RedisTemplate<String, Object> redisTemplate;
    @MockitoBean protected StreamMessageListenerContainer<String, ?> streamListenerContainer;

    protected User clientUser;
    protected User providerUser;
    protected String clientToken;
    protected String providerToken;

    @BeforeEach
    void setUpBase() {
        clientUser = userRepository.save(User.builder()
                .name("Cliente Teste")
                .email("cliente@test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .role(UserRole.CLIENT)
                .build());

        providerUser = userRepository.save(User.builder()
                .name("Prestador Teste")
                .email("prestador@test.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .role(UserRole.PROVIDER)
                .build());

        clientToken = jwtService.generateToken(clientUser);
        providerToken = jwtService.generateToken(providerUser);
    }

    protected String bearerToken(String token) {
        return "Bearer " + token;
    }
}