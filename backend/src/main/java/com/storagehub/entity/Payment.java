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

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID", nullable = false, updatable = false)
    private Long id;

    @Column(name = "ReceiptCode", nullable = false, length = 20, unique = true)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PayerID", nullable = false)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReservationID")
    private Reservation reservation;

    @Column(name = "ExtensionID")
    private Long extensionId;

    @Column(name = "SettlementID")
    private Long settlementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Purpose", nullable = false)
    private PaymentPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "Method", nullable = false)
    private PaymentMethod method;

    @Column(name = "Amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private PaymentStatus status;

    protected Payment() {
    }

    public Payment(String receiptCode, User payer, Reservation reservation, Long extensionId,
                   Long settlementId, PaymentPurpose purpose, PaymentMethod method,
                   BigDecimal amount, PaymentStatus status) {
        this.receiptCode = receiptCode;
        this.payer = payer;
        this.reservation = reservation;
        this.extensionId = extensionId;
        this.settlementId = settlementId;
        this.purpose = purpose;
        this.method = method;
        this.amount = amount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public User getPayer() {
        return payer;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Long getExtensionId() {
        return extensionId;
    }

    public Long getSettlementId() {
        return settlementId;
    }

    public PaymentPurpose getPurpose() {
        return purpose;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
