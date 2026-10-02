package com.storagehub.notification;

import com.storagehub.dto.ListQuery;
import com.storagehub.dto.PageResponse;

public interface NotificationService {

    NotificationDto send(NotificationEvent event);

    UnreadCountResponse getUnreadCount(Long userId);

    PageResponse<NotificationDto> getNotifications(Long userId, ListQuery query);

    NotificationDto markAsRead(Long id, Long currentUserId);

    UnreadCountResponse markAllAsRead(Long currentUserId);
}
