package com.storagehub.controller;

import com.storagehub.dto.CreatePaymentRequest;
import com.storagehub.dto.PaymentDto;
import com.storagehub.dto.PaymentResponseDto;
import com.storagehub.service.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        PaymentResponseDto response = paymentService.createPayment(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receiveWebhook(@RequestBody String rawPayload) {
        paymentService.processPayOsWebhook(rawPayload);
        return ResponseEntity.ok(Map.of("code", "00", "message", "Webhook processed successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        PaymentDto paymentDto = paymentService.getPayment(id, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(paymentDto);
    }

    @PostMapping("/{id}/confirm-cash")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentDto> confirmCashPayment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        PaymentDto paymentDto = paymentService.confirmCashPayment(id, currentUserId);
        return ResponseEntity.ok(paymentDto);
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("User is not authenticated");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Invalid authenticated user id: " + authentication.getName());
        }
    }

    private boolean isStaffOrAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if ("ROLE_STAFF".equals(auth) || "ROLE_FACILITY_MANAGER".equals(auth) || "ROLE_ADMIN".equals(auth)) {
                return true;
            }
        }
        return false;
    }
}
