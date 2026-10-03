package com.storagehub.controller;

import com.storagehub.dto.FinalizeSettlementRequest;
import com.storagehub.dto.SettlementPreviewDto;
import com.storagehub.dto.SettlementReceiptDto;
import com.storagehub.entity.User;
import com.storagehub.service.SettlementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservations/{reservationId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/settlement-preview")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'CUSTOMER')")
    public ResponseEntity<SettlementPreviewDto> getSettlementPreview(
            @PathVariable Long reservationId,
            @RequestParam(required = false) BigDecimal damageFee,
            @RequestParam(required = false) String damageReason,
            @RequestParam(required = false) BigDecimal waiverAmount,
            @RequestParam(required = false) String waiverReason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkoutDate) {

        SettlementPreviewDto preview = settlementService.calculatePreview(
                reservationId, damageFee, damageReason, waiverAmount, waiverReason, checkoutDate);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/settlement-preview")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'CUSTOMER')")
    public ResponseEntity<SettlementPreviewDto> postSettlementPreview(
            @PathVariable Long reservationId,
            @RequestBody(required = false) FinalizeSettlementRequest request) {

        BigDecimal damageFee = (request != null) ? request.damageFee() : BigDecimal.ZERO;
        String damageReason = (request != null) ? request.damageReason() : null;
        BigDecimal waiverAmount = (request != null) ? request.waiverAmount() : BigDecimal.ZERO;
        String waiverReason = (request != null) ? request.waiverReason() : null;

        SettlementPreviewDto preview = settlementService.calculatePreview(
                reservationId, damageFee, damageReason, waiverAmount, waiverReason, null);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/settlement")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<SettlementReceiptDto> finalizeSettlement(
            @PathVariable Long reservationId,
            @RequestBody FinalizeSettlementRequest request,
            Authentication authentication) {

        Long staffUserId = resolveUserId(authentication);
        SettlementReceiptDto receipt = settlementService.finalizeSettlement(reservationId, request, staffUserId);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/settlement")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SettlementReceiptDto> getSettlement(
            @PathVariable Long reservationId) {

        SettlementReceiptDto receipt = settlementService.getSettlementByReservationId(reservationId);
        return ResponseEntity.ok(receipt);
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User u) {
            return u.getId();
        }
        return 2L; // Default staff ID
    }
}
