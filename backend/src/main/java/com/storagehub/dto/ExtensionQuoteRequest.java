package com.storagehub.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ExtensionQuoteRequest(
        @NotNull(message = "New end date is required")
        LocalDate newEndDate
) {}
