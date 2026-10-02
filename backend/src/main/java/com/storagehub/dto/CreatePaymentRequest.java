package com.storagehub.dto;

import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreatePaymentRequest(
        @NotNull(message = "reservationId is required")
        Long reservationId,

        @NotNull(message = "purpose is required")
        PaymentPurpose purpose,

        @NotNull(message = "method is required")
        PaymentMethod method,

        Long amount,

        LocalDate newEndDate
) {
    public CreatePaymentRequest(Long reservationId, PaymentPurpose purpose, PaymentMethod method, Long amount) {
        this(reservationId, purpose, method, amount, null);
    }
}
