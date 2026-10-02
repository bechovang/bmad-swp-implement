package com.storagehub.notification;

import com.storagehub.dto.ListQuery;
import com.storagehub.dto.PageResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Dedicated lightweight endpoint polled by the top-bar bell icon.
     */
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return notificationService.getUnreadCount(currentUserId);
    }

    /**
     * Paginated notifications scoped to the authenticated user, unread first.
     */
    @GetMapping
    public PageResponse<NotificationDto> getNotifications(ListQuery query, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return notificationService.getNotifications(currentUserId, query);
    }

    /**
     * Mark a single notification as read (must be owned by the caller).
     */
    @PostMapping("/{id}/read")
    public NotificationDto markAsRead(@PathVariable("id") Long id, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return notificationService.markAsRead(id, currentUserId);
    }

    /**
     * Mark all notifications of the current user as read.
     */
    @PostMapping("/mark-all-read")
    public UnreadCountResponse markAllAsRead(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return notificationService.markAllAsRead(currentUserId);
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("User is not authenticated");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Invalid authenticated user id: " + authentication.getName());
        }
    }
}
