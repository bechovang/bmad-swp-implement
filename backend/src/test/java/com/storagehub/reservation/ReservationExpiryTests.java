package com.storagehub.reservation;

import com.storagehub.entity.Action;
import com.storagehub.entity.Contract;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Facility;
import com.storagehub.entity.RentalPolicy;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.UnitType;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.entity.Zone;
import com.storagehub.repository.ContractRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.ReservationExpiryService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExpiryTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private LogService logService;

    @Mock
    private NotificationService notificationService;

    private ReservationExpiryService expiryService;

    private User customer;
    private Unit unit;
    private Contract contract;

    @BeforeEach
    void setUp() {
        expiryService = new ReservationExpiryService(
                reservationRepository,
                unitRepository,
                contractRepository,
                logService,
                notificationService
        );

        Role role = new Role(1, "Customer", null);
        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", role, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        UnitType unitType = new UnitType(1, "S", "Small unit around 5 m2");
        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        Zone zone = new Zone(facility, "A", 1);
        unit = new Unit(10L, "S-3", unitType, zone, new BigDecimal("5.00"), 1, "PIN", UnitStatus.RESERVED);

        RentalPolicy policy = new RentalPolicy("v3", LocalDate.of(2026, 1, 1), null);
        contract = new Contract("CT-2026-0001", null, policy, "{}", null, ContractStatus.DRAFT, null, 1);
    }

    @Test
    @DisplayName("checkAndExpireReservation: past RESERVED reservation expires, frees unit to AVAILABLE, closes contract, logs audit, and sends notification")
    void pastReservedReservation_expiresAtomically() {
        LocalDate yesterdayICT = LocalDate.now(ReservationExpiryService.ICT_ZONE).minusDays(1);
        Reservation pastReservation = new Reservation(
                "BK-2026-0001", customer, unit, yesterdayICT, yesterdayICT.plusMonths(1),
                new BigDecimal("103500"), null, ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(pastReservation, "id", 100L);

        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(contract));

        boolean expired = expiryService.checkAndExpireReservation(pastReservation);

        assertThat(expired).isTrue();
        assertThat(pastReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.AVAILABLE);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CLOSED);

        verify(reservationRepository).save(pastReservation);
        verify(unitRepository).save(unit);
        verify(contractRepository).save(contract);
        verify(logService).append(
                eq(1L),
                eq(EntityType.RESERVATION),
                eq(100L),
                eq(Action.STATUS_CHANGE),
                eq("RESERVED"),
                eq("EXPIRED"),
                eq("Reservation expired due to no-show on check-in date")
        );
        verify(notificationService).send(
                eq(1L),
                eq("RESERVATION_EXPIRED"),
                eq("Reservation BK-2026-0001 expired due to no-show. Deposit forfeited."),
                eq("/rentals/100")
        );
    }

    @Test
    @DisplayName("checkAndExpireReservation: today or future start date is NOT expired")
    void futureOrTodayReservation_notExpired() {
        LocalDate todayICT = LocalDate.now(ReservationExpiryService.ICT_ZONE);
        Reservation todayReservation = new Reservation(
                "BK-2026-0002", customer, unit, todayICT, todayICT.plusMonths(1),
                new BigDecimal("103500"), null, ReservationStatus.RESERVED
        );

        Reservation futureReservation = new Reservation(
                "BK-2026-0003", customer, unit, todayICT.plusDays(5), todayICT.plusMonths(1),
                new BigDecimal("103500"), null, ReservationStatus.RESERVED
        );

        assertThat(expiryService.checkAndExpireReservation(todayReservation)).isFalse();
        assertThat(todayReservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.RESERVED);

        assertThat(expiryService.checkAndExpireReservation(futureReservation)).isFalse();
        assertThat(futureReservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);

        verify(reservationRepository, never()).save(any());
        verify(unitRepository, never()).save(any());
        verify(logService, never()).append(any(), any(), any(), any(), any(), any(), any());
        verify(notificationService, never()).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkAndExpireReservation: already CHECKED_IN or CLOSED or CANCELLED is untouched")
    void nonReservedStatuses_notExpired() {
        LocalDate pastDate = LocalDate.now(ReservationExpiryService.ICT_ZONE).minusDays(5);
        Reservation checkedIn = new Reservation(
                "BK-2026-0004", customer, unit, pastDate, pastDate.plusMonths(1),
                new BigDecimal("103500"), "123456", ReservationStatus.CHECKED_IN
        );
        Reservation closed = new Reservation(
                "BK-2026-0005", customer, unit, pastDate, pastDate.plusMonths(1),
                new BigDecimal("103500"), null, ReservationStatus.CLOSED
        );

        assertThat(expiryService.checkAndExpireReservation(checkedIn)).isFalse();
        assertThat(checkedIn.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);

        assertThat(expiryService.checkAndExpireReservation(closed)).isFalse();
        assertThat(closed.getStatus()).isEqualTo(ReservationStatus.CLOSED);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("expireReservation idempotency: second call on already EXPIRED reservation is no-op")
    void idempotency_secondCall_noOp() {
        LocalDate yesterdayICT = LocalDate.now(ReservationExpiryService.ICT_ZONE).minusDays(1);
        Reservation reservation = new Reservation(
                "BK-2026-0001", customer, unit, yesterdayICT, yesterdayICT.plusMonths(1),
                new BigDecimal("103500"), null, ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(reservation, "id", 100L);
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(contract));

        // First call
        expiryService.expireReservation(reservation);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        // Second call
        expiryService.expireReservation(reservation);

        // Verification: side effects executed strictly once
        verify(reservationRepository, times(1)).save(reservation);
        verify(unitRepository, times(1)).save(unit);
        verify(contractRepository, times(1)).save(contract);
        verify(logService, times(1)).append(any(), any(), any(), any(), any(), any(), any());
        verify(notificationService, times(1)).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("expirePastReservationsForCustomer: finds and expires customer's past RESERVED reservations")
    void expirePastReservationsForCustomer_success() {
        LocalDate yesterdayICT = LocalDate.now(ReservationExpiryService.ICT_ZONE).minusDays(2);
        Reservation pastRes = new Reservation(
                "BK-2026-0001", customer, unit, yesterdayICT, yesterdayICT.plusMonths(1),
                new BigDecimal("103500"), null, ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(pastRes, "id", 101L);

        when(reservationRepository.findByCustomerIdAndStatusAndStartDateBefore(eq(1L), eq(ReservationStatus.RESERVED), any()))
                .thenReturn(List.of(pastRes));
        when(contractRepository.findLatestByReservationId(101L)).thenReturn(Optional.of(contract));

        expiryService.expirePastReservationsForCustomer(1L);

        assertThat(pastRes.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(reservationRepository).save(pastRes);
    }

    @Test
    @DisplayName("expireAllPastReservedReservations: finds all past RESERVED across system and releases units")
    void expireAllPastReservedReservations_success() {
        LocalDate yesterdayICT = LocalDate.now(ReservationExpiryService.ICT_ZONE).minusDays(1);
        Reservation pastRes1 = new Reservation(
                "BK-2026-0001", customer, unit, yesterdayICT, yesterdayICT.plusMonths(1),
                new BigDecimal("103500"), null, ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(pastRes1, "id", 101L);

        when(reservationRepository.findByStatusAndStartDateBefore(eq(ReservationStatus.RESERVED), any()))
                .thenReturn(List.of(pastRes1));
        when(contractRepository.findLatestByReservationId(101L)).thenReturn(Optional.of(contract));

        expiryService.expireAllPastReservedReservations();

        assertThat(pastRes1.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.AVAILABLE);
        verify(unitRepository).save(unit);
    }
}
