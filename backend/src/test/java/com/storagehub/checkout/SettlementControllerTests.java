package com.storagehub.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.controller.SettlementController;
import com.storagehub.dto.FinalizeSettlementRequest;
import com.storagehub.dto.SettlementPreviewDto;
import com.storagehub.dto.SettlementReceiptDto;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.JwtService;
import com.storagehub.service.SettlementService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SettlementController.class, properties = {
        "app.jwt.secret=settlement-controller-secret-0123456789-abcdefghij",
        "app.jwt.ttl-hours=24"
})
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
public class SettlementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementService settlementService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String staffToken;
    private static final Long STAFF_ID = 2L;

    @BeforeEach
    void setUp() {
        Role staffRole = new Role(2, "Staff", null);
        User staff = new User("Staff Alex", "staff@storagehub.vn", "0900000002",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", staffRole, UserStatus.ACTIVE);

        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));
        staffToken = jwtService.issueToken(STAFF_ID, RoleName.STAFF);
    }

    @Test
    @DisplayName("GET /api/v1/reservations/{id}/settlement-preview returns preview calculation")
    void testGetSettlementPreview() throws Exception {
        SettlementPreviewDto preview = new SettlementPreviewDto(
                871L,
                "BK-2026-00871",
                "S-3",
                "Lan Nguyen",
                BigDecimal.valueOf(172500),
                BigDecimal.valueOf(40000),
                "Scratched door panel",
                BigDecimal.ZERO,
                0,
                BigDecimal.valueOf(20000),
                "Loyalty discount",
                BigDecimal.valueOf(50000),
                "Rental Policy v3",
                false,
                true,
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(152500),
                BigDecimal.ZERO,
                false,
                false,
                true,
                true,
                "Refund 152.500 ₫ after damage fee 40.000 ₫ (waived 20.000 ₫)"
        );

        when(settlementService.calculatePreview(eq(871L), any(), any(), any(), any(), any())).thenReturn(preview);

        mockMvc.perform(get("/api/v1/reservations/871/settlement-preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .param("damageFee", "40000")
                        .param("damageReason", "Scratched door panel")
                        .param("waiverAmount", "20000")
                        .param("waiverReason", "Loyalty discount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositHeld").value(172500))
                .andExpect(jsonPath("$.damageFee").value(40000))
                .andExpect(jsonPath("$.waiverAmount").value(20000))
                .andExpect(jsonPath("$.refundAmount").value(152500))
                .andExpect(jsonPath("$.canFinalize").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/reservations/{id}/settlement closes rental and returns receipt")
    void testFinalizeSettlement() throws Exception {
        FinalizeSettlementRequest request = new FinalizeSettlementRequest(
                BigDecimal.valueOf(40000),
                "Lost access card badge",
                BigDecimal.ZERO,
                BigDecimal.valueOf(20000),
                "Loyalty discount",
                null,
                false,
                "Customer acknowledged deduction"
        );

        SettlementReceiptDto receipt = new SettlementReceiptDto(
                1L,
                "STL-2026-94812",
                871L,
                "BK-2026-00871",
                "S-3",
                "Lan Nguyen",
                "Staff Alex",
                BigDecimal.valueOf(172500),
                BigDecimal.valueOf(40000),
                "Lost access card badge",
                BigDecimal.ZERO,
                BigDecimal.valueOf(20000),
                "Loyalty discount",
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(152500),
                BigDecimal.ZERO,
                "FINALIZED",
                "Customer acknowledged deduction",
                LocalDateTime.now(),
                "Refund 152.500 ₫ after damage fee 40.000 ₫ (waived 20.000 ₫)"
        );

        when(settlementService.finalizeSettlement(eq(871L), any(), any())).thenReturn(receipt);

        mockMvc.perform(post("/api/v1/reservations/871/settlement")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptCode").value("STL-2026-94812"))
                .andExpect(jsonPath("$.refundAmount").value(152500))
                .andExpect(jsonPath("$.waiverAmount").value(20000));
    }
}
