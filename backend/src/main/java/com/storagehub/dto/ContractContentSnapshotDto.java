package com.storagehub.dto;

import java.time.LocalDate;

public record ContractContentSnapshotDto(
        String code,
        String reservationCode,
        String unitCode,
        Long monthlyRate,
        Long baseRent,
        Long totalRent,
        Long depositAmount,
        Long depositRate,
        Integer durationMonths,
        String policyVersion,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        String customerName,
        String customerEmail,
        String customerPhone,
        String facilityName,
        String facilityAddress,
        String zoneCode,
        Integer floor,
        Double sizeM2
) {
}
