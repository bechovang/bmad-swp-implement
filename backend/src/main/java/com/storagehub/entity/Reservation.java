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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReservationID", nullable = false, updatable = false)
    private Long id;

    @Column(name = "Code", nullable = false, length = 20, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CustomerID", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UnitID", nullable = false)
    private Unit unit;

    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "EndDate", nullable = false)
    private LocalDate endDate;

    @Column(name = "DepositAmount", nullable = false, precision = 15, scale = 0)
    private BigDecimal depositAmount;

    @Column(name = "AccessCode", length = 10)
    private String accessCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private ReservationStatus status;

    protected Reservation() {
    }

    public Reservation(String code, User customer, Unit unit, LocalDate startDate, LocalDate endDate, BigDecimal depositAmount, String accessCode, ReservationStatus status) {
        this.code = code;
        this.customer = customer;
        this.unit = unit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.depositAmount = depositAmount;
        this.accessCode = accessCode;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public User getCustomer() {
        return customer;
    }

    public Unit getUnit() {
        return unit;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
