package com.storagehub.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.ExtensionBoundaryDto;
import com.storagehub.dto.ExtensionQuoteDto;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.*;
import com.storagehub.service.LogService;
import com.storagehub.service.PricingEngine;
import com.storagehub.service.ReservationExpiryService;
import com.storagehub.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RentalExtensionBoundaryTests {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private PolicyRuleRepository policyRuleRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PricingEngine pricingEngine;
    @Mock
    private LogService logService;
    @Mock
    private ReservationExpiryService reservationExpiryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReservationService reservationService;

    private User customer;
    private Unit unit;
    private Reservation currentRental;
    private Contract contract;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                unitRepository,
                userRepository,
                contractRepository,
                policyRuleRepository,
                paymentRepository,
                pricingEngine,
                logService,
                reservationExpiryService,
                objectMapper
        );

        Role customerRole = new Role(1, "Customer", "Customer role");
        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        ReflectionTestUtils.setField(facility, "id", 1);

        Zone zone = new Zone(facility, "A", 1);
        ReflectionTestUtils.setField(zone, "id", 1);

        UnitType unitType = new UnitType("M", "Medium unit");
        ReflectionTestUtils.setField(unitType, "id", 2);

        unit = new Unit("M-2", unitType, zone, new BigDecimal("8.0"), 1, "QR", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit, "id", 2L);

        currentRental = new Reservation(
                "BK-2026-0002",
                customer,
                unit,
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 15),
                new BigDecimal("69000"),
                "482913",
                ReservationStatus.CHECKED_IN
        );
        ReflectionTestUtils.setField(currentRental, "id", 200L);

        RentalPolicy policy = new RentalPolicy("v3", LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE);
        ReflectionTestUtils.setField(policy, "id", 1);

        contract = new Contract(
                "CT-2026-0002",
                currentRental,
                policy,
                "{\"code\":\"CT-2026-0002\",\"monthlyRate\":690000,\"depositRate\":10,\"baseRent\":690000,\"totalRent\":690000,\"depositAmount\":69000,\"currency\":\"VND\",\"policyVersion\":\"v3\"}",
                null,
                ContractStatus.ACTIVE,
                null,
                1
        );
        ReflectionTestUtils.setField(contract, "id", 20L);
    }

    @Test
    @DisplayName("Story 4.1: Extension boundary returns conflict dates and max checkout date when next booking exists")
    void testGetExtensionBoundary_withUpcomingReservation() {
        when(reservationRepository.findById(200L)).thenReturn(Optional.of(currentRental));

        // Upcoming reservation on M-2 starting Oct 19, 2026
        Reservation nextBooking = new Reservation(
                "BK-2026-0045",
                customer,
                unit,
                LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 11, 19),
                new BigDecimal("69000"),
                null,
                ReservationStatus.RESERVED
        );
        when(reservationRepository.findUpcomingReservationsForUnit(eq(2L), eq(200L), any(), any()))
                .thenReturn(List.of(nextBooking));

        ExtensionBoundaryDto boundary = reservationService.getExtensionBoundary(200L, 1L, false);

        assertThat(boundary).isNotNull();
        assertThat(boundary.unitCode()).isEqualTo("M-2");
        assertThat(boundary.currentEndDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(boundary.conflictStartDate()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(boundary.latestPossibleCheckoutDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(boundary.isExtendable()).isTrue();
        assertThat(boundary.message()).contains("M-2 has a reservation starting 2026-10-19. Latest possible checkout is 2026-10-18.");
    }

    @Test
    @DisplayName("Story 4.1: Submitting date within conflict zone throws 409 EXTENSION_DATE_CONFLICT")
    void testGetExtensionQuote_conflictingDate_throws409() {
        when(reservationRepository.findById(200L)).thenReturn(Optional.of(currentRental));

        Reservation nextBooking = new Reservation(
                "BK-2026-0045",
                customer,
                unit,
                LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 11, 19),
                new BigDecimal("69000"),
                null,
                ReservationStatus.RESERVED
        );
        when(reservationRepository.findUpcomingReservationsForUnit(eq(2L), eq(200L), any(), any()))
                .thenReturn(List.of(nextBooking));

        LocalDate conflictingDate = LocalDate.of(2026, 10, 20);

        assertThatThrownBy(() -> reservationService.getExtensionQuote(200L, conflictingDate, 1L, false))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("EXTENSION_DATE_CONFLICT");
                    assertThat(bre.getMessage()).contains("Can't extend to 2026-10-20 — M-2 has a reservation starting 2026-10-19. Latest possible checkout is 2026-10-18.");
                });
    }

    @Test
    @DisplayName("Story 4.1 & 4.2: Extension quote calculates 2-line financial breakdown (rent + deposit top-up AD-11)")
    void testGetExtensionQuote_validDate_calculatesBreakdown() {
        when(reservationRepository.findById(200L)).thenReturn(Optional.of(currentRental));
        when(contractRepository.findLatestByReservationId(200L)).thenReturn(Optional.of(contract));
        when(reservationRepository.findUpcomingReservationsForUnit(eq(2L), eq(200L), any(), any()))
                .thenReturn(List.of());

        // Extend by 1 full month: from 2026-10-15 to 2026-11-15 (31 days)
        LocalDate newDate = LocalDate.of(2026, 11, 15);

        ExtensionQuoteDto quote = reservationService.getExtensionQuote(200L, newDate, 1L, false);

        assertThat(quote).isNotNull();
        assertThat(quote.unitCode()).isEqualTo("M-2");
        assertThat(quote.currentEndDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(quote.newEndDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(quote.monthlyRate()).isEqualTo(690000L);
        assertThat(quote.additionalRent()).isGreaterThan(0L);
        assertThat(quote.currentHeldDeposit()).isEqualTo(69000L);
        assertThat(quote.totalFee()).isEqualTo(quote.additionalRent() + quote.depositTopUp());
    }

    @Test
    @DisplayName("Story 4.1: Rentals past end date are not extendable")
    void testGetExtensionBoundary_pastEndDate_notExtendable() {
        Reservation pastRental = new Reservation(
                "BK-2026-0001",
                customer,
                unit,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                new BigDecimal("69000"),
                "123456",
                ReservationStatus.CHECKED_IN
        );
        ReflectionTestUtils.setField(pastRental, "id", 100L);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(pastRental));

        ExtensionBoundaryDto boundary = reservationService.getExtensionBoundary(100L, 1L, false);

        assertThat(boundary).isNotNull();
        assertThat(boundary.isExtendable()).isFalse();
    }
}
