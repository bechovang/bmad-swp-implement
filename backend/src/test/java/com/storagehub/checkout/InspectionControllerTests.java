package com.storagehub.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.controller.InspectionController;
import com.storagehub.dto.CheckoutTaskDetailDto;
import com.storagehub.dto.InspectionItemDto;
import com.storagehub.dto.InspectionItemInput;
import com.storagehub.dto.SubmitInspectionRequest;
import com.storagehub.entity.*;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.InspectionService;
import com.storagehub.service.JwtService;
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

import java.time.LocalDate;
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

@WebMvcTest(controllers = InspectionController.class, properties = {
        "app.jwt.secret=inspection-controller-secret-0123456789-abcdefghij",
        "app.jwt.ttl-hours=24"
})
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
public class InspectionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InspectionService inspectionService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String staffToken;
    private String customerToken;
    private static final Long STAFF_ID = 2L;
    private static final Long CUSTOMER_ID = 1L;

    @BeforeEach
    void setUp() {
        Role staffRole = new Role(2, "Staff", null);
        User staff = new User("Minh Tran", "staff@storagehub.dev", "0902345678",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", staffRole, UserStatus.ACTIVE);

        Role customerRole = new Role(1, "Customer", null);
        User customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", customerRole, UserStatus.ACTIVE);

        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        staffToken = jwtService.issueToken(STAFF_ID, RoleName.STAFF);
        customerToken = jwtService.issueToken(CUSTOMER_ID, RoleName.CUSTOMER);
    }

    @Test
    @DisplayName("Staff submits inspection for reservation successfully")
    void testStaffSubmitInspection() throws Exception {
        List<InspectionItemInput> items = List.of(
                new InspectionItemInput(InspectionItem.ACCESS_CARD, InspectionResult.OK, "Key card intact"),
                new InspectionItemInput(InspectionItem.PADLOCK, InspectionResult.OK, "Padlock returned"),
                new InspectionItemInput(InspectionItem.CLEANLINESS, InspectionResult.MINOR, "Minor swept debris"),
                new InspectionItemInput(InspectionItem.STRUCTURE, InspectionResult.OK, "No damage")
        );

        SubmitInspectionRequest req = new SubmitInspectionRequest(items, true, true, "Keys received");

        InspectionItemDto inspDto = new InspectionItemDto(1L, InspectionItem.ACCESS_CARD, InspectionResult.OK, "Key card intact", 2L, "Minh Tran", LocalDateTime.now());
        CheckoutTaskDetailDto responseDto = new CheckoutTaskDetailDto(
                10L,
                "TODO",
                100L,
                "BK-2026-0001",
                1L,
                "Lan Nguyen",
                "0901234567",
                5L,
                "S-03",
                LocalDate.of(2026, 11, 20),
                true,
                true,
                List.of(inspDto),
                false,
                List.of()
        );

        when(inspectionService.submitInspection(eq(100L), any(SubmitInspectionRequest.class), eq(STAFF_ID), eq("STAFF")))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/reservations/100/inspections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationCode").value("BK-2026-0001"))
                .andExpect(jsonPath("$.keyReturned").value(true))
                .andExpect(jsonPath("$.hasMajorDamage").value(false));
    }

    @Test
    @DisplayName("Customer is forbidden from submitting inspection")
    void testCustomerForbiddenFromSubmittingInspection() throws Exception {
        SubmitInspectionRequest req = new SubmitInspectionRequest(List.of(
                new InspectionItemInput(InspectionItem.ACCESS_CARD, InspectionResult.OK, null)
        ), true, true, null);

        mockMvc.perform(post("/api/v1/reservations/100/inspections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get checkout task detail by task ID")
    void testGetCheckoutTaskDetailByTaskId() throws Exception {
        CheckoutTaskDetailDto responseDto = new CheckoutTaskDetailDto(
                55L,
                "TODO",
                100L,
                "BK-2026-0001",
                1L,
                "Lan Nguyen",
                "0901234567",
                5L,
                "S-03",
                LocalDate.of(2026, 11, 20),
                true,
                true,
                List.of(),
                false,
                List.of()
        );

        when(inspectionService.getCheckoutTaskDetailByTaskId(eq(55L), eq(STAFF_ID), eq("STAFF")))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/checkout-tasks/55")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(55))
                .andExpect(jsonPath("$.unitCode").value("S-03"));
    }
}
