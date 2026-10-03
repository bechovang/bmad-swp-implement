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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SettlementID", nullable = false, updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReservationID", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ContractID")
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StaffID", nullable = false)
    private User staff;

    @Column(name = "DepositHeld", nullable = false, precision = 15, scale = 0)
    private BigDecimal depositHeld = BigDecimal.ZERO;

    @Column(name = "DamageFee", nullable = false, precision = 15, scale = 0)
    private BigDecimal damageFee = BigDecimal.ZERO;

    @Column(name = "DamageReason", length = 255)
    private String damageReason;

    @Column(name = "LateFee", nullable = false, precision = 15, scale = 0)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "RefundAmount", nullable = false, precision = 15, scale = 0)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "ExtraFee", nullable = false, precision = 15, scale = 0)
    private BigDecimal extraFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 30)
    private SettlementStatus status = SettlementStatus.FINALIZED;

    @Column(name = "ReceiptCode", nullable = false, length = 20, unique = true)
    private String receiptCode;

    @Column(name = "Notes", length = 500)
    private String notes;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Settlement() {
    }

    public Settlement(Reservation reservation, Contract contract, User staff,
                      BigDecimal depositHeld, BigDecimal damageFee, String damageReason,
                      BigDecimal lateFee, BigDecimal refundAmount, BigDecimal extraFee,
                      SettlementStatus status, String receiptCode, String notes) {
        this.reservation = reservation;
        this.contract = contract;
        this.staff = staff;
        this.depositHeld = depositHeld != null ? depositHeld : BigDecimal.ZERO;
        this.damageFee = damageFee != null ? damageFee : BigDecimal.ZERO;
        this.damageReason = damageReason;
        this.lateFee = lateFee != null ? lateFee : BigDecimal.ZERO;
        this.refundAmount = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        this.extraFee = extraFee != null ? extraFee : BigDecimal.ZERO;
        this.status = status != null ? status : SettlementStatus.FINALIZED;
        this.receiptCode = receiptCode;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public User getStaff() {
        return staff;
    }

    public void setStaff(User staff) {
        this.staff = staff;
    }

    public BigDecimal getDepositHeld() {
        return depositHeld;
    }

    public void setDepositHeld(BigDecimal depositHeld) {
        this.depositHeld = depositHeld;
    }

    public BigDecimal getDamageFee() {
        return damageFee;
    }

    public void setDamageFee(BigDecimal damageFee) {
        this.damageFee = damageFee;
    }

    public String getDamageReason() {
        return damageReason;
    }

    public void setDamageReason(String damageReason) {
        this.damageReason = damageReason;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public BigDecimal getExtraFee() {
        return extraFee;
    }

    public void setExtraFee(BigDecimal extraFee) {
        this.extraFee = extraFee;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
