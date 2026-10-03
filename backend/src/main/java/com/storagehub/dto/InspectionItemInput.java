package com.storagehub.dto;

import com.storagehub.entity.InspectionItem;
import com.storagehub.entity.InspectionResult;
import jakarta.validation.constraints.NotNull;

public record InspectionItemInput(
        @NotNull(message = "item is required")
        InspectionItem item,
        @NotNull(message = "result is required")
        InspectionResult result,
        String note
) {
}
