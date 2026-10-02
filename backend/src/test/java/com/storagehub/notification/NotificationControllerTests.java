package com.storagehub.notification;

import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.dto.ListQuery;
import com.storagehub.dto.PageResponse;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class, properties =
        "app.jwt.secret=notification-test-secret-0123456789-abcdefghij")
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
class NotificationControllerTests {

    private static final Long USER_ID = 101L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserRepository userRepository;

    private String validToken;

    @BeforeEach
    void setUp() {
        User user = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu",
                new Role(1, "Customer", null), UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        validToken = jwtService.issueToken(USER_ID, RoleName.CUSTOMER);
    }

    @Test
    void getUnreadCount_whenAuthenticated_returns200AndCount() throws Exception {
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(new UnreadCountResponse(3));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void getUnreadCount_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getNotifications_whenAuthenticated_returns200AndPageResponse() throws Exception {
        NotificationDto dto = new NotificationDto(
                1L,
                USER_ID,
                "PAYMENT_SUCCEEDED",
                "Deposit received",
                "/rentals/1",
                false,
                Instant.now()
        );
        PageResponse<NotificationDto> pageResponse = PageResponse.of(List.of(dto), ListQuery.of(1, 10), 1);

        when(notificationService.getNotifications(eq(USER_ID), any(ListQuery.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/notifications?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Deposit received"))
                .andExpect(jsonPath("$.items[0].isRead").value(false));
    }

    @Test
    void markAsRead_whenAuthenticatedAndOwned_returns200AndUpdatedDto() throws Exception {
        NotificationDto updated = new NotificationDto(
                1L,
                USER_ID,
                "PAYMENT_SUCCEEDED",
                "Deposit received",
                "/rentals/1",
                true,
                Instant.now()
        );
        when(notificationService.markAsRead(1L, USER_ID)).thenReturn(updated);

        mockMvc.perform(post("/api/v1/notifications/1/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void markAsRead_whenNotFound_returns404Envelope() throws Exception {
        when(notificationService.markAsRead(999L, USER_ID))
                .thenThrow(new ResourceNotFoundException("Notification not found: 999"));

        mockMvc.perform(post("/api/v1/notifications/999/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Notification not found: 999"));
    }

    @Test
    void markAsRead_whenForbidden_returns403Envelope() throws Exception {
        when(notificationService.markAsRead(2L, USER_ID))
                .thenThrow(new AccessDeniedException("User cannot access notification 2"));

        mockMvc.perform(post("/api/v1/notifications/2/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void markAllAsRead_whenAuthenticated_returns200AndZeroCount() throws Exception {
        when(notificationService.markAllAsRead(USER_ID)).thenReturn(new UnreadCountResponse(0));

        mockMvc.perform(post("/api/v1/notifications/mark-all-read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}
