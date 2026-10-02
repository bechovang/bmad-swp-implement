package com.storagehub.dto;

import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "reservationId is required")
        Long reservationId,

        @NotNull(message = "purpose is required")
        PaymentPurpose purpose,

        @NotNull(message = "method is required")
        PaymentMethod method,

        Long amount
) {
}
