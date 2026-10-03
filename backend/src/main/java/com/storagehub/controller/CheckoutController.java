package com.storagehub.controller;

import com.storagehub.dto.CheckoutRequestDto;
import com.storagehub.dto.CreateCheckoutRequest;
import com.storagehub.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations/{reservationId}/checkout-request")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<CheckoutRequestDto> requestCheckout(
            @PathVariable Long reservationId,
            @Valid @RequestBody CreateCheckoutRequest request,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        String role = getRole(authentication);
        CheckoutRequestDto dto = checkoutService.requestCheckout(
                reservationId,
                request,
                currentUserId,
                role
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<CheckoutRequestDto> getCheckoutRequest(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        String role = getRole(authentication);
        CheckoutRequestDto dto = checkoutService.getCheckoutRequest(
                reservationId,
                currentUserId,
                role
        );
        return ResponseEntity.ok(dto);
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

    private String getRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "CUSTOMER";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if (auth.startsWith("ROLE_")) {
                return auth.substring(5);
            }
        }
        return "CUSTOMER";
    }
}
