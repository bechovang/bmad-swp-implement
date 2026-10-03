package com.storagehub.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCheckoutRequest(
        @NotNull(message = "requestedDate must not be null")
        LocalDate requestedDate,
        String notes
) {
}
