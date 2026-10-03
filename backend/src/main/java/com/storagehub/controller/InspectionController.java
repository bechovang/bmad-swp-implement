package com.storagehub.controller;

import com.storagehub.dto.CheckoutTaskDetailDto;
import com.storagehub.dto.SubmitInspectionRequest;
import com.storagehub.service.InspectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @PostMapping("/reservations/{reservationId}/inspections")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<CheckoutTaskDetailDto> submitInspection(
            @PathVariable Long reservationId,
            @Valid @RequestBody SubmitInspectionRequest request,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        String role = extractRole(authentication);
        CheckoutTaskDetailDto result = inspectionService.submitInspection(reservationId, request, currentUserId, role);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/reservations/{reservationId}/inspections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutTaskDetailDto> getInspectionsByReservation(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        String role = extractRole(authentication);
        CheckoutTaskDetailDto result = inspectionService.getCheckoutTaskDetailByReservation(reservationId, currentUserId, role);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/checkout-tasks/{taskId}")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<CheckoutTaskDetailDto> getCheckoutTaskDetail(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        String role = extractRole(authentication);
        CheckoutTaskDetailDto result = inspectionService.getCheckoutTaskDetailByTaskId(taskId, currentUserId, role);
        return ResponseEntity.ok(result);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if (auth.startsWith("ROLE_")) {
                return auth.substring(5);
            }
        }
        return "";
    }
}
