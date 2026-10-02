package com.storagehub.notification;

import com.storagehub.dto.ListQuery;
import com.storagehub.dto.PageResponse;
import com.storagehub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository);
    }

    @Test
    void send_savesNotificationAndReturnsDto() {
        NotificationEvent event = new NotificationEvent(
                1L,
                "PAYMENT_SUCCEEDED",
                "Deposit received - 103.500 VND for unit S-3",
                "/rentals/1",
                "success"
        );

        Notification saved = new Notification(
                10L,
                1L,
                "PAYMENT_SUCCEEDED",
                "Deposit received - 103.500 VND for unit S-3",
                "/rentals/1",
                false,
                Instant.now()
        );

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationDto result = notificationService.send(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification passed = captor.getValue();
        assertThat(passed.getUserId()).isEqualTo(1L);
        assertThat(passed.getType()).isEqualTo("PAYMENT_SUCCEEDED");
        assertThat(passed.getTitle()).isEqualTo("Deposit received - 103.500 VND for unit S-3");
        assertThat(passed.getDeepLink()).isEqualTo("/rentals/1");
        assertThat(passed.isRead()).isFalse();

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Deposit received - 103.500 VND for unit S-3");
        assertThat(result.isRead()).isFalse();
    }

    @Test
    void getUnreadCount_returnsCountFromRepository() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

        UnreadCountResponse response = notificationService.getUnreadCount(1L);

        assertThat(response.count()).isEqualTo(3L);
        verify(notificationRepository).countByUserIdAndIsReadFalse(1L);
    }

    @Test
    void getNotifications_returnsPageResponseWithCorrectOrder() {
        Instant now = Instant.now();
        List<Notification> content = List.of(
                new Notification(2L, 1L, "TASK", "Unread notification", "/tasks/1", false, now),
                new Notification(1L, 1L, "TASK", "Read notification", "/tasks/2", true, now.minusSeconds(60))
        );

        Pageable pageable = PageRequest.of(0, 10);
        when(notificationRepository.findByUserIdOrderByIsReadAscCreatedAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(content, pageable, 2));

        ListQuery query = ListQuery.of(1, 10);
        PageResponse<NotificationDto> response = notificationService.getNotifications(1L, query);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).id()).isEqualTo(2L);
        assertThat(response.items().get(0).isRead()).isFalse();
        assertThat(response.items().get(1).id()).isEqualTo(1L);
        assertThat(response.items().get(1).isRead()).isTrue();
    }

    @Test
    void markAsRead_whenOwnedByUser_marksReadAndReturnsDto() {
        Notification existing = new Notification(5L, 1L, "TYPE", "Title", "/path", false, Instant.now());
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationDto result = notificationService.markAsRead(5L, 1L);

        assertThat(result.isRead()).isTrue();
        verify(notificationRepository).save(existing);
        assertThat(existing.isRead()).isTrue();
    }

    @Test
    void markAsRead_whenAlreadyRead_doesNotDuplicateSave() {
        Notification existing = new Notification(5L, 1L, "TYPE", "Title", "/path", true, Instant.now());
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(existing));

        NotificationDto result = notificationService.markAsRead(5L, 1L);

        assertThat(result.isRead()).isTrue();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_whenNotFound_throwsResourceNotFoundException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void markAsRead_whenBelongsToDifferentUser_throwsAccessDeniedException() {
        Notification existing = new Notification(5L, 2L, "TYPE", "Title", "/path", false, Instant.now());
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> notificationService.markAsRead(5L, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User 1 cannot access notification 5");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_callsRepositoryAndReturnsZeroCount() {
        when(notificationRepository.markAllAsReadByUserId(1L)).thenReturn(5);

        UnreadCountResponse response = notificationService.markAllAsRead(1L);

        assertThat(response.count()).isEqualTo(0);
        verify(notificationRepository).markAllAsReadByUserId(1L);
    }
}
