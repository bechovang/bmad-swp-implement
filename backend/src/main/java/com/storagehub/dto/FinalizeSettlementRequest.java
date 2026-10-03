package com.storagehub.dto;

import java.math.BigDecimal;

public record FinalizeSettlementRequest(
        BigDecimal damageFee,
        String damageReason,
        BigDecimal lateFee,
        String paymentMethod,
        Boolean cashReceived,
        String notes
) {
    public FinalizeSettlementRequest {
        if (damageFee == null) {
            damageFee = BigDecimal.ZERO;
        }
        if (lateFee == null) {
            lateFee = BigDecimal.ZERO;
        }
        if (cashReceived == null) {
            cashReceived = false;
        }
    }
}
