package com.storagehub.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementReceiptDto(
        Long id,
        String receiptCode,
        Long reservationId,
        String reservationCode,
        String unitCode,
        String customerName,
        String staffName,
        BigDecimal depositHeld,
        BigDecimal damageFee,
        String damageReason,
        BigDecimal lateFee,
        BigDecimal totalCharges,
        BigDecimal refundAmount,
        BigDecimal extraFeeAmount,
        String status,
        String notes,
        LocalDateTime createdAt,
        String summaryMessage
) {}
