package com.storagehub.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.EscalationController;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.dto.EscalationDto;
import com.storagehub.dto.SeverityDecisionRequest;
import com.storagehub.entity.EscalationDecision;
import com.storagehub.entity.IncidentType;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.SupportTicketStatus;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.JwtService;
import com.storagehub.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EscalationController.class, properties = {
        "app.jwt.secret=escalation-controller-secret-0123456789-abcdefghij",
        "app.jwt.ttl-hours=24"
})
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
public class EscalationControllerTests {

    private static final Long MANAGER_ID = 3L;
    private static final Long STAFF_ID = 2L;
    private static final Long CUSTOMER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserRepository userRepository;

    private String managerToken;
    private String staffToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role managerRole = new Role(3, "Facility Manager", null);
        User manager = new User("Hoa Pham", "manager@storagehub.dev", "0903456789",
                "hash", managerRole, UserStatus.ACTIVE);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

        Role staffRole = new Role(2, "Staff", null);
        User staff = new User("Minh Tran", "staff@storagehub.dev", "0902345678",
                "hash", staffRole, UserStatus.ACTIVE);
        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));

        Role customerRole = new Role(1, "Customer", null);
        User customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "hash", customerRole, UserStatus.ACTIVE);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        managerToken = jwtService.issueToken(MANAGER_ID, RoleName.FACILITY_MANAGER);
        staffToken = jwtService.issueToken(STAFF_ID, RoleName.STAFF);
        customerToken = jwtService.issueToken(CUSTOMER_ID, RoleName.CUSTOMER);
    }

    @Test
    @DisplayName("GET /api/v1/escalations succeeds for Facility Manager")
    void getEscalationsSucceedsForFM() throws Exception {
        EscalationDto mockDto = new EscalationDto(
                1L, 2L, "SR-0033", 1L, "Lan Nguyen",
                2L, "M-2", 1L, "BK-1042",
                IncidentType.OTHER, SupportTicketStatus.ESCALATED, "Water ingress",
                2L, "Minh Tran", "Water leak in unit M-2",
                null, null, EscalationDecision.PENDING, null,
                null, null, null,
                LocalDateTime.now(), null
        );

        when(ticketService.getEscalations(eq(MANAGER_ID), eq("FACILITY_MANAGER")))
                .thenReturn(List.of(mockDto));

        mockMvc.perform(get("/api/v1/escalations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCode").value("SR-0033"))
                .andExpect(jsonPath("$[0].decision").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/escalations returns 403 Forbidden for Customer")
    void getEscalationsForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/escalations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/escalations/{id}/decision succeeds for Facility Manager")
    void postDecisionSucceedsForFM() throws Exception {
        EscalationDto mockDto = new EscalationDto(
                1L, 2L, "SR-0033", 1L, "Lan Nguyen",
                2L, "M-2", 1L, "BK-1042",
                IncidentType.OTHER, SupportTicketStatus.IN_PROGRESS, "Water ingress",
                2L, "Minh Tran", "Water leak in unit M-2",
                3L, "Hoa Pham", EscalationDecision.MAINTENANCE_RELOCATE, "Customer relocated to M-5",
                3L, "M-5", "481920",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(ticketService.processSeverityDecision(eq(1L), any(SeverityDecisionRequest.class), eq(MANAGER_ID)))
                .thenReturn(mockDto);

        mockMvc.perform(post("/api/v1/escalations/1/decision")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"MAINTENANCE_RELOCATE\",\"managerNote\":\"Customer relocated to M-5\",\"targetUnitId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("MAINTENANCE_RELOCATE"))
                .andExpect(jsonPath("$.relocatedToUnitCode").value("M-5"));
    }
}
