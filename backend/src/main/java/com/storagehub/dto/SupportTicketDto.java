package com.storagehub.dto;

import com.storagehub.entity.EscalationDecision;
import com.storagehub.entity.IncidentType;
import com.storagehub.entity.SupportTicketStatus;

import java.time.LocalDateTime;

public record SupportTicketDto(
        Long id,
        String code,
        Long customerId,
        String customerName,
        Long unitId,
        String unitCode,
        Long reservationId,
        String reservationCode,
        IncidentType incidentType,
        SupportTicketStatus status,
        String description,
        Long assignedStaffId,
        String assignedStaffName,
        String resolutionNote,
        String escalationNote,
        EscalationDecision managerDecision,
        String managerNote,
        Long relocatedToUnitId,
        String relocatedToUnitCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
