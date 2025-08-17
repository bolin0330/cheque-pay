package com.chequepay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ChequeResponse {
    private UUID id;
    private BigDecimal amount;
    private String payerUsername;
    private String payeeUsername;
    private LocalDateTime issueDate;
    private LocalDateTime expiryDate;
    private String status;
}