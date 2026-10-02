package com.storagehub.notification;

import java.time.Instant;

/**
 * Public notification representation matching openapi schema NotificationDto.
 */
public record NotificationDto(
        Long id,
        Long userId,
        String type,
        String title,
        String deepLink,
        boolean isRead,
        Instant createdAt) {

    public static NotificationDto fromEntity(Notification entity) {
        return new NotificationDto(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTitle(),
                entity.getDeepLink(),
                entity.isRead(),
                entity.getCreatedAt()
        );
    }
}
