package com.storagehub.controller;

import com.storagehub.dto.EscalationDto;
import com.storagehub.dto.SeverityDecisionRequest;
import com.storagehub.service.TicketService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/escalations")
public class EscalationController {

    private final TicketService ticketService;

    public EscalationController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * List all escalations for Facility Manager inbox (Story 5.3).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<List<EscalationDto>> getEscalations(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        String role = getRole(authentication);
        List<EscalationDto> list = ticketService.getEscalations(currentUserId, role);
        return ResponseEntity.ok(list);
    }

    /**
     * Get single escalation details by ID (Story 5.3).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<EscalationDto> getEscalationById(@PathVariable("id") Long id,
                                                           Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        String role = getRole(authentication);
        EscalationDto dto = ticketService.getEscalationById(id, currentUserId, role);
        return ResponseEntity.ok(dto);
    }

    /**
     * Facility Manager makes severity decision on escalation (Story 5.3).
     */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<EscalationDto> processSeverityDecision(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody SeverityDecisionRequest request,
                                                                 Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        EscalationDto dto = ticketService.processSeverityDecision(id, request, currentUserId);
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
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            String authority = auth.getAuthority();
            if (authority.startsWith("ROLE_")) {
                return authority.substring(5);
            }
        }
        return "CUSTOMER";
    }
}
