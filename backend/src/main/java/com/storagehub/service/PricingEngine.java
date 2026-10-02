package com.storagehub.service;

import com.storagehub.dto.FieldError;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.dto.SurchargeItemDto;
import com.storagehub.entity.PolicyRule;
import com.storagehub.entity.PolicyRuleType;
import com.storagehub.entity.PolicyStatus;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Unit;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.InvalidRequestException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.PolicyRuleRepository;
import com.storagehub.repository.RentalPolicyRepository;
import com.storagehub.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Single source of truth for all pricing calculations (AD-11, AD-7).
 * Reads active RentalPolicy rules and computes base rent, surcharges,
 * and refundable deposit amounts in BigDecimal VND.
 */
@Service
@Transactional(readOnly = true)
public class PricingEngine {

    private final UnitRepository unitRepository;
    private final RentalPolicyRepository rentalPolicyRepository;
    private final PolicyRuleRepository policyRuleRepository;

    public PricingEngine(UnitRepository unitRepository,
                         RentalPolicyRepository rentalPolicyRepository,
                         PolicyRuleRepository policyRuleRepository) {
        this.unitRepository = unitRepository;
        this.rentalPolicyRepository = rentalPolicyRepository;
        this.policyRuleRepository = policyRuleRepository;
    }

    /**
     * Resolves the active rental policy for the specified date.
     * Picks status = ACTIVE (1) and effectiveDate <= queryDate, ordered by effectiveDate DESC.
     */
    public RentalPolicy resolveActivePolicy(LocalDate queryDate) {
        LocalDate date = (queryDate != null) ? queryDate : LocalDate.now();
        return rentalPolicyRepository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                PolicyStatus.ACTIVE, date)
                .orElseThrow(() -> new BusinessRuleException("ACTIVE_POLICY_NOT_FOUND",
                        "No active rental policy found for date: " + date));
    }

    /**
     * Calculates the full pricing breakdown for a unit code and duration.
     */
    public PricingBreakdownDto calculatePricing(String unitCode, int durationMonths, LocalDate startDate) {
        if (durationMonths < 1) {
            throw new InvalidRequestException(List.of(
                    new FieldError("durationMonths", "Duration must be at least 1 month")
            ));
        }

        Unit unit = unitRepository.findByCodeWithDetails(unitCode)
                .or(() -> unitRepository.findByCode(unitCode))
                .orElseThrow(() -> new ResourceNotFoundException("Unit " + unitCode + " not found"));

        return calculatePricing(unit, durationMonths, startDate);
    }

    /**
     * Calculates the pricing breakdown for a loaded Unit entity.
     */
    public PricingBreakdownDto calculatePricing(Unit unit, int durationMonths, LocalDate startDate) {
        if (unit == null || unit.getUnitType() == null) {
            throw new ResourceNotFoundException("Unit or unit type not found");
        }

        if (durationMonths < 1) {
            throw new InvalidRequestException(List.of(
                    new FieldError("durationMonths", "Duration must be at least 1 month")
            ));
        }

        LocalDate effectiveDate = (startDate != null) ? startDate : LocalDate.now();
        RentalPolicy activePolicy = resolveActivePolicy(effectiveDate);

        Integer typeId = unit.getUnitType().getId();

        PolicyRule rentRule = policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(
                activePolicy.getId(), typeId, PolicyRuleType.RENT_RATE)
                .orElseThrow(() -> new BusinessRuleException("POLICY_RULE_NOT_FOUND",
                        "No RENT_RATE rule found for unit type " + unit.getUnitType().getName()));

        PolicyRule depositRule = policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(
                activePolicy.getId(), typeId, PolicyRuleType.DEPOSIT_RATE)
                .orElseThrow(() -> new BusinessRuleException("POLICY_RULE_NOT_FOUND",
                        "No DEPOSIT_RATE rule found for unit type " + unit.getUnitType().getName()));

        BigDecimal monthlyRate = rentRule.getValue().setScale(0, RoundingMode.HALF_UP);
        BigDecimal depositRate = depositRule.getValue();

        BigDecimal baseRent = monthlyRate.multiply(BigDecimal.valueOf(durationMonths)).setScale(0, RoundingMode.HALF_UP);
        List<SurchargeItemDto> surcharges = List.of();
        BigDecimal totalRent = baseRent;

        BigDecimal depositAmount = baseRent.multiply(depositRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        boolean depositRefundable = true;
        BigDecimal totalDueNow = depositAmount;
        String currency = "VND";
        String policyVersion = activePolicy.getVersion();

        return new PricingBreakdownDto(
                unit.getCode(),
                durationMonths,
                monthlyRate,
                baseRent,
                surcharges,
                totalRent,
                depositRate.setScale(0, RoundingMode.HALF_UP),
                depositAmount,
                depositRefundable,
                totalDueNow,
                currency,
                policyVersion
        );
    }
}
