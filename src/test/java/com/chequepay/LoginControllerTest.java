package com.chequepay;

import com.chequepay.controller.AuthController;
import com.chequepay.dto.LoginRequest;
import com.chequepay.dto.AuthResponse;
import com.chequepay.repository.UserRepository;
import com.chequepay.service.CustomUserDetailsService;
import com.chequepay.service.AuthService;
import com.chequepay.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @WithMockUser(username = "test1")
    @Test
    void loginSuccess() throws Exception {
        AuthResponse response = new AuthResponse(true, "Login successful", "mocked-jwt-token");

        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        Mockito.when(jwtUtil.generateToken(anyString()))
                .thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test1\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }

    @WithMockUser(username = "test2")
    @Test
    void loginInvalidCredentials() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test2\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @WithMockUser(username = "test3")
    @Test
    void loginCorsError() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("CORS policy: No 'Access-Control-Allow-Origin' header present"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test3\",\"password\":\"123456\"}"))
                .andExpect(status().isForbidden());
    }

    @WithMockUser(username = "test4")
    @Test
    void loginInternalError() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Internal server error"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test4\",\"password\":\"123456\"}"))
                .andExpect(status().isInternalServerError());
    }
}