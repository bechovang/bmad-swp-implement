package com.storagehub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateReservationRequest(
        @NotBlank(message = "unitCode is required")
        String unitCode,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "durationMonths is required")
        @Min(value = 1, message = "durationMonths must be at least 1")
        @Max(value = 120, message = "durationMonths cannot exceed 120")
        Integer durationMonths
) {
}
