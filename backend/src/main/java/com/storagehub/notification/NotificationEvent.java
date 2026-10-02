package com.storagehub.notification;

/**
 * Internal business event DTO passed to {@link NotificationService#send(NotificationEvent)}.
 * Other modules must never INSERT into notifications directly (AD-6).
 */
public record NotificationEvent(
        Long userId,
        String type,
        String title,
        String deepLink,
        String tone) {

    public NotificationEvent(Long userId, String type, String title, String deepLink) {
        this(userId, type, title, deepLink, "info");
    }

    public NotificationEvent {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type cannot be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be blank");
        }
        if (tone == null || tone.isBlank()) {
            tone = "info";
        }
    }
}
