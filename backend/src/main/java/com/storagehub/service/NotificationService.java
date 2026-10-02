package com.storagehub.service;

import com.storagehub.entity.Notification;
import com.storagehub.entity.User;
import com.storagehub.repository.NotificationRepository;
import com.storagehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification send(Long userId, String type, String title, String deepLink) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Notification notification = new Notification(user, type, title, deepLink);
        return notificationRepository.save(notification);
    }
}
