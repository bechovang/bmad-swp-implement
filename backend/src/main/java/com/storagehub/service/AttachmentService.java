package com.storagehub.service;

import com.storagehub.dto.AttachmentUploadResponseDto;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path storageLocation;

    public AttachmentService() {
        this.storageLocation = Paths.get("storage/attachments").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            log.error("Could not create storage directory for attachments: {}", e.getMessage());
        }
    }

    public AttachmentUploadResponseDto storeAttachment(MultipartFile file, Long currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("FILE_EMPTY", "Cannot upload empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("FILE_TOO_LARGE", "File size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException("UNSUPPORTED_MEDIA_TYPE", "Only JPEG, PNG, WebP, and PDF files are allowed");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment");
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx >= 0) {
            extension = originalFilename.substring(dotIdx);
        } else {
            if ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType)) {
                extension = ".jpg";
            } else if ("image/png".equalsIgnoreCase(contentType)) {
                extension = ".png";
            } else if ("application/pdf".equalsIgnoreCase(contentType)) {
                extension = ".pdf";
            }
        }

        String uniqueFileName = UUID.randomUUID().toString() + extension;
        Path targetPath = this.storageLocation.resolve(uniqueFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file {}: {}", uniqueFileName, e.getMessage());
            throw new BusinessRuleException("STORAGE_ERROR", "Failed to store file: " + e.getMessage());
        }

        String fileUrl = "/api/v1/attachments/" + uniqueFileName;
        return new AttachmentUploadResponseDto(
                fileUrl,
                uniqueFileName,
                file.getSize(),
                contentType
        );
    }

    public Resource loadAttachmentAsResource(String filename) {
        try {
            String cleanFilename = StringUtils.cleanPath(filename);
            Path filePath = this.storageLocation.resolve(cleanFilename).normalize();
            if (!filePath.startsWith(this.storageLocation)) {
                throw new BusinessRuleException("INVALID_PATH", "Cannot access files outside storage directory");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Attachment not found: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Attachment not found: " + filename);
        }
    }
}
