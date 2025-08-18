package com.chequepay.controller;

import com.chequepay.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/email")
    public Map<String, Object> transferByEmail(@RequestParam UUID chequeId,
                                               @RequestParam String email) {
        boolean success = transferService.sendChequeByEmail(chequeId, email);
        return Map.of("success", success);
    }

    @PostMapping("/qr")
    public Map<String, Object> transferByQr(@RequestParam UUID chequeId) throws Exception {
        String qrBase64 = transferService.generateChequeQRCode(chequeId);
        return Map.of("qrCodeBase64", qrBase64);
    }

    @PostMapping("/p2p")
    public Map<String, Object> transferByP2P(@RequestParam UUID chequeId) {
        String payload = transferService.transferChequeP2P(chequeId);
        return Map.of("payload", payload);
    }
}
