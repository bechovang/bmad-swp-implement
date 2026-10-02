package com.storagehub.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateCheckInRequest(
        @NotBlank(message = "Reservation code is required")
        String reservationCode
) {
}
