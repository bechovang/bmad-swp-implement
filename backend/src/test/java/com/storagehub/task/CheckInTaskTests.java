package com.storagehub.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.CheckInValidationDto;
import com.storagehub.dto.PricingBreakdownDto;
import com.storagehub.entity.Facility;
import com.storagehub.entity.Payment;
import com.storagehub.entity.PaymentMethod;
import com.storagehub.entity.PaymentPurpose;
import com.storagehub.entity.PaymentStatus;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.Task;
import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.TaskType;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.UnitType;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.entity.Zone;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ContractRepository;
import com.storagehub.repository.PaymentRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.TaskRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.PricingEngine;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CheckInTaskTests {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private PricingEngine pricingEngine;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TaskService taskService;

    private User staff;
    private User customer;
    private Unit unit;
    private Reservation reservation;
    private Task checkInTask;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                userRepository,
                reservationRepository,
                unitRepository,
                paymentRepository,
                contractRepository,
                pricingEngine,
                logService,
                notificationService,
                objectMapper
        );

        Role staffRole = new Role(2, "Staff", "Staff role");
        Role customerRole = new Role(1, "Customer", "Customer role");

        staff = new User("Minh Tran", "staff@storagehub.dev", "0902345678", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 2L);

        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("S", "Small unit");
        ReflectionTestUtils.setField(unitType, "id", 2);

        unit = new Unit("S-3", unitType, zone, BigDecimal.valueOf(5.0), 1, "PIN", UnitStatus.RESERVED);
        ReflectionTestUtils.setField(unit, "id", 1L);

        reservation = new Reservation(
                "BK-1042",
                customer,
                unit,
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 11, 3),
                BigDecimal.valueOf(103500),
                null,
                ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(reservation, "id", 100L);

        checkInTask = new Task(TaskType.CHECK_IN, "BK-1042", staff, LocalDate.of(2026, 10, 3), TaskStatus.TODO);
        ReflectionTestUtils.setField(checkInTask, "id", 10L);
    }

    @Test
    @DisplayName("validateCheckInReservation with valid BK- code returns 2-line financial breakdown")
    void testValidateCheckInReservation_valid() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));

        Payment depositPayment = new Payment(
                "RC-1727932800101", customer, reservation, null, null,
                PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, BigDecimal.valueOf(103500), PaymentStatus.SUCCEEDED
        );
        when(paymentRepository.findByReservationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(depositPayment));

        PricingBreakdownDto pricing = new PricingBreakdownDto(
                "S-3", 1, BigDecimal.valueOf(1035000), BigDecimal.valueOf(1035000),
                List.of(), BigDecimal.valueOf(1035000), BigDecimal.valueOf(10),
                BigDecimal.valueOf(103500), true, BigDecimal.valueOf(103500), "VND", "v3"
        );
        when(pricingEngine.calculatePricing(any(Unit.class), anyInt(), any())).thenReturn(pricing);

        CheckInValidationDto result = taskService.validateCheckInReservation(10L, "BK-1042");

        assertThat(result).isNotNull();
        assertThat(result.valid()).isTrue();
        assertThat(result.reservationCode()).isEqualTo("BK-1042");
        assertThat(result.customerName()).isEqualTo("Lan Nguyen");
        assertThat(result.unitCode()).isEqualTo("S-3");
        assertThat(result.depositAmountPaid()).isEqualTo(103500L);
        assertThat(result.totalRentDue()).isEqualTo(1035000L);
        assertThat(result.depositReceiptCode()).isEqualTo("RC-1727932800101");
        assertThat(result.rentPaid()).isFalse();
        assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("validateCheckInReservation throws ResourceNotFoundException for unknown BK- code")
    void testValidateCheckInReservation_notFound() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.validateCheckInReservation(10L, "BK-9999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation BK-9999 not found");
    }

    @Test
    @DisplayName("validateCheckInReservation halts with DEPOSIT_UNPAID when reservation is PENDING_PAYMENT")
    void testValidateCheckInReservation_unpaidDepositStatus() {
        reservation.setStatus(ReservationStatus.PENDING_PAYMENT);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> taskService.validateCheckInReservation(10L, "BK-1042"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Deposit has not been paid");
    }

    @Test
    @DisplayName("validateCheckInReservation halts with DEPOSIT_UNPAID when deposit payment record is missing")
    void testValidateCheckInReservation_missingDepositPaymentRecord() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> taskService.validateCheckInReservation(10L, "BK-1042"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Deposit has not been paid");
    }

    @Test
    @DisplayName("validateCheckInReservation halts with RESERVATION_EXPIRED when reservation is EXPIRED")
    void testValidateCheckInReservation_expired() {
        reservation.setStatus(ReservationStatus.EXPIRED);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> taskService.validateCheckInReservation(10L, "BK-1042"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Reservation BK-1042 has expired");
    }

    @Test
    @DisplayName("validateCheckInReservation returns rentPaid = true if RENT payment already succeeded")
    void testValidateCheckInReservation_rentAlreadyPaid() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));

        Payment depositPayment = new Payment(
                "RC-1727932800101", customer, reservation, null, null,
                PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, BigDecimal.valueOf(103500), PaymentStatus.SUCCEEDED
        );
        Payment rentPayment = new Payment(
                "RC-1727932900202", customer, reservation, null, null,
                PaymentPurpose.RENT, PaymentMethod.CASH, BigDecimal.valueOf(1035000), PaymentStatus.SUCCEEDED
        );
        when(paymentRepository.findByReservationIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(depositPayment, rentPayment));

        PricingBreakdownDto pricing = new PricingBreakdownDto(
                "S-3", 1, BigDecimal.valueOf(1035000), BigDecimal.valueOf(1035000),
                List.of(), BigDecimal.valueOf(1035000), BigDecimal.valueOf(10),
                BigDecimal.valueOf(103500), true, BigDecimal.valueOf(103500), "VND", "v3"
        );
        when(pricingEngine.calculatePricing(any(Unit.class), anyInt(), any())).thenReturn(pricing);

        CheckInValidationDto result = taskService.validateCheckInReservation(10L, "BK-1042");

        assertThat(result).isNotNull();
        assertThat(result.valid()).isTrue();
        assertThat(result.rentPaid()).isTrue();
        assertThat(result.rentReceiptCode()).isEqualTo("RC-1727932900202");
    }
}
