package com.storagehub.notification;

/**
 * Lightweight unread notifications count response for polling and mark-all-read.
 */
public record UnreadCountResponse(long count) {
}
