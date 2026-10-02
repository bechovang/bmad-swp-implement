package com.storagehub.unit;

import com.storagehub.dto.BrowseUnitsResponse;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.entity.Facility;
import com.storagehub.entity.PolicyRule;
import com.storagehub.entity.PolicyRuleType;
import com.storagehub.entity.PolicyStatus;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.UnitType;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.entity.Zone;
import com.storagehub.repository.PolicyRuleRepository;
import com.storagehub.repository.RentalPolicyRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.service.PricingEngine;
import com.storagehub.service.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitBrowseTests {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private PricingEngine pricingEngine;

    @Mock
    private PolicyRuleRepository policyRuleRepository;

    @Mock
    private ReservationRepository reservationRepository;

    private UnitService unitService;

    private RentalPolicy activePolicy;
    private UnitType typeS;
    private UnitType typeM;
    private Facility facility;
    private Zone zoneA;
    private Zone zoneB;

    private Unit unitS3;
    private Unit unitM2;
    private Unit unitM5;

    @BeforeEach
    void setUp() {
        unitService = new UnitService(unitRepository, pricingEngine, policyRuleRepository, reservationRepository);

        activePolicy = new RentalPolicy("v3", LocalDate.of(2026, 10, 1), PolicyStatus.ACTIVE);
        facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        zoneA = new Zone(facility, "A", 1);
        zoneB = new Zone(facility, "B", 2);

        typeS = new UnitType(1, "S", "Small unit around 5 m2");
        typeM = new UnitType(2, "M", "Medium unit around 8 m2");

        unitS3 = new Unit(1L, "S-3", typeS, zoneA, new BigDecimal("5.00"), 1, "PIN", UnitStatus.PREPARING);
        unitM2 = new Unit(2L, "M-2", typeM, zoneB, new BigDecimal("8.00"), 1, "QR", UnitStatus.MAINTENANCE);
        unitM5 = new Unit(3L, "M-5", typeM, zoneB, new BigDecimal("8.00"), 2, "QR", UnitStatus.AVAILABLE);

        when(pricingEngine.resolveActivePolicy(any())).thenReturn(activePolicy);
    }

    @Test
    @DisplayName("browseUnits excludes MAINTENANCE (M-2) and includes S-3 (buffer) and M-5 (available)")
    void browseUnits_seedExclusionsAndBuffer_correct() {
        when(unitRepository.findAllWithDetails()).thenReturn(List.of(unitS3, unitM2, unitM5));

        // Buffer rule for type S: 2 days
        PolicyRule bufferRuleS = new PolicyRule(activePolicy, typeS, PolicyRuleType.TURNOVER_BUFFER, null, new BigDecimal("2"), null);
        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(any(), eq(typeS.getId()), eq(PolicyRuleType.TURNOVER_BUFFER)))
                .thenReturn(Optional.of(bufferRuleS));

        // Buffer rule for type M: 2 days
        PolicyRule bufferRuleM = new PolicyRule(activePolicy, typeM, PolicyRuleType.TURNOVER_BUFFER, null, new BigDecimal("2"), null);
        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(any(), eq(typeM.getId()), eq(PolicyRuleType.TURNOVER_BUFFER)))
                .thenReturn(Optional.of(bufferRuleM));

        // S-3 has ended reservation ending today -> +2 days buffer = today + 2 days
        LocalDate today = LocalDate.now();
        LocalDate expectedS3Date = today.plusDays(2);
        User customer = new User("Lan", "lan@test.com", "0901", "hash", null, UserStatus.ACTIVE);
        Reservation pastRes = new Reservation("BK-1042", customer, unitS3, today.minusDays(10), today, new BigDecimal("103500"), "1234", ReservationStatus.CLOSED);
        when(reservationRepository.findByUnitIdOrderByEndDateDesc(unitS3.getId())).thenReturn(List.of(pastRes));

        // PricingEngine mock for pricing
        PricingBreakdownDto pricingS3 = new PricingBreakdownDto("S-3", 1, new BigDecimal("345000"), new BigDecimal("345000"), List.of(), new BigDecimal("345000"), new BigDecimal("10"), new BigDecimal("34500"), true, new BigDecimal("34500"), "VND", "v3");
        PricingBreakdownDto pricingM5 = new PricingBreakdownDto("M-5", 1, new BigDecimal("690000"), new BigDecimal("690000"), List.of(), new BigDecimal("690000"), new BigDecimal("10"), new BigDecimal("69000"), true, new BigDecimal("69000"), "VND", "v3");
        when(pricingEngine.calculatePricing(eq(unitS3), eq(1), any())).thenReturn(pricingS3);
        when(pricingEngine.calculatePricing(eq(unitM5), eq(1), any())).thenReturn(pricingM5);

        BrowseUnitsResponse res = unitService.browseUnits(null, null, null, 1);

        // M-2 must be excluded, total bookable units in catalog = 2
        assertThat(res.items()).hasSize(2);
        assertThat(res.totalAvailable()).isEqualTo(2);
        assertThat(res.totalUnits()).isEqualTo(2);

        // S-3 is buffer
        var s3Dto = res.items().stream().filter(u -> u.code().equals("S-3")).findFirst().orElseThrow();
        assertThat(s3Dto.isInCleaningBuffer()).isTrue();
        assertThat(s3Dto.isImmediatelyAvailable()).isFalse();
        assertThat(s3Dto.availableFromDate()).isEqualTo(expectedS3Date);
        assertThat(s3Dto.availabilityStatus()).contains("cleaning buffer");
        assertThat(s3Dto.monthlyRate()).isEqualByComparingTo(new BigDecimal("345000"));

        // M-5 is available
        var m5Dto = res.items().stream().filter(u -> u.code().equals("M-5")).findFirst().orElseThrow();
        assertThat(m5Dto.isInCleaningBuffer()).isFalse();
        assertThat(m5Dto.isImmediatelyAvailable()).isTrue();
        assertThat(m5Dto.availabilityStatus()).isEqualTo("Available now");
        assertThat(m5Dto.monthlyRate()).isEqualByComparingTo(new BigDecimal("690000"));
    }

    @Test
    @DisplayName("browseUnits filters by requested startDate")
    void browseUnits_filterStartDate_filtersOutLaterUnits() {
        when(unitRepository.findAllWithDetails()).thenReturn(List.of(unitS3, unitM5));

        PolicyRule bufferRuleS = new PolicyRule(activePolicy, typeS, PolicyRuleType.TURNOVER_BUFFER, null, new BigDecimal("2"), null);
        when(policyRuleRepository.findByPolicy_IdAndUnitType_IdAndRuleType(any(), eq(typeS.getId()), eq(PolicyRuleType.TURNOVER_BUFFER)))
                .thenReturn(Optional.of(bufferRuleS));

        LocalDate today = LocalDate.now();
        // S-3 available today + 2 days
        Reservation pastRes = new Reservation("BK-1042", null, unitS3, today.minusDays(10), today, new BigDecimal("103500"), "1234", ReservationStatus.CLOSED);
        when(reservationRepository.findByUnitIdOrderByEndDateDesc(unitS3.getId())).thenReturn(List.of(pastRes));

        PricingBreakdownDto pricingM5 = new PricingBreakdownDto("M-5", 1, new BigDecimal("690000"), new BigDecimal("690000"), List.of(), new BigDecimal("690000"), new BigDecimal("10"), new BigDecimal("69000"), true, new BigDecimal("69000"), "VND", "v3");
        when(pricingEngine.calculatePricing(eq(unitM5), eq(1), any())).thenReturn(pricingM5);

        // Query with startDate = today -> S-3 (available today+2) should be filtered out
        BrowseUnitsResponse res = unitService.browseUnits(null, null, today, 1);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).code()).isEqualTo("M-5");
        assertThat(res.totalUnits()).isEqualTo(2);
    }

    @Test
    @DisplayName("browseUnits filters by type S returns only S-3")
    void browseUnits_filterTypeS_returnsOnlyS() {
        when(unitRepository.findAllWithDetails()).thenReturn(List.of(unitS3, unitM5));

        PricingBreakdownDto pricingS3 = new PricingBreakdownDto("S-3", 1, new BigDecimal("345000"), new BigDecimal("345000"), List.of(), new BigDecimal("345000"), new BigDecimal("10"), new BigDecimal("34500"), true, new BigDecimal("34500"), "VND", "v3");
        when(pricingEngine.calculatePricing(eq(unitS3), eq(1), any())).thenReturn(pricingS3);

        BrowseUnitsResponse res = unitService.browseUnits("S", null, null, 1);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).code()).isEqualTo("S-3");
    }

    @Test
    @DisplayName("browseUnits when no units match returns empty list and 0 totalAvailable, but preserves totalUnits")
    void browseUnits_noMatch_returnsEmpty() {
        when(unitRepository.findAllWithDetails()).thenReturn(List.of(unitS3, unitM5));

        BrowseUnitsResponse res = unitService.browseUnits("Locker", null, null, 1);

        assertThat(res.items()).isEmpty();
        assertThat(res.totalAvailable()).isEqualTo(0);
        assertThat(res.totalUnits()).isEqualTo(2);
    }
}
