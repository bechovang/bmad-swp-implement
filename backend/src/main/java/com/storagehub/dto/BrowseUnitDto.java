package com.storagehub.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BrowseUnitDto(
        Long id,
        String code,
        String typeName,
        String typeDescription,
        String zoneCode,
        String facilityName,
        Integer floor,
        BigDecimal sizeM2,
        String accessType,
        String status,
        String imageUrl,
        BigDecimal monthlyRate,
        BigDecimal depositRate,
        String availabilityStatus,
        LocalDate availableFromDate,
        boolean isImmediatelyAvailable,
        boolean isInCleaningBuffer
) {
}
