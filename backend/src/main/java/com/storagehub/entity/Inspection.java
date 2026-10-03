package com.storagehub.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspections")
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InspectionID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReservationID", nullable = false)
    private Reservation reservation;

    @Column(name = "SettlementID")
    private Long settlementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Item", nullable = false)
    private InspectionItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "Result", nullable = false)
    private InspectionResult result;

    @Column(name = "Note", length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InspectorStaffID")
    private User inspectorStaff;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Inspection() {
    }

    public Inspection(Reservation reservation, InspectionItem item, InspectionResult result, String note, User inspectorStaff) {
        this.reservation = reservation;
        this.item = item;
        this.result = result;
        this.note = note;
        this.inspectorStaff = inspectorStaff;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Long getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(Long settlementId) {
        this.settlementId = settlementId;
    }

    public InspectionItem getItem() {
        return item;
    }

    public void setItem(InspectionItem item) {
        this.item = item;
    }

    public InspectionResult getResult() {
        return result;
    }

    public void setResult(InspectionResult result) {
        this.result = result;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getInspectorStaff() {
        return inspectorStaff;
    }

    public void setInspectorStaff(User inspectorStaff) {
        this.inspectorStaff = inspectorStaff;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
