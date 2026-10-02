package com.storagehub.dto;

import java.math.BigDecimal;
import java.util.List;

public record PricingBreakdownDto(
        String unitCode,
        int durationMonths,
        BigDecimal monthlyRate,
        BigDecimal baseRent,
        List<SurchargeItemDto> surcharges,
        BigDecimal totalRent,
        BigDecimal depositRate,
        BigDecimal depositAmount,
        boolean depositRefundable,
        BigDecimal totalDueNow,
        String currency,
        String policyVersion
) {
}
