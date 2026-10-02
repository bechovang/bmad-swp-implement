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

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ContractID", nullable = false, updatable = false)
    private Long id;

    @Column(name = "Code", nullable = false, length = 20, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ReservationID", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PolicyID", nullable = false)
    private RentalPolicy policy;

    @Column(name = "ContentSnapshot", nullable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    @Column(name = "SignedPhotoUrl", length = 255)
    private String signedPhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private ContractStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupersedesContractID")
    private Contract supersedesContract;

    @Column(name = "IsLatest", nullable = false)
    private Integer isLatest = 1;

    @Column(name = "LatestReservationID", insertable = false, updatable = false)
    private Long latestReservationId;

    protected Contract() {
    }

    public Contract(String code, Reservation reservation, RentalPolicy policy, String contentSnapshot,
                    String signedPhotoUrl, ContractStatus status, Contract supersedesContract, Integer isLatest) {
        this.code = code;
        this.reservation = reservation;
        this.policy = policy;
        this.contentSnapshot = contentSnapshot;
        this.signedPhotoUrl = signedPhotoUrl;
        this.status = status;
        this.supersedesContract = supersedesContract;
        this.isLatest = isLatest != null ? isLatest : 1;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public RentalPolicy getPolicy() {
        return policy;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public void setContentSnapshot(String contentSnapshot) {
        this.contentSnapshot = contentSnapshot;
    }

    public String getSignedPhotoUrl() {
        return signedPhotoUrl;
    }

    public void setSignedPhotoUrl(String signedPhotoUrl) {
        this.signedPhotoUrl = signedPhotoUrl;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public Contract getSupersedesContract() {
        return supersedesContract;
    }

    public void setSupersedesContract(Contract supersedesContract) {
        this.supersedesContract = supersedesContract;
    }

    public Integer getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Integer isLatest) {
        this.isLatest = isLatest;
    }

    public Long getLatestReservationId() {
        return latestReservationId;
    }
}
