package com.storagehub.dto;

import com.storagehub.entity.EscalationDecision;
import com.storagehub.entity.IncidentType;
import com.storagehub.entity.SupportTicketStatus;

import java.time.LocalDateTime;

public class EscalationDto {

    private Long id;
    private Long ticketId;
    private String ticketCode;
    private Long customerId;
    private String customerName;
    private Long unitId;
    private String unitCode;
    private Long reservationId;
    private String reservationCode;
    private IncidentType incidentType;
    private SupportTicketStatus ticketStatus;
    private String ticketDescription;
    private Long escalatedByStaffId;
    private String escalatedByStaffName;
    private String escalationNote;
    private Long managerId;
    private String managerName;
    private EscalationDecision decision;
    private String managerNote;
    private Long relocatedToUnitId;
    private String relocatedToUnitCode;
    private String newAccessCode;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public EscalationDto() {
    }

    public EscalationDto(Long id, Long ticketId, String ticketCode, Long customerId, String customerName,
                         Long unitId, String unitCode, Long reservationId, String reservationCode,
                         IncidentType incidentType, SupportTicketStatus ticketStatus, String ticketDescription,
                         Long escalatedByStaffId, String escalatedByStaffName, String escalationNote,
                         Long managerId, String managerName, EscalationDecision decision, String managerNote,
                         Long relocatedToUnitId, String relocatedToUnitCode, String newAccessCode,
                         LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.customerId = customerId;
        this.customerName = customerName;
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.reservationId = reservationId;
        this.reservationCode = reservationCode;
        this.incidentType = incidentType;
        this.ticketStatus = ticketStatus;
        this.ticketDescription = ticketDescription;
        this.escalatedByStaffId = escalatedByStaffId;
        this.escalatedByStaffName = escalatedByStaffName;
        this.escalationNote = escalationNote;
        this.managerId = managerId;
        this.managerName = managerName;
        this.decision = decision;
        this.managerNote = managerNote;
        this.relocatedToUnitId = relocatedToUnitId;
        this.relocatedToUnitCode = relocatedToUnitCode;
        this.newAccessCode = newAccessCode;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public IncidentType getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(IncidentType incidentType) {
        this.incidentType = incidentType;
    }

    public SupportTicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(SupportTicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public String getTicketDescription() {
        return ticketDescription;
    }

    public void setTicketDescription(String ticketDescription) {
        this.ticketDescription = ticketDescription;
    }

    public Long getEscalatedByStaffId() {
        return escalatedByStaffId;
    }

    public void setEscalatedByStaffId(Long escalatedByStaffId) {
        this.escalatedByStaffId = escalatedByStaffId;
    }

    public String getEscalatedByStaffName() {
        return escalatedByStaffName;
    }

    public void setEscalatedByStaffName(String escalatedByStaffName) {
        this.escalatedByStaffName = escalatedByStaffName;
    }

    public String getEscalationNote() {
        return escalationNote;
    }

    public void setEscalationNote(String escalationNote) {
        this.escalationNote = escalationNote;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public EscalationDecision getDecision() {
        return decision;
    }

    public void setDecision(EscalationDecision decision) {
        this.decision = decision;
    }

    public String getManagerNote() {
        return managerNote;
    }

    public void setManagerNote(String managerNote) {
        this.managerNote = managerNote;
    }

    public Long getRelocatedToUnitId() {
        return relocatedToUnitId;
    }

    public void setRelocatedToUnitId(Long relocatedToUnitId) {
        this.relocatedToUnitId = relocatedToUnitId;
    }

    public String getRelocatedToUnitCode() {
        return relocatedToUnitCode;
    }

    public void setRelocatedToUnitCode(String relocatedToUnitCode) {
        this.relocatedToUnitCode = relocatedToUnitCode;
    }

    public String getNewAccessCode() {
        return newAccessCode;
    }

    public void setNewAccessCode(String newAccessCode) {
        this.newAccessCode = newAccessCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
