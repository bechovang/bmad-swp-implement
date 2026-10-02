package com.storagehub.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.controller.PaymentController;
import com.storagehub.dto.CreatePaymentRequest;
import com.storagehub.dto.PaymentDto;
import com.storagehub.dto.PaymentResponseDto;
import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import com.storagehub.entity.PaymentStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.JwtService;
import com.storagehub.service.payment.PaymentService;
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

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class, properties =
        "app.jwt.secret=payment-test-secret-0123456789-abcdefghij")
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
public class PaymentControllerTests {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long STAFF_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private UserRepository userRepository;

    private String customerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role(1, "Customer", null);
        User customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        Role staffRole = new Role(2, "Staff", null);
        User staff = new User("Minh Tran", "minh@storagehub.dev", "0902345678", "hash", staffRole, UserStatus.ACTIVE);
        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));

        customerToken = jwtService.issueToken(CUSTOMER_ID, RoleName.CUSTOMER);
        staffToken = jwtService.issueToken(STAFF_ID, RoleName.STAFF);
    }

    @Test
    @DisplayName("POST /api/v1/payments/create: 201 Created on valid request")
    void testCreatePayment_success() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(100L, PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, 103500L);
        PaymentResponseDto responseDto = new PaymentResponseDto(
                10L, 1727932800L, 103500L, PaymentStatus.PENDING, PaymentMethod.PAYOS, PaymentPurpose.DEPOSIT,
                "https://pay.payos.vn/checkout", "mock-qr", Instant.now().plusSeconds(900)
        );

        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(CUSTOMER_ID)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/payments/create")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(10))
                .andExpect(jsonPath("$.orderCode").value(1727932800))
                .andExpect(jsonPath("$.amount").value(103500))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://pay.payos.vn/checkout"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook: 200 OK without JWT (public)")
    void testWebhook_unauthenticated_success() throws Exception {
        String payload = """
                {
                  "code": "00",
                  "desc": "success",
                  "data": {
                    "orderCode": 1727932800,
                    "amount": 103500
                  },
                  "signature": "valid-signature"
                }
                """;

        doNothing().when(paymentService).processPayOsWebhook(any());

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.message").value("Webhook processed successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{id}/confirm-cash: 200 OK when called by STAFF")
    void testConfirmCash_byStaff_success() throws Exception {
        PaymentDto dto = new PaymentDto(
                10L, "RC-1727932800", CUSTOMER_ID, 100L, 1727932800L,
                PaymentPurpose.DEPOSIT, PaymentMethod.CASH, 103500L, PaymentStatus.SUCCEEDED
        );

        when(paymentService.confirmCashPayment(eq(10L), eq(STAFF_ID))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/payments/10/confirm-cash")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.method").value("CASH"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{id}/confirm-cash: 403 Forbidden when called by CUSTOMER")
    void testConfirmCash_byCustomer_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/payments/10/confirm-cash")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id}: 200 OK when found")
    void testGetPayment_success() throws Exception {
        PaymentDto dto = new PaymentDto(
                10L, "RC-1727932800", CUSTOMER_ID, 100L, 1727932800L,
                PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, 103500L, PaymentStatus.SUCCEEDED
        );

        when(paymentService.getPayment(eq(10L), eq(CUSTOMER_ID), eq(false))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/payments/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.receiptCode").value("RC-1727932800"))
                .andExpect(jsonPath("$.amount").value(103500));
    }
}
