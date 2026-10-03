package com.storagehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "escalations")
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EscalationID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TicketID", nullable = false, unique = true)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EscalatedByStaffID", nullable = false)
    private User escalatedByStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ManagerID")
    private User manager;

    @Column(name = "Note", nullable = false, columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "Decision", nullable = false)
    private EscalationDecision decision = EscalationDecision.PENDING;

    public Escalation() {
    }

    public Escalation(SupportTicket ticket, User escalatedByStaff, User manager, String note) {
        this.ticket = ticket;
        this.escalatedByStaff = escalatedByStaff;
        this.manager = manager;
        this.note = note;
        this.decision = EscalationDecision.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SupportTicket getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
    }

    public User getEscalatedByStaff() {
        return escalatedByStaff;
    }

    public void setEscalatedByStaff(User escalatedByStaff) {
        this.escalatedByStaff = escalatedByStaff;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public EscalationDecision getDecision() {
        return decision;
    }

    public void setDecision(EscalationDecision decision) {
        this.decision = decision;
    }
}
