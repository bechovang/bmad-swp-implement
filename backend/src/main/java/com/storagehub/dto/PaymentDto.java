package com.storagehub.dto;

import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import com.storagehub.entity.PaymentStatus;

public record PaymentDto(
        Long id,
        String receiptCode,
        Long payerId,
        Long reservationId,
        Long orderCode,
        PaymentPurpose purpose,
        PaymentMethod method,
        Long amount,
        PaymentStatus status
) {
}
