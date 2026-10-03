package com.storagehub.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkout_requests")
public class CheckoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReservationID", nullable = false)
    private Reservation reservation;

    @Column(name = "RequestedDate", nullable = false)
    private LocalDate requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private CheckoutRequestStatus status;

    @Column(name = "Notes", length = 500)
    private String notes;

    @Column(name = "KeyReturned", nullable = false)
    private Boolean keyReturned = false;

    @Column(name = "UnitEmptied", nullable = false)
    private Boolean unitEmptied = false;

    @CreationTimestamp
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    public CheckoutRequest() {
    }

    public CheckoutRequest(Reservation reservation, LocalDate requestedDate, CheckoutRequestStatus status, String notes) {
        this.reservation = reservation;
        this.requestedDate = requestedDate;
        this.status = status;
        this.notes = notes;
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

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public CheckoutRequestStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutRequestStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getKeyReturned() {
        return keyReturned;
    }

    public void setKeyReturned(Boolean keyReturned) {
        this.keyReturned = keyReturned;
    }

    public Boolean getUnitEmptied() {
        return unitEmptied;
    }

    public void setUnitEmptied(Boolean unitEmptied) {
        this.unitEmptied = unitEmptied;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
