package com.storagehub.notification;

import com.storagehub.dto.ListQuery;
import com.storagehub.dto.PageResponse;
import com.storagehub.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public NotificationDto send(NotificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        Notification notification = new Notification(
                event.userId(),
                event.type(),
                event.title(),
                event.deepLink()
        );
        Notification saved = notificationRepository.save(notification);
        return NotificationDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> getNotifications(Long userId, ListQuery query) {
        ListQuery q = query != null ? query : ListQuery.of((Integer) null, (Integer) null);
        Pageable pageable = PageRequest.of(q.page() - 1, q.pageSize());
        Page<Notification> pageResult = notificationRepository.findByUserIdOrderByIsReadAscCreatedAtDesc(userId, pageable);
        List<NotificationDto> items = pageResult.getContent().stream()
                .map(NotificationDto::fromEntity)
                .toList();
        return PageResponse.of(items, q, pageResult.getTotalElements());
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long id, Long currentUserId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

        if (!notification.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("User " + currentUserId + " cannot access notification " + id);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }

        return NotificationDto.fromEntity(notification);
    }

    @Override
    @Transactional
    public UnreadCountResponse markAllAsRead(Long currentUserId) {
        notificationRepository.markAllAsReadByUserId(currentUserId);
        return new UnreadCountResponse(0);
    }
}
