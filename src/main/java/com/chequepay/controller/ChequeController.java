package com.chequepay.controller;

import com.chequepay.dto.ChequeRequest;
import com.chequepay.dto.ChequeResponse;
import com.chequepay.dto.ChequeSplitRequest;
import com.chequepay.service.ChequeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/cheques")
@RequiredArgsConstructor
public class ChequeController {

    private final ChequeService chequeService;

    @PostMapping
    public ResponseEntity<?> issueCheque(Authentication authentication,
                                         @RequestBody ChequeRequest request) {
        try {
            String payerUsername = authentication.getName();
            ChequeResponse response = chequeService.issueCheque(payerUsername, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCheque(@PathVariable UUID id) {
        try {
            ChequeResponse response = chequeService.getCheque(id);
            return ResponseEntity.ok(response); // 200
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id,
                                                 @RequestParam String status) {
        try {
            ChequeResponse response = chequeService.updateStatus(id, status);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    @PostMapping("/{id}/split")
    public ResponseEntity<?> splitCheque(@PathVariable UUID id,
                                            @RequestBody ChequeSplitRequest request) {
        try {
            List<ChequeResponse> response = chequeService.splitCheque(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
}