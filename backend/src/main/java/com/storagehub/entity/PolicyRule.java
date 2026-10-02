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
@Table(name = "policy_rules")
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RuleID", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PolicyID", nullable = false)
    private RentalPolicy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TypeID", nullable = false)
    private UnitType unitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "RuleType", nullable = false)
    private PolicyRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "SurchargeType")
    private SurchargeType surchargeType;

    @Column(name = "Value", nullable = false, precision = 15, scale = 0)
    private BigDecimal value;

    @Column(name = "Cap", precision = 15, scale = 0)
    private BigDecimal cap;

    protected PolicyRule() {
    }

    public PolicyRule(RentalPolicy policy, UnitType unitType, PolicyRuleType ruleType, SurchargeType surchargeType, BigDecimal value, BigDecimal cap) {
        this.policy = policy;
        this.unitType = unitType;
        this.ruleType = ruleType;
        this.surchargeType = surchargeType;
        this.value = value;
        this.cap = cap;
    }

    public Long getId() {
        return id;
    }

    public RentalPolicy getPolicy() {
        return policy;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public PolicyRuleType getRuleType() {
        return ruleType;
    }

    public SurchargeType getSurchargeType() {
        return surchargeType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getCap() {
        return cap;
    }
}
