package com.storagehub.unit;

import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.controller.PricingController;
import com.storagehub.controller.UnitController;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.dto.UnitDetailDto;
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
import com.storagehub.service.PricingEngine;
import com.storagehub.service.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { UnitController.class, PricingController.class }, properties =
        "app.jwt.secret=unit-controller-test-secret-0123456789-abcdefghij")
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
class UnitControllerTests {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UnitService unitService;

    @MockitoBean
    private PricingEngine pricingEngine;

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
    @DisplayName("GET /api/v1/units/S-3 when authenticated returns 200 and UnitDetailDto")
    void getUnitDetail_authenticated_returns200() throws Exception {
        UnitDetailDto dto = new UnitDetailDto(
                1L,
                "S-3",
                "S",
                "Small unit around 5 m2",
                "Tan Binh Depot",
                "45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City",
                "A",
                1,
                new BigDecimal("5.00"),
                "PIN",
                "AVAILABLE",
                "/units/S-3.jpg",
                new BigDecimal("345000"),
                new BigDecimal("10"),
                List.of("24/7 CCTV Monitoring", "Personal Access Code (PIN)")
        );
        when(unitService.getUnitDetail("S-3")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/units/S-3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S-3"))
                .andExpect(jsonPath("$.typeName").value("S"))
                .andExpect(jsonPath("$.monthlyRate").value(345000))
                .andExpect(jsonPath("$.depositRate").value(10))
                .andExpect(jsonPath("$.imageUrl").value("/units/S-3.jpg"))
                .andExpect(jsonPath("$.accessType").value("PIN"));
    }

    @Test
    @DisplayName("GET /api/v1/units/S-3 when unauthenticated returns 401 UNAUTHENTICATED")
    void getUnitDetail_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/units/S-3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/units/UNKNOWN-99 returns 404 NOT_FOUND")
    void getUnitDetail_unknown_returns404() throws Exception {
        when(unitService.getUnitDetail("UNKNOWN-99"))
                .thenThrow(new ResourceNotFoundException("Unit UNKNOWN-99 not found"));

        mockMvc.perform(get("/api/v1/units/UNKNOWN-99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Unit UNKNOWN-99 not found"));
    }

    @Test
    @DisplayName("GET /api/v1/pricing/calculate with valid parameters returns 200 and PricingBreakdownDto")
    void calculatePricing_valid_returns200() throws Exception {
        PricingBreakdownDto dto = new PricingBreakdownDto(
                "S-3",
                3,
                new BigDecimal("345000"),
                new BigDecimal("1035000"),
                List.of(),
                new BigDecimal("1035000"),
                new BigDecimal("10"),
                new BigDecimal("103500"),
                true,
                new BigDecimal("103500"),
                "VND",
                "v3"
        );
        when(pricingEngine.calculatePricing(eq("S-3"), eq(3), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pricing/calculate")
                        .param("unitCode", "S-3")
                        .param("durationMonths", "3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitCode").value("S-3"))
                .andExpect(jsonPath("$.durationMonths").value(3))
                .andExpect(jsonPath("$.monthlyRate").value(345000))
                .andExpect(jsonPath("$.baseRent").value(1035000))
                .andExpect(jsonPath("$.depositAmount").value(103500))
                .andExpect(jsonPath("$.depositRefundable").value(true))
                .andExpect(jsonPath("$.totalDueNow").value(103500))
                .andExpect(jsonPath("$.policyVersion").value("v3"));
    }

    @Test
    @DisplayName("GET /api/v1/pricing/calculate with durationMonths=121 returns 400 VALIDATION_FAILED")
    void calculatePricing_durationExceedsMax_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/calculate")
                        .param("unitCode", "S-3")
                        .param("durationMonths", "121")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/pricing/calculate when unauthenticated returns 401 UNAUTHENTICATED")
    void calculatePricing_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/calculate")
                        .param("unitCode", "S-3")
                        .param("durationMonths", "3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/pricing/calculate with durationMonths=0 returns 400 VALIDATION_FAILED")
    void calculatePricing_invalidDuration_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/calculate")
                        .param("unitCode", "S-3")
                        .param("durationMonths", "0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("GET /api/v1/units/browse when authenticated returns 200 and BrowseUnitsResponse")
    void browseUnits_authenticated_returns200() throws Exception {
        com.storagehub.dto.BrowseUnitDto unit1 = new com.storagehub.dto.BrowseUnitDto(
                1L,
                "S-3",
                "S",
                "Small unit around 5 m2",
                "A",
                "Tan Binh Depot",
                1,
                new BigDecimal("5.00"),
                "PIN",
                "PREPARING",
                "/units/S-3.jpg",
                new BigDecimal("345000"),
                new BigDecimal("10"),
                "Available Oct 5 · cleaning buffer",
                LocalDate.of(2026, 10, 5),
                false,
                true
        );
        com.storagehub.dto.BrowseUnitsResponse response = new com.storagehub.dto.BrowseUnitsResponse(
                List.of(unit1),
                1,
                2
        );
        when(unitService.browseUnits(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/units/browse")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvailable").value(1))
                .andExpect(jsonPath("$.totalUnits").value(2))
                .andExpect(jsonPath("$.items[0].code").value("S-3"))
                .andExpect(jsonPath("$.items[0].availabilityStatus").value("Available Oct 5 · cleaning buffer"))
                .andExpect(jsonPath("$.items[0].isInCleaningBuffer").value(true));
    }
}
