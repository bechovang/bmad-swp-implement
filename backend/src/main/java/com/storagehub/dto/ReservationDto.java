package com.storagehub.dto;

import com.storagehub.entity.ReservationStatus;

import java.time.LocalDate;

public record ReservationDto(
        Long id,
        String code,
        Long customerId,
        String customerName,
        Long unitId,
        String unitCode,
        String unitTypeName,
        String facilityName,
        String facilityAddress,
        String zoneCode,
        Integer floor,
        Double sizeM2,
        String accessType,
        LocalDate startDate,
        LocalDate endDate,
        Integer durationMonths,
        Long depositAmount,
        Long monthlyRate,
        Long baseRent,
        Long totalRent,
        String policyVersion,
        String accessCode,
        ReservationStatus status
) {
}
