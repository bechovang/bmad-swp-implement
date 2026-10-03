package com.storagehub.dto;

import com.storagehub.entity.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(
        @NotNull(message = "unitId is required")
        Long unitId,

        @NotNull(message = "incidentType is required")
        IncidentType incidentType,

        @NotBlank(message = "description must not be blank")
        @Size(min = 5, max = 1000, message = "description must be between 5 and 1000 characters")
        String description
) {
}
