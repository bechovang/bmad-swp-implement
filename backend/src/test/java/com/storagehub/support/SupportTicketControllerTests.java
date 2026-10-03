package com.storagehub.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.controller.SupportTicketController;
import com.storagehub.dto.EscalateSupportTicketRequest;
import com.storagehub.dto.ResolveSupportTicketRequest;
import com.storagehub.dto.SupportTicketDto;
import com.storagehub.entity.IncidentType;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.SupportTicketStatus;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SupportTicketController.class, properties = {
        "app.jwt.secret=support-handling-test-secret-0123456789-abcdefghij",
        "app.jwt.ttl-hours=24"
})
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
public class SupportTicketControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserRepository userRepository;

    private static final Long STAFF_ID = 2L;
    private static final Long CUSTOMER_ID = 1L;

    private String staffToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        com.storagehub.entity.Role staffRole = new com.storagehub.entity.Role(2, "Staff", null);
        com.storagehub.entity.User staff = new com.storagehub.entity.User("Minh Tran", "minh@storagehub.dev", "0902345678",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", staffRole, com.storagehub.entity.UserStatus.ACTIVE);
        when(userRepository.findById(STAFF_ID)).thenReturn(java.util.Optional.of(staff));

        com.storagehub.entity.Role customerRole = new com.storagehub.entity.Role(1, "Customer", null);
        com.storagehub.entity.User customer = new com.storagehub.entity.User("Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", customerRole, com.storagehub.entity.UserStatus.ACTIVE);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(java.util.Optional.of(customer));

        staffToken = jwtService.issueToken(STAFF_ID, RoleName.STAFF);
        customerToken = jwtService.issueToken(CUSTOMER_ID, RoleName.CUSTOMER);
    }

    @Test
    @DisplayName("POST /api/v1/support-tickets/{id}/resolve succeeds for Staff")
    void resolveEndpointSucceeds() throws Exception {
        SupportTicketDto mockDto = new SupportTicketDto(
                10L, "SR-0032", 1L, "Lan Nguyen", 1L, "S-3",
                1L, "BK-1042", IncidentType.DEVICE_ISSUE, SupportTicketStatus.RESOLVED,
                "Door latch issue", 2L, "Minh Tran", "Replaced hinge", null,
                null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(ticketService.resolveTicket(eq(10L), any(ResolveSupportTicketRequest.class), eq(2L)))
                .thenReturn(mockDto);

        mockMvc.perform(post("/api/v1/support-tickets/10/resolve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Replaced hinge\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNote").value("Replaced hinge"));
    }

    @Test
    @DisplayName("POST /api/v1/support-tickets/{id}/escalate succeeds for Staff")
    void escalateEndpointSucceeds() throws Exception {
        SupportTicketDto mockDto = new SupportTicketDto(
                10L, "SR-0032", 1L, "Lan Nguyen", 1L, "S-3",
                1L, "BK-1042", IncidentType.DEVICE_ISSUE, SupportTicketStatus.ESCALATED,
                "Door latch issue", 2L, "Minh Tran", null, "Flooding in corridor",
                null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(ticketService.escalateTicket(eq(10L), any(EscalateSupportTicketRequest.class), eq(2L)))
                .thenReturn(mockDto);

        mockMvc.perform(post("/api/v1/support-tickets/10/escalate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Flooding in corridor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"));
    }

    @Test
    @DisplayName("POST /api/v1/support-tickets/{id}/escalate with blank note returns 400 Validation Error")
    void escalateBlankNoteReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/support-tickets/10/escalate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Customer is forbidden from calling resolve or escalate endpoints (403)")
    void customerForbiddenFromResolvingOrEscalating() throws Exception {
        mockMvc.perform(post("/api/v1/support-tickets/10/resolve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Customer trying to resolve\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/support-tickets/10/escalate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Customer trying to escalate\"}"))
                .andExpect(status().isForbidden());
    }
}
