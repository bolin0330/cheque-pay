package com.chequepay;

import com.chequepay.dto.AuthResponse;
import com.chequepay.dto.LoginRequest;
import com.chequepay.service.AuthService;
import com.chequepay.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {
    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void loginSuccess_returnsToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("test1");
        request.setPassword("123456");

        Authentication mockAuth = Mockito.mock(Authentication.class);
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        Mockito.when(jwtUtil.generateToken("test1"))
                .thenReturn("real-jwt-token");

        AuthResponse response = authService.login(request);

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("Login successful", response.getMessage());
        Assertions.assertEquals("real-jwt-token", response.getToken());
    }

    @Test
    void loginBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("test2");
        request.setPassword("wrong123");

        Mockito.doThrow(new BadCredentialsException("Invalid username or password"))
                .when(authenticationManager)
                .authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class));

        Assertions.assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void loginRuntimeException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("test3");
        request.setPassword("123456");

        Mockito.doThrow(new RuntimeException("Some unexpected error"))
                .when(authenticationManager)
                .authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class));

        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> {
            authService.login(request);
        });

        Assertions.assertTrue(ex.getMessage().contains("Unexpected error during login"));
    }
}
