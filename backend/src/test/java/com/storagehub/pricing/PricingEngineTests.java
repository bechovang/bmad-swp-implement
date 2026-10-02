package com.storagehub.pricing;

import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.entity.Facility;
import com.storagehub.entity.PolicyRule;
import com.storagehub.entity.PolicyRuleType;
import com.storagehub.entity.PolicyStatus;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.SurchargeType;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.UnitType;
import com.storagehub.entity.Zone;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.InvalidRequestException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.PolicyRuleRepository;
import com.storagehub.repository.RentalPolicyRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.service.PricingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingEngineTests {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private RentalPolicyRepository rentalPolicyRepository;

    @Mock
    private PolicyRuleRepository policyRuleRepository;

    @InjectMocks
    private PricingEngine pricingEngine;

    private RentalPolicy activePolicyV3;
    private UnitType typeS;
    private UnitType typeM;
    private Unit unitS3;
    private Unit unitM5;

    @BeforeEach
    void setUp() {
        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        Zone zoneA = new Zone(facility, "A", 1);
        Zone zoneB = new Zone(facility, "B", 2);

        typeS = new UnitType("S", "Small unit around 5 m2");
        typeM = new UnitType("M", "Medium unit around 8 m2");

        unitS3 = new Unit("S-3", typeS, zoneA, new BigDecimal("5.00"), 1, "PIN", UnitStatus.AVAILABLE);
        unitM5 = new Unit("M-5", typeM, zoneB, new BigDecimal("8.00"), 2, "QR", UnitStatus.AVAILABLE);

        activePolicyV3 = new RentalPolicy("v3", LocalDate.of(2026, 10, 1), PolicyStatus.ACTIVE);
    }

    @Test
    @DisplayName("S-3 standard 3 months: 345,000 * 3 = 1,035,000 VND, 10% refundable deposit = 103,500 VND")
    void calculatePricing_unitS3_3Months() {
        when(unitRepository.findByCodeWithDetails("S-3")).thenReturn(Optional.of(unitS3));
        when(rentalPolicyRepository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(eq(PolicyStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(Optional.of(activePolicyV3));

        PolicyRule rentRule = new PolicyRule(activePolicyV3, typeS, PolicyRuleType.RENT_RATE, null, new BigDecimal("345000"), null);
        PolicyRule depositRule = new PolicyRule(activePolicyV3, typeS, PolicyRuleType.DEPOSIT_RATE, null, new BigDecimal("10"), null);

        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(activePolicyV3.getId(), typeS.getId(), PolicyRuleType.RENT_RATE))
                .thenReturn(Optional.of(rentRule));
        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(activePolicyV3.getId(), typeS.getId(), PolicyRuleType.DEPOSIT_RATE))
                .thenReturn(Optional.of(depositRule));

        PricingBreakdownDto result = pricingEngine.calculatePricing("S-3", 3, LocalDate.of(2026, 10, 3));

        assertThat(result.unitCode()).isEqualTo("S-3");
        assertThat(result.durationMonths()).isEqualTo(3);
        assertThat(result.monthlyRate()).isEqualByComparingTo(new BigDecimal("345000"));
        assertThat(result.baseRent()).isEqualByComparingTo(new BigDecimal("1035000"));
        assertThat(result.totalRent()).isEqualByComparingTo(new BigDecimal("1035000"));
        assertThat(result.depositRate()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(result.depositAmount()).isEqualByComparingTo(new BigDecimal("103500"));
        assertThat(result.depositRefundable()).isTrue();
        assertThat(result.totalDueNow()).isEqualByComparingTo(new BigDecimal("103500"));
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.policyVersion()).isEqualTo("v3");
    }

    @Test
    @DisplayName("M-5 1 month: 690,000 * 1 = 690,000 VND, 10% refundable deposit = 69,000 VND")
    void calculatePricing_unitM5_1Month() {
        when(unitRepository.findByCodeWithDetails("M-5")).thenReturn(Optional.of(unitM5));
        when(rentalPolicyRepository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(eq(PolicyStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(Optional.of(activePolicyV3));

        PolicyRule rentRule = new PolicyRule(activePolicyV3, typeM, PolicyRuleType.RENT_RATE, null, new BigDecimal("690000"), null);
        PolicyRule depositRule = new PolicyRule(activePolicyV3, typeM, PolicyRuleType.DEPOSIT_RATE, null, new BigDecimal("10"), null);

        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(activePolicyV3.getId(), typeM.getId(), PolicyRuleType.RENT_RATE))
                .thenReturn(Optional.of(rentRule));
        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(activePolicyV3.getId(), typeM.getId(), PolicyRuleType.DEPOSIT_RATE))
                .thenReturn(Optional.of(depositRule));

        PricingBreakdownDto result = pricingEngine.calculatePricing("M-5", 1, LocalDate.of(2026, 10, 3));

        assertThat(result.unitCode()).isEqualTo("M-5");
        assertThat(result.durationMonths()).isEqualTo(1);
        assertThat(result.monthlyRate()).isEqualByComparingTo(new BigDecimal("690000"));
        assertThat(result.baseRent()).isEqualByComparingTo(new BigDecimal("690000"));
        assertThat(result.totalRent()).isEqualByComparingTo(new BigDecimal("690000"));
        assertThat(result.depositRate()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(result.depositAmount()).isEqualByComparingTo(new BigDecimal("69000"));
        assertThat(result.depositRefundable()).isTrue();
        assertThat(result.totalDueNow()).isEqualByComparingTo(new BigDecimal("69000"));
        assertThat(result.policyVersion()).isEqualTo("v3");
    }

    @Test
    @DisplayName("Throws InvalidRequestException when duration is 0 or negative")
    void calculatePricing_invalidDuration_throwsException() {
        assertThatThrownBy(() -> pricingEngine.calculatePricing("S-3", 0, LocalDate.now()))
                .isInstanceOf(InvalidRequestException.class)
                .satisfies(ex -> {
                    InvalidRequestException ire = (InvalidRequestException) ex;
                    assertThat(ire.fieldErrors()).anyMatch(fe -> fe.field().equals("durationMonths"));
                });

        assertThatThrownBy(() -> pricingEngine.calculatePricing("S-3", -2, LocalDate.now()))
                .isInstanceOf(InvalidRequestException.class)
                .satisfies(ex -> {
                    InvalidRequestException ire = (InvalidRequestException) ex;
                    assertThat(ire.fieldErrors()).anyMatch(fe -> fe.field().equals("durationMonths"));
                });
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when unit code is unknown")
    void calculatePricing_unknownUnit_throwsNotFound() {
        when(unitRepository.findByCodeWithDetails("UNKNOWN-99")).thenReturn(Optional.empty());
        when(unitRepository.findByCode("UNKNOWN-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingEngine.calculatePricing("UNKNOWN-99", 3, LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Unit UNKNOWN-99 not found");
    }

    @Test
    @DisplayName("Throws BusinessRuleException ACTIVE_POLICY_NOT_FOUND when no active policy found")
    void calculatePricing_noActivePolicy_throwsConflict() {
        when(unitRepository.findByCodeWithDetails("S-3")).thenReturn(Optional.of(unitS3));
        when(rentalPolicyRepository.findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(eq(PolicyStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pricingEngine.calculatePricing("S-3", 3, LocalDate.now()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("ACTIVE_POLICY_NOT_FOUND"));
    }
}
