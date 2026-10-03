package com.storagehub.dto;

import com.storagehub.entity.CheckoutRequestStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CheckoutRequestDto(
        Long id,
        Long reservationId,
        String reservationCode,
        Long unitId,
        String unitCode,
        LocalDate requestedDate,
        CheckoutRequestStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
