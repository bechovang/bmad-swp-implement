package com.storagehub.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.CheckoutController;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.dto.CheckoutRequestDto;
import com.storagehub.dto.CreateCheckoutRequest;
import com.storagehub.entity.CheckoutRequestStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.CheckoutService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CheckoutController.class, properties = {
        "app.jwt.secret=checkout-controller-secret-0123456789-abcdefghij",
        "app.jwt.ttl-hours=24"
})
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
public class CheckoutControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String customerToken;
    private static final Long CUSTOMER_ID = 1L;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role(1, "Customer", null);
        User customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", customerRole, UserStatus.ACTIVE);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        customerToken = jwtService.issueToken(CUSTOMER_ID, RoleName.CUSTOMER);
    }

    @Test
    @DisplayName("POST /api/v1/reservations/{id}/checkout-request succeeds with 201 Created")
    void createCheckoutRequestSucceeds() throws Exception {
        LocalDate requestedDate = LocalDate.now().plusDays(5);
        CheckoutRequestDto mockDto = new CheckoutRequestDto(
                501L, 100L, "BK-1042", 10L, "S-3",
                requestedDate, CheckoutRequestStatus.PENDING, "Returning keys",
                false, false,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(checkoutService.requestCheckout(eq(100L), any(CreateCheckoutRequest.class), eq(CUSTOMER_ID), eq("CUSTOMER")))
                .thenReturn(mockDto);

        mockMvc.perform(post("/api/v1/reservations/100/checkout-request")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedDate\":\"" + requestedDate + "\",\"notes\":\"Returning keys\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(501L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestedDate").value(requestedDate.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/{id}/checkout-request returns 200 OK")
    void getCheckoutRequestSucceeds() throws Exception {
        LocalDate requestedDate = LocalDate.now().plusDays(5);
        CheckoutRequestDto mockDto = new CheckoutRequestDto(
                501L, 100L, "BK-1042", 10L, "S-3",
                requestedDate, CheckoutRequestStatus.PENDING, "Returning keys",
                false, false,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(checkoutService.getCheckoutRequest(eq(100L), eq(CUSTOMER_ID), eq("CUSTOMER")))
                .thenReturn(mockDto);

        mockMvc.perform(get("/api/v1/reservations/100/checkout-request")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
