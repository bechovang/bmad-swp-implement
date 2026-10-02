package com.storagehub.dto;

public record AccessCodeResponseDto(
        String accessCode,
        String accessType,
        String unitCode
) {
}
