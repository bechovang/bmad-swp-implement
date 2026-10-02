package com.storagehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "zones")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ZoneID", nullable = false, updatable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FacilityID", nullable = false)
    private Facility facility;

    @Column(name = "Code", nullable = false, length = 20)
    private String code;

    @Column(name = "Floor", nullable = false)
    private Integer floor;

    protected Zone() {
    }

    public Zone(Facility facility, String code, Integer floor) {
        this.facility = facility;
        this.code = code;
        this.floor = floor;
    }

    public Integer getId() {
        return id;
    }

    public Facility getFacility() {
        return facility;
    }

    public String getCode() {
        return code;
    }

    public Integer getFloor() {
        return floor;
    }
}
