package com.storagehub.dto;

import java.math.BigDecimal;

public record SettlementPreviewDto(
        Long reservationId,
        String reservationCode,
        String unitCode,
        String customerName,
        BigDecimal depositHeld,
        BigDecimal damageFee,
        String damageReason,
        BigDecimal lateFee,
        int daysLate,
        BigDecimal totalCharges,
        BigDecimal refundAmount,
        BigDecimal extraFeeAmount,
        boolean extraFeeRequired,
        boolean extraFeePaid,
        boolean damageReasonRequired,
        boolean canFinalize,
        String summaryMessage
) {}
