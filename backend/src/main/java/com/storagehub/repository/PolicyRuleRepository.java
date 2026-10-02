package com.storagehub.repository;

import com.storagehub.entity.PolicyRule;
import com.storagehub.entity.PolicyRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {

    List<PolicyRule> findByPolicy_Id(Integer policyId);

    List<PolicyRule> findByPolicy_IdAndUnitType_Id(Integer policyId, Integer typeId);

    Optional<PolicyRule> findByPolicy_IdAndUnitType_IdAndRuleType(Integer policyId, Integer typeId, PolicyRuleType ruleType);
}
