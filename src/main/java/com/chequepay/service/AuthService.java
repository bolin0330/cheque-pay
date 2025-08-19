package com.chequepay.service;

import com.chequepay.dto.AuthResponse;
import com.chequepay.dto.LoginRequest;
import com.chequepay.dto.RegisterRequest;
import com.chequepay.entity.Account;
import com.chequepay.entity.User;
import com.chequepay.repository.UserRepository;
import com.chequepay.repository.AccountRepository;
import com.chequepay.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse(false, "Username already exists", null);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "Email already registered", null);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        userRepository.save(user);

        Account account = Account.builder()
                .username(user.getUsername())
                .balance(BigDecimal.valueOf(5000))
                .build();

        accountRepository.save(account);

        return new AuthResponse(true, "User registered successfully", null);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            String token = jwtUtil.generateToken(request.getUsername());
            return new AuthResponse(true, "Login successful", token);
        } catch (Exception e) {
            return new AuthResponse(false, "Invalid username or password", null);
        }
    }



}