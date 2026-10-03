package com.storagehub.checkout;

import com.storagehub.dto.FinalizeSettlementRequest;
import com.storagehub.dto.SettlementPreviewDto;
import com.storagehub.dto.SettlementReceiptDto;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.*;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.SettlementService;
import com.storagehub.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementTests {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private SettlementService settlementService;

    private User staffUser;
    private User customerUser;
    private Unit unit;
    private Reservation reservation;
    private Contract contract;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                reservationRepository,
                settlementRepository,
                paymentRepository,
                contractRepository,
                unitRepository,
                userRepository,
                taskRepository,
                taskService,
                logService,
                notificationService
        );

        Role staffRole = new Role(2, "Staff", null);
        staffUser = new User("Staff Alex", "staff@storagehub.vn", "0900000002", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staffUser, "id", 2L);

        Role customerRole = new Role(1, "Customer", null);
        customerUser = new User("Lan Nguyen", "customer@gmail.com", "0900000003", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customerUser, "id", 3L);

        Facility facility = new Facility("Tan Binh Depot", "45 NV Troi", "0900000000", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("Standard", "Standard Lock");

        unit = new Unit("S-3", unitType, zone, BigDecimal.valueOf(10), 1, "PIN", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit, "id", 101L);

        reservation = new Reservation(
                "BK-2026-00871",
                customerUser,
                unit,
                LocalDate.now().minusMonths(1),
                LocalDate.now(),
                BigDecimal.valueOf(172500),
                "123456",
                ReservationStatus.CHECKOUT_REQUESTED
        );
        reservation.setMonthlyRate(BigDecimal.valueOf(690000));
        reservation.setBaseRent(BigDecimal.valueOf(690000));
        ReflectionTestUtils.setField(reservation, "id", 871L);

        RentalPolicy policy = new RentalPolicy("POL-2026-01", LocalDate.now().minusYears(1), PolicyStatus.ACTIVE);
        ReflectionTestUtils.setField(policy, "id", 1);

        contract = new Contract(
                "CT-2026-00871",
                reservation,
                policy,
                "{}",
                null,
                ContractStatus.ACTIVE,
                null,
                1
        );
        ReflectionTestUtils.setField(contract, "id", 501L);
    }

    @Test
    @DisplayName("Settlement preview with 172.500 deposit and 40.000 damage yields 132.500 refund")
    void testSettlementPreviewExactDemoArithmetic() {
        when(reservationRepository.findById(871L)).thenReturn(Optional.of(reservation));

        SettlementPreviewDto preview = settlementService.calculatePreview(
                871L,
                BigDecimal.valueOf(40000),
                "Scratched door panel and missing key fob",
                LocalDate.now()
        );

        assertThat(preview.depositHeld()).isEqualByComparingTo(BigDecimal.valueOf(172500));
        assertThat(preview.damageFee()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(preview.lateFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(preview.totalCharges()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(preview.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(132500));
        assertThat(preview.extraFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(preview.extraFeeRequired()).isFalse();
        assertThat(preview.damageReasonRequired()).isTrue();
        assertThat(preview.canFinalize()).isTrue();
    }

    @Test
    @DisplayName("Settlement preview requires damageReason when damageFee > 0")
    void testSettlementPreviewRequiresReason() {
        when(reservationRepository.findById(871L)).thenReturn(Optional.of(reservation));

        SettlementPreviewDto preview = settlementService.calculatePreview(
                871L,
                BigDecimal.valueOf(40000),
                null, // No reason
                LocalDate.now()
        );

        assertThat(preview.damageReasonRequired()).isTrue();
        assertThat(preview.canFinalize()).isFalse();
    }

    @Test
    @DisplayName("Settlement preview auto-computes LATE_FEE when checkout is past end date")
    void testSettlementPreviewAutoComputesLateFee() {
        reservation.setEndDate(LocalDate.now().minusDays(3)); // 3 days late
        when(reservationRepository.findById(871L)).thenReturn(Optional.of(reservation));

        SettlementPreviewDto preview = settlementService.calculatePreview(
                871L,
                BigDecimal.valueOf(0),
                null,
                LocalDate.now()
        );

        assertThat(preview.daysLate()).isEqualTo(3);
        // dailyRate = 690,000 / 30 = 23,000; 3 * 23,000 = 69,000
        assertThat(preview.lateFee()).isEqualByComparingTo(BigDecimal.valueOf(69000));
        assertThat(preview.totalCharges()).isEqualByComparingTo(BigDecimal.valueOf(69000));
        assertThat(preview.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(103500)); // 172,500 - 69,000
    }

    @Test
    @DisplayName("Settlement preview calculates Extra Fee when charges exceed deposit")
    void testSettlementPreviewExtraFeeExceedsDeposit() {
        when(reservationRepository.findById(871L)).thenReturn(Optional.of(reservation));

        SettlementPreviewDto preview = settlementService.calculatePreview(
                871L,
                BigDecimal.valueOf(200000), // Damage exceeds 172,500 deposit
                "Severe structural damage to rolling shutter",
                LocalDate.now()
        );

        assertThat(preview.refundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(preview.extraFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(27500)); // 200,000 - 172,500
        assertThat(preview.extraFeeRequired()).isTrue();
        assertThat(preview.extraFeePaid()).isFalse();
        assertThat(preview.canFinalize()).isFalse();
    }

    @Test
    @DisplayName("Finalizing settlement blocks if damageFee > 0 and damageReason is missing")
    void testFinalizeBlocksMissingReason() {
        when(reservationRepository.findById(871L)).thenReturn(Optional.of(reservation));

        FinalizeSettlementRequest request = new FinalizeSettlementRequest(
                BigDecimal.valueOf(40000),
                "   ", // Blank reason
                BigDecimal.ZERO,
                null,
                false,
                null
        );

        assertThatThrownBy(() -> settlementService.finalizeSettlement(871L, request, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("damage reason is mandatory");
    }

    @Test
    @DisplayName("Finalizing settlement blocks if extraFee is unpaid and no cash collected")
    void testFinalizeBlocksUnpaidExtraFee() {
        when(reservationRepository.findById(871L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffUser));
        when(paymentRepository.findByReservationId(871L)).thenReturn(Collections.emptyList());

        FinalizeSettlementRequest request = new FinalizeSettlementRequest(
                BigDecimal.valueOf(200000),
                "Severe structural damage",
                BigDecimal.ZERO,
                "PAYOS",
                false, // not paid cash
                null
        );

        assertThatThrownBy(() -> settlementService.finalizeSettlement(871L, request, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("extra fee of");
    }

    @Test
    @DisplayName("Finalizing settlement executes atomic closure: CLOSED, PREPARING, CLEANING task, Receipt")
    void testFinalizeSettlementAtomicSuccess() {
        when(reservationRepository.findById(871L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffUser));
        when(contractRepository.findLatestByReservationId(871L)).thenReturn(Optional.of(contract));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> {
            Settlement s = i.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 999L);
            return s;
        });

        Task checkoutTask = new Task(TaskType.CHECKOUT, "BK-2026-00871", staffUser, LocalDate.now(), TaskStatus.IN_PROGRESS);
        when(taskRepository.findByRefCodeAndType("BK-2026-00871", TaskType.CHECKOUT)).thenReturn(Optional.of(checkoutTask));

        FinalizeSettlementRequest request = new FinalizeSettlementRequest(
                BigDecimal.valueOf(40000),
                "Lost access card badge and scratched door paint",
                BigDecimal.ZERO,
                null,
                false,
                "Customer acknowledged damage deduction."
        );

        SettlementReceiptDto receipt = settlementService.finalizeSettlement(871L, request, 2L);

        // Verify status transitions
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CLOSED);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.CLOSED);
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.PREPARING);
        assertThat(checkoutTask.getStatus()).isEqualTo(TaskStatus.DONE);

        // Verify TaskService called for cleaning task
        verify(taskService).createCleaningTask(unit);

        // Verify audit logs & notifications
        verify(logService).append(eq(2L), eq(EntityType.SETTLEMENT), eq(999L), eq(Action.DAMAGE_CHARGE), any(), eq("40000"), any());
        verify(notificationService).send(eq(3L), eq("SETTLEMENT_FINALIZED"), any(), any());

        // Verify receipt data
        assertThat(receipt.receiptCode()).startsWith("STL-");
        assertThat(receipt.refundAmount()).isEqualByComparingTo(BigDecimal.valueOf(132500));
        assertThat(receipt.damageFee()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(receipt.depositHeld()).isEqualByComparingTo(BigDecimal.valueOf(172500));
    }
}
