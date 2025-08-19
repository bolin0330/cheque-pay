package com.chequepay.controller;

import com.chequepay.service.ClearingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/clearing")
@RequiredArgsConstructor
public class ClearingController {

    private final ClearingService clearingService;

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestParam UUID chequeId) {
        boolean valid = clearingService.verifyCheque(chequeId);
        return Map.of("valid", valid);
    }

    @PostMapping("/settle")
    public Map<String, Object> settle(@RequestParam UUID chequeId) {
        boolean success = clearingService.settleCheque(chequeId);
        return Map.of("settled", success);
    }
}
