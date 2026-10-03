package com.storagehub.controller;

import com.storagehub.dto.ContractDto;
import com.storagehub.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractDto> getContract(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        ContractDto contractDto = contractService.getContract(id, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(contractDto);
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<ContractDto> getContractByReservation(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        ContractDto contractDto = contractService.getLatestContractByReservationId(reservationId, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(contractDto);
    }

    @PostMapping("/{id}/print")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ContractDto> printContract(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long staffUserId = getCurrentUserId(authentication);
        ContractDto contractDto = contractService.markContractPrinted(id, staffUserId);
        return ResponseEntity.ok(contractDto);
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ContractDto> signContract(
            @PathVariable Long id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.storagehub.dto.SignContractRequest request,
            Authentication authentication
    ) {
        Long staffUserId = getCurrentUserId(authentication);
        ContractDto contractDto = contractService.signContract(id, request.signedPhotoUrl(), staffUserId);
        return ResponseEntity.ok(contractDto);
    }

    @GetMapping("/reservation/{reservationId}/chain")
    public ResponseEntity<java.util.List<ContractDto>> getContractChain(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isStaffOrAdmin = isStaffOrAdmin(authentication);
        java.util.List<ContractDto> chain = contractService.getContractChain(reservationId, currentUserId, isStaffOrAdmin);
        return ResponseEntity.ok(chain);
    }

    @PostMapping("/{id}/re-draft")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ContractDto> reDraftContract(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long staffUserId = getCurrentUserId(authentication);
        ContractDto contractDto = contractService.reDraftContract(id, staffUserId);
        return ResponseEntity.ok(contractDto);
    }

    @PostMapping("/{id}/expire")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ContractDto> expireContract(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) com.storagehub.dto.ExpireContractRequest request,
            Authentication authentication
    ) {
        Long staffUserId = getCurrentUserId(authentication);
        String reason = request != null ? request.reason() : null;
        ContractDto contractDto = contractService.expireContract(id, reason, staffUserId);
        return ResponseEntity.ok(contractDto);
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ContractDto> voidContract(
            @PathVariable Long id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.storagehub.dto.VoidContractRequest request,
            Authentication authentication
    ) {
        Long staffUserId = getCurrentUserId(authentication);
        ContractDto contractDto = contractService.voidContract(id, request.reason(), staffUserId);
        return ResponseEntity.ok(contractDto);
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
            if ("ROLE_STAFF".equals(auth) || "ROLE_FACILITY_MANAGER".equals(auth)
                    || "ROLE_BUSINESS_OPS".equals(auth)
                    || "ROLE_SYSTEM_ADMINISTRATOR".equals(auth) || "ROLE_ADMIN".equals(auth)) {
                return true;
            }
        }
        return false;
    }
}
