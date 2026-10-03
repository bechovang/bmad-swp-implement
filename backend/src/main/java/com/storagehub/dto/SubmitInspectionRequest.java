package com.storagehub.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitInspectionRequest(
        @NotEmpty(message = "items list cannot be empty")
        List<InspectionItemInput> items,
        Boolean keyReturned,
        Boolean unitEmptied,
        String generalNotes
) {
}
