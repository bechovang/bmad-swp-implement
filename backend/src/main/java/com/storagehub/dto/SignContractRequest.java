package com.storagehub.dto;

import jakarta.validation.constraints.NotBlank;

public record SignContractRequest(
        @NotBlank(message = "Signed photo URL is required")
        String signedPhotoUrl
) {
}
