package com.storagehub.controller;

import com.storagehub.dto.CreateSupportTicketRequest;
import com.storagehub.dto.SupportTicketDto;
import com.storagehub.entity.SupportTicketStatus;
import com.storagehub.service.TicketService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support-tickets")
public class SupportTicketController {

    private final TicketService ticketService;

    public SupportTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Create a new support ticket (Customer only).
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SupportTicketDto> createTicket(@Valid @RequestBody CreateSupportTicketRequest request,
                                                         Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        SupportTicketDto dto = ticketService.createTicket(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * List support tickets (Customer views own; Staff / Manager views all / filtered).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SupportTicketDto>> getTickets(@RequestParam(value = "status", required = false) SupportTicketStatus status,
                                                            @RequestParam(value = "unitCode", required = false) String unitCode,
                                                            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        String role = getRole(authentication);
        List<SupportTicketDto> list = ticketService.getTickets(currentUserId, role, status, unitCode);
        return ResponseEntity.ok(list);
    }

    /**
     * Get single support ticket by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupportTicketDto> getTicketById(@PathVariable("id") Long id,
                                                          Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        String role = getRole(authentication);
        SupportTicketDto dto = ticketService.getTicketById(id, currentUserId, role);
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
