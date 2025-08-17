package com.chequepay.controller;

import com.chequepay.dto.ChequeRequest;
import com.chequepay.dto.ChequeResponse;
import com.chequepay.dto.ChequeSplitRequest;
import com.chequepay.service.ChequeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/cheques")
@RequiredArgsConstructor
public class ChequeController {

    private final ChequeService chequeService;

    @PostMapping
    public ChequeResponse issueCheque(Authentication authentication,
                                      @RequestBody ChequeRequest request) {
        String payerUsername = authentication.getName();
        return chequeService.issueCheque(payerUsername, request);
    }

    @GetMapping("/{id}")
    public Optional<ChequeResponse> getCheque(@PathVariable UUID id) {
        return chequeService.getCheque(id);
    }

    @PatchMapping("/{id}/status")
    public Optional<ChequeResponse> updateStatus(@PathVariable UUID id,
                                                 @RequestParam String status) {
        return chequeService.updateStatus(id, status);
    }

    @PostMapping("/{id}/split")
    public List<ChequeResponse> splitCheque(@PathVariable UUID id,
                                            @RequestBody ChequeSplitRequest request) {
        return chequeService.splitCheque(id, request);
    }
}