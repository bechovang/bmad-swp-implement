package com.storagehub.dto;

import com.storagehub.entity.ReservationStatus;

public record CheckInValidationDto(
        boolean valid,
        String errorCode,
        String errorMessage,
        Long taskId,
        Long reservationId,
        String reservationCode,
        String customerName,
        String unitCode,
        Long depositAmountPaid,
        Long totalRentDue,
        String depositReceiptCode,
        boolean rentPaid,
        String rentReceiptCode,
        ReservationStatus status
) {
    public static CheckInValidationDto error(Long taskId, String errorCode, String errorMessage) {
        return new CheckInValidationDto(
                false,
                errorCode,
                errorMessage,
                taskId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null
        );
    }
}
