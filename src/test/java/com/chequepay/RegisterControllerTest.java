package com.chequepay;

import com.chequepay.controller.AuthController;
import com.chequepay.entity.User;
import com.chequepay.repository.AccountRepository;
import com.chequepay.repository.UserRepository;
import com.chequepay.service.AuthService;
import com.chequepay.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void registerSuccess() throws Exception {
        String requestJson = """
            {
              "username": "test1",
              "email": "test@example.com",
              "password": "Password123",
              "realname": "Marc Spector",
              "phoneNumber": "0912345678"
            }
        """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        assertTrue(userRepository.existsByUsername("test1"));
        assertTrue(accountRepository.existsByUsername("test1"));
    }

    @Test
    void registerExistingUsername() throws Exception {
        userRepository.save(User.builder()
                .username("test2")
                .email("exist@example.com")
                .password("encoded")
                .realname("Existing User")
                .phoneNumber("0988888888")
                .role("USER")
                .build());

        String requestJson = """
            {
              "username": "test2",
              "email": "new@example.com",
              "password": "Password123",
              "realname": "Eric Cartman",
              "phoneNumber": "123456789"
            }
        """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void registerExistingEmail() throws Exception {
        userRepository.save(User.builder()
                .username("test3")
                .email("exist@example.com")
                .password("encoded")
                .realname("Existing User")
                .phoneNumber("0988888888")
                .role("USER")
                .build());

        String requestJson = """
            {
              "username": "bolin0330",
              "email": "exist@example.com",
              "password": "Password123",
              "realname": "Bolin Chen",
              "phoneNumber": "0912345678"
            }
        """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void registerInvalidName() throws Exception {
        String requestJson = """
            {
              "username": "test4",
              "email": "test4@example.com",
              "password": "Password123",
              "realname": "ØZI",
              "phoneNumber": "0912345678"
            }
        """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Real name must be English letters and spaces only (max 25)"));
    }

    @Test
    void registerInvalidPhoneNumber() throws Exception {
        String requestJson = """
            {
              "username": "test5",
              "email": "test5@example.com",
              "password": "Password123",
              "realname": "Billy Butcher",
              "phoneNumber": "XX0123456789"
            }
        """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Phone number must be digits only (max 15)"));
    }

    @Test
    void registerInternalError() throws Exception {
        UserRepository mockRepo = Mockito.mock(UserRepository.class);
        Mockito.when(mockRepo.existsByUsername(Mockito.any())).thenReturn(false);
        Mockito.when(mockRepo.existsByEmail(Mockito.any())).thenReturn(false);
        Mockito.when(mockRepo.save(Mockito.any())).thenThrow(new RuntimeException("DB Error"));

        AuthService authService = new AuthService(
                mockRepo,
                accountRepository,
                new BCryptPasswordEncoder(),
                new JwtUtil(),
                Mockito.mock(AuthenticationManager.class)
        );

        AuthController controller = new AuthController(authService, mockRepo);

        MockMvc standalone = MockMvcBuilders.standaloneSetup(controller).build();

        String requestJson = """
            {
              "username": "test6",
              "email": "crash@example.com",
              "password": "Password123",
              "realname": "Murakami Haruki",
              "phoneNumber": "0912345678"
            }
        """;

        standalone.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }
}