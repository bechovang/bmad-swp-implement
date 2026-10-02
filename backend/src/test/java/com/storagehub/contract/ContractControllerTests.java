package com.storagehub.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ContractController;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.dto.ContractContentSnapshotDto;
import com.storagehub.dto.ContractDto;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.ContractService;
import com.storagehub.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContractController.class, properties =
        "app.jwt.secret=contract-test-secret-0123456789-abcdefghij")
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
class ContractControllerTests {

    private static final Long CUSTOMER_ID = 101L;
    private static final Long STAFF_ID = 102L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private UserRepository userRepository;

    private String customerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role(1, "Customer", null);
        User customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", customerRole, UserStatus.ACTIVE);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        Role staffRole = new Role(2, "Staff", null);
        User staff = new User("Minh Tran", "minh@storagehub.dev", "0902345678",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", staffRole, UserStatus.ACTIVE);
        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));

        customerToken = jwtService.issueToken(CUSTOMER_ID, RoleName.CUSTOMER);
        staffToken = jwtService.issueToken(STAFF_ID, RoleName.STAFF);
    }

    @Test
    @DisplayName("GET /api/v1/contracts/{id}: 200 OK when requested by contract owner")
    void getContract_success() throws Exception {
        ContractContentSnapshotDto snapshot = new ContractContentSnapshotDto(
                "CT-2026-0001", "BK-2026-0001", "S-3",
                345000L, 1035000L, 1035000L, 103500L, 10L, 3, "v3", "VND",
                LocalDate.of(2026, 10, 5), LocalDate.of(2027, 1, 5),
                "Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "Tan Binh Depot", "45 Nguyen Van Troi", "A", 1, 5.0
        );

        ContractDto dto = new ContractDto(
                1L, "CT-2026-0001", 100L, "BK-2026-0001",
                1, "v3", "{}", snapshot, null, ContractStatus.DRAFT, null, 1
        );

        when(contractService.getContract(eq(1L), eq(CUSTOMER_ID), anyBoolean())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/contracts/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("CT-2026-0001"))
                .andExpect(jsonPath("$.reservationId").value(100))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isLatest").value(1))
                .andExpect(jsonPath("$.policyVersion").value("v3"))
                .andExpect(jsonPath("$.snapshot.unitCode").value("S-3"))
                .andExpect(jsonPath("$.snapshot.monthlyRate").value(345000));
    }

    @Test
    @DisplayName("GET /api/v1/contracts/{id}: 401 Unauthorized without token")
    void getContract_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /api/v1/contracts/{id}: 403 Forbidden when customer does not own contract")
    void getContract_forbidden() throws Exception {
        when(contractService.getContract(eq(99L), eq(CUSTOMER_ID), anyBoolean()))
                .thenThrow(new AccessDeniedException("Access denied to contract 99"));

        mockMvc.perform(get("/api/v1/contracts/99")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/v1/contracts/{id}: 404 NOT_FOUND when contract does not exist")
    void getContract_notFound() throws Exception {
        when(contractService.getContract(eq(999L), eq(CUSTOMER_ID), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Contract not found: 999"));

        mockMvc.perform(get("/api/v1/contracts/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/contracts/reservation/{reservationId}: 200 OK")
    void getContractByReservation_success() throws Exception {
        ContractDto dto = new ContractDto(
                1L, "CT-2026-0001", 100L, "BK-2026-0001",
                1, "v3", "{}", null, null, ContractStatus.DRAFT, null, 1
        );

        when(contractService.getLatestContractByReservationId(eq(100L), eq(CUSTOMER_ID), anyBoolean()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/contracts/reservation/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("CT-2026-0001"))
                .andExpect(jsonPath("$.reservationId").value(100));
    }

    @Test
    @DisplayName("POST /api/v1/contracts/{id}/re-draft: 200 OK when called by Staff")
    void reDraftContract_staff_success() throws Exception {
        ContractDto dto = new ContractDto(
                2L, "CT-2026-0001-R1", 100L, "BK-2026-0001",
                1, "v3", "{}", null, null, ContractStatus.DRAFT, 1L, 1
        );

        when(contractService.reDraftContract(eq(1L), eq(STAFF_ID))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/contracts/1/re-draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("CT-2026-0001-R1"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.supersedesContractId").value(1))
                .andExpect(jsonPath("$.isLatest").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/contracts/{id}/re-draft: 403 Forbidden when called by Customer")
    void reDraftContract_customer_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/contracts/1/re-draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/contracts/{id}/re-draft: 409 Conflict when contract not in DRAFT")
    void reDraftContract_conflict() throws Exception {
        when(contractService.reDraftContract(eq(1L), eq(STAFF_ID)))
                .thenThrow(new BusinessRuleException("INVALID_CONTRACT_STATUS", "Only DRAFT contracts can be re-drafted"));

        mockMvc.perform(post("/api/v1/contracts/1/re-draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_CONTRACT_STATUS"))
                .andExpect(jsonPath("$.message").value("Only DRAFT contracts can be re-drafted"));
    }
}
