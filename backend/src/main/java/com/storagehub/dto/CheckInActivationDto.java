package com.storagehub.dto;

import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.UnitStatus;

public record CheckInActivationDto(
        String accessCode,
        String reservationCode,
        Long reservationId,
        String unitCode,
        ReservationStatus reservationStatus,
        UnitStatus unitStatus,
        TaskStatus taskStatus
) {
}
