package com.storagehub.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidContractRequest(
        @NotBlank(message = "Void reason is mandatory")
        String reason
) {
}
