package com.chequepay.controller;

import com.chequepay.service.ClearingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/clearing")
@RequiredArgsConstructor
public class ClearingController {

    private final ClearingService clearingService;

    @PostMapping("/verify")
    public ResponseEntity<?> verify(Authentication authentication,
                                    @RequestParam UUID chequeId) {
        String currentUser = authentication.getName();

        try {
            clearingService.verifyCheque(chequeId, currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cheque verified successfully"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Internal server error"
            ));
        }
    }

    @PostMapping("/settle")
    public ResponseEntity<?> settle(Authentication authentication,
                                      @RequestParam UUID chequeId) {
        String currentUser = authentication.getName();

        try {
            clearingService.settleCheque(chequeId, currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cheque settled successfully"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (ArithmeticException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Internal server error"
            ));
        }
    }
}
