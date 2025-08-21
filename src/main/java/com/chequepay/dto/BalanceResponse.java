package com.chequepay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private BigDecimal amount;
    private String payeeUsername;
    private String payeeRealname;
}
