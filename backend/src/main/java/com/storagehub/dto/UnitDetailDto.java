package com.storagehub.dto;

import java.math.BigDecimal;
import java.util.List;

public record UnitDetailDto(
        Long id,
        String code,
        String typeName,
        String typeDescription,
        String facilityName,
        String facilityAddress,
        String zoneCode,
        Integer floor,
        BigDecimal sizeM2,
        String accessType,
        String status,
        String imageUrl,
        BigDecimal monthlyRate,
        BigDecimal depositRate,
        List<String> securityFeatures
) {
}
