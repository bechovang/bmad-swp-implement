package com.storagehub.dto;

import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import com.storagehub.entity.PaymentStatus;

import java.time.Instant;

public record PaymentResponseDto(
        Long paymentId,
        Long orderCode,
        Long amount,
        PaymentStatus status,
        PaymentMethod method,
        PaymentPurpose purpose,
        String checkoutUrl,
        String qrCode,
        Instant expiresAt
) {
}
