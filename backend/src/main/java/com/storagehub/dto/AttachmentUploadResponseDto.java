package com.storagehub.dto;

public record AttachmentUploadResponseDto(
        String fileUrl,
        String fileName,
        Long size,
        String contentType
) {
}
