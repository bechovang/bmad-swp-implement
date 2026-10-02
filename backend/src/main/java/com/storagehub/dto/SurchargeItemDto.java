package com.storagehub.dto;

import java.math.BigDecimal;

public record SurchargeItemDto(
        String type,
        String name,
        BigDecimal amount,
        String description
) {
}
