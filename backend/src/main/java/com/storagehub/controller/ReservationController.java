package com.storagehub.controller;

import com.storagehub.dto.CreateReservationRequest;
import com.storagehub.dto.ReservationDto;
import com.storagehub.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationDto> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        ReservationDto reservationDto = reservationService.createReservation(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationDto);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationDto>> getMyReservations(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        List<ReservationDto> reservations = reservationService.getMyReservations(currentUserId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto> getReservation(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        ReservationDto reservationDto = reservationService.getReservation(id, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(reservationDto);
    }

    @GetMapping("/{id}/access-code")
    public ResponseEntity<com.storagehub.dto.AccessCodeResponseDto> getReservationAccessCode(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        com.storagehub.dto.AccessCodeResponseDto accessCodeDto = reservationService.getReservationAccessCode(id, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(accessCodeDto);
    }

    @GetMapping("/{id}/extension-boundary")
    public ResponseEntity<com.storagehub.dto.ExtensionBoundaryDto> getExtensionBoundary(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        com.storagehub.dto.ExtensionBoundaryDto boundaryDto = reservationService.getExtensionBoundary(id, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(boundaryDto);
    }

    @PostMapping("/{id}/extension-quote")
    public ResponseEntity<com.storagehub.dto.ExtensionQuoteDto> getExtensionQuote(
            @PathVariable Long id,
            @Valid @RequestBody com.storagehub.dto.ExtensionQuoteRequest request,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        com.storagehub.dto.ExtensionQuoteDto quoteDto = reservationService.getExtensionQuote(id, request.newEndDate(), currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(quoteDto);
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
