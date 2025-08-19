package com.chequepay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChequeRequest {
    private BigDecimal amount;
    private String payeeUsername;
    private String payeeRealname;
    private LocalDateTime expiryDate;
}
