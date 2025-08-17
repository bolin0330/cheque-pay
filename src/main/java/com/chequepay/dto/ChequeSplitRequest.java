package com.chequepay.dto;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class ChequeSplitRequest {
    private List<BigDecimal> splitAmounts;
}
