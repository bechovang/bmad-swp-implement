package com.storagehub.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rental_policies")
public class RentalPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PolicyID", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "Version", nullable = false, length = 20, unique = true)
    private String version;

    @Column(name = "EffectiveDate", nullable = false)
    private LocalDate effectiveDate;

    @Convert(converter = PolicyStatusConverter.class)
    @Column(name = "Status", nullable = false)
    private PolicyStatus status;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PolicyRule> rules = new ArrayList<>();

    protected RentalPolicy() {
    }

    public RentalPolicy(String version, LocalDate effectiveDate, PolicyStatus status) {
        this.version = version;
        this.effectiveDate = effectiveDate;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public List<PolicyRule> getRules() {
        return rules;
    }
}
