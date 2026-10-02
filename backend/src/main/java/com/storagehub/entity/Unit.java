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
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UnitID", nullable = false, updatable = false)
    private Long id;

    @Column(name = "Code", nullable = false, length = 20, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TypeID", nullable = false)
    private UnitType unitType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ZoneID", nullable = false)
    private Zone zone;

    @Column(name = "SizeM2", nullable = false, precision = 6, scale = 2)
    private BigDecimal sizeM2;

    @Column(name = "Floor", nullable = false)
    private Integer floor;

    @Column(name = "AccessType", nullable = false, length = 20)
    private String accessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private UnitStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MergedIntoID")
    private Unit mergedInto;

    protected Unit() {
    }

    public Unit(String code, UnitType unitType, Zone zone, BigDecimal sizeM2, Integer floor, String accessType, UnitStatus status) {
        this.code = code;
        this.unitType = unitType;
        this.zone = zone;
        this.sizeM2 = sizeM2;
        this.floor = floor;
        this.accessType = accessType;
        this.status = status;
    }

    public Unit(Long id, String code, UnitType unitType, Zone zone, BigDecimal sizeM2, Integer floor, String accessType, UnitStatus status) {
        this.id = id;
        this.code = code;
        this.unitType = unitType;
        this.zone = zone;
        this.sizeM2 = sizeM2;
        this.floor = floor;
        this.accessType = accessType;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public Zone getZone() {
        return zone;
    }

    public BigDecimal getSizeM2() {
        return sizeM2;
    }

    public Integer getFloor() {
        return floor;
    }

    public String getAccessType() {
        return accessType;
    }

    public UnitStatus getStatus() {
        return status;
    }

    public Unit getMergedInto() {
        return mergedInto;
    }

    public void setStatus(UnitStatus status) {
        this.status = status;
    }
}
