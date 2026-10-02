package com.storagehub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "facilities")
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FacilityID", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "Address", nullable = false, length = 255)
    private String address;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "Status", nullable = false)
    private Integer status;

    protected Facility() {
    }

    public Facility(String name, String address, String phone, Integer status) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getStatus() {
        return status;
    }
}
