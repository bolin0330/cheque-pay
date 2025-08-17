package com.chequepay.controller;

import com.chequepay.dto.AuthResponse;
import com.chequepay.dto.LoginRequest;
import com.chequepay.dto.RegisterRequest;
import com.chequepay.entity.User;
import com.chequepay.repository.UserRepository;
import com.chequepay.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/profile")
    public AuthResponse profile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String message = String.format("Profile: %s (%s)", user.getUsername(), user.getEmail());
            return new AuthResponse(true, message, null);
        } else {
            return new AuthResponse(false, "User not found", null);
        }
    }

    @PostMapping("/logout")
    public AuthResponse logout() {
        SecurityContextHolder.clearContext();
        return new AuthResponse(true, "Logged out successfully", null);
    }
}