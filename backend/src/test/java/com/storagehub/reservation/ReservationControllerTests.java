package com.storagehub.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.controller.ReservationController;
import com.storagehub.dto.CreateReservationRequest;
import com.storagehub.dto.ReservationDto;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.JwtService;
import com.storagehub.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservationController.class, properties =
        "app.jwt.secret=reservation-test-secret-0123456789-abcdefghij")
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
class ReservationControllerTests {

    private static final Long USER_ID = 101L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserRepository userRepository;

    private String customerToken;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role(1, "Customer", null);
        User user = new User("Customer User", "customer@example.com", "0901234567",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", customerRole, UserStatus.ACTIVE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        customerToken = jwtService.issueToken(USER_ID, RoleName.CUSTOMER);
    }

    @Test
    @DisplayName("POST /api/v1/reservations: 201 Created on valid request")
    void createReservation_success() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest("S-3", LocalDate.of(2026, 10, 5), 3);
        ReservationDto dto = new ReservationDto(
                1L, "BK-2026-0001", USER_ID, "Customer User",
                10L, "S-3", "Small", "Tan Binh Depot",
                "45 Nguyen Van Troi", "A", 1, 4.0, "PIN",
                LocalDate.of(2026, 10, 5), LocalDate.of(2027, 1, 5),
                3, 103500L, 345000L, 1035000L, 1035000L, "v3", null,
                ReservationStatus.PENDING_PAYMENT
        );

        when(reservationService.createReservation(any(CreateReservationRequest.class), eq(USER_ID)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("BK-2026-0001"))
                .andExpect(jsonPath("$.unitCode").value("S-3"))
                .andExpect(jsonPath("$.durationMonths").value(3))
                .andExpect(jsonPath("$.depositAmount").value(103500))
                .andExpect(jsonPath("$.monthlyRate").value(345000))
                .andExpect(jsonPath("$.baseRent").value(1035000))
                .andExpect(jsonPath("$.totalRent").value(1035000))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations: 401 Unauthorized when no token provided")
    void createReservation_unauthenticated_returns401() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest("S-3", LocalDate.of(2026, 10, 5), 1);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations: 409 Conflict when unit unavailable")
    void createReservation_conflict_returns409() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest("S-3", LocalDate.of(2026, 10, 5), 1);

        when(reservationService.createReservation(any(CreateReservationRequest.class), eq(USER_ID)))
                .thenThrow(new BusinessRuleException("UNIT_UNAVAILABLE", "Unit S-3 is no longer available for the selected dates"));

        mockMvc.perform(post("/api/v1/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UNIT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Unit S-3 is no longer available for the selected dates"));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/{id}: 200 OK when found")
    void getReservation_success() throws Exception {
        ReservationDto dto = new ReservationDto(
                1L, "BK-2026-0001", USER_ID, "Customer User",
                10L, "S-3", "Small", "Tan Binh Depot",
                "45 Nguyen Van Troi", "A", 1, 4.0, "PIN",
                LocalDate.of(2026, 10, 5), LocalDate.of(2027, 1, 5),
                3, 103500L, 345000L, 1035000L, 1035000L, "v3", null,
                ReservationStatus.PENDING_PAYMENT
        );

        when(reservationService.getReservation(eq(1L), eq(USER_ID), anyBoolean()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/reservations/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("BK-2026-0001"))
                .andExpect(jsonPath("$.unitCode").value("S-3"));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/{id}: 404 NOT_FOUND when reservation not found")
    void getReservation_notFound_returns404() throws Exception {
        when(reservationService.getReservation(eq(999L), eq(USER_ID), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Reservation 999 not found"));

        mockMvc.perform(get("/api/v1/reservations/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/{id}: 403 FORBIDDEN when user denied")
    void getReservation_forbidden_returns403() throws Exception {
        when(reservationService.getReservation(eq(2L), eq(USER_ID), anyBoolean()))
                .thenThrow(new AccessDeniedException("Access denied to reservation 2"));

        mockMvc.perform(get("/api/v1/reservations/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
