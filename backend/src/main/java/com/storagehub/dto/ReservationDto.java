package com.storagehub.dto;

import com.storagehub.entity.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

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
        ReservationStatus status,
        List<PaymentDto> payments
) {
    public ReservationDto(
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
        this(id, code, customerId, customerName, unitId, unitCode, unitTypeName, facilityName, facilityAddress,
                zoneCode, floor, sizeM2, accessType, startDate, endDate, durationMonths, depositAmount,
                monthlyRate, baseRent, totalRent, policyVersion, accessCode, status, List.of());
    }
}
