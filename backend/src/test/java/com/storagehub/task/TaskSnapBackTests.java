package com.storagehub.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.TaskDto;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.*;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskSnapBackTests {

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
    private RentalPolicy policy;
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

        Role customerRole = new Role(1, "Customer", "Customer role");
        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        Role staffRole = new Role(2, "Staff", "Staff role");
        staff = new User("Minh Tran", "staff@storagehub.dev", "0902345678", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 2L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        ReflectionTestUtils.setField(facility, "id", 1);

        Zone zone = new Zone(facility, "A", 1);
        ReflectionTestUtils.setField(zone, "id", 1);

        UnitType unitType = new UnitType("S", "Small unit");
        ReflectionTestUtils.setField(unitType, "id", 1);

        unit = new Unit("S-3", unitType, zone, new BigDecimal("5.0"), 1, "PIN", UnitStatus.AVAILABLE);
        ReflectionTestUtils.setField(unit, "id", 1L);

        policy = new RentalPolicy("v3", LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE);
        ReflectionTestUtils.setField(policy, "id", 1);

        reservation = new Reservation(
                "BK-1042",
                customer,
                unit,
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2027, 1, 3),
                new BigDecimal("103500"),
                null,
                ReservationStatus.RESERVED
        );
        ReflectionTestUtils.setField(reservation, "id", 100L);

        checkInTask = new Task(
                TaskType.CHECK_IN,
                "BK-1042",
                staff,
                LocalDate.of(2026, 10, 3),
                TaskStatus.IN_PROGRESS
        );
        ReflectionTestUtils.setField(checkInTask, "id", 10L);
    }

    @Test
    @DisplayName("Story 3.5: Moving Check-in task to DONE fails with RENT_PAYMENT_PENDING when rent is unpaid")
    void testCheckInTask_doneFails_whenRentUnpaid() {
        when(taskRepository.findByIdWithStaff(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(100L)).thenReturn(List.of(
                new Payment("RC-101", customer, reservation, 1727932800101L, 101L, PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, new BigDecimal("103500"), PaymentStatus.SUCCEEDED)
        ));

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, TaskStatus.DONE, "Attempt done", 2L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("CLOSING_STEP_MISSING");
                    assertThat(bre.getMissingStep()).isEqualTo("RENT_PAYMENT_PENDING");
                    assertThat(bre.getStepLabel()).isEqualTo("Rent payment is still pending on this check-in.");
                });
    }

    @Test
    @DisplayName("Story 3.5: Moving Check-in task to DONE fails with CONTRACT_UNSIGNED when contract is not signed")
    void testCheckInTask_doneFails_whenContractUnsigned() {
        when(taskRepository.findByIdWithStaff(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(100L)).thenReturn(List.of(
                new Payment("RC-101", customer, reservation, 1727932800101L, 101L, PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, new BigDecimal("103500"), PaymentStatus.SUCCEEDED),
                new Payment("RC-102", customer, reservation, 1727932800102L, 102L, PaymentPurpose.RENT, PaymentMethod.CASH, new BigDecimal("1035000"), PaymentStatus.SUCCEEDED)
        ));

        Contract draftContract = new Contract(
                "CT-1042",
                reservation,
                policy,
                "{\"code\":\"CT-1042\"}",
                null,
                ContractStatus.DRAFT,
                null,
                1
        );
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(draftContract));

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, TaskStatus.DONE, "Attempt done", 2L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("CLOSING_STEP_MISSING");
                    assertThat(bre.getMissingStep()).isEqualTo("CONTRACT_UNSIGNED");
                    assertThat(bre.getStepLabel()).isEqualTo("Contract signature is required before completing check-in.");
                });
    }

    @Test
    @DisplayName("Story 3.5: Moving Check-in task to DONE fails with CHECKIN_NOT_ACTIVATED when access code not handed over")
    void testCheckInTask_doneFails_whenCheckInNotActivated() {
        when(taskRepository.findByIdWithStaff(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(100L)).thenReturn(List.of(
                new Payment("RC-101", customer, reservation, 1727932800101L, 101L, PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, new BigDecimal("103500"), PaymentStatus.SUCCEEDED),
                new Payment("RC-102", customer, reservation, 1727932800102L, 102L, PaymentPurpose.RENT, PaymentMethod.CASH, new BigDecimal("1035000"), PaymentStatus.SUCCEEDED)
        ));

        Contract signedContract = new Contract(
                "CT-1042",
                reservation,
                policy,
                "{\"code\":\"CT-1042\"}",
                "/api/v1/attachments/signed-1042.jpg",
                ContractStatus.SIGNED,
                null,
                1
        );
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(signedContract));

        // Reservation is still RESERVED (not CHECKED_IN)
        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, TaskStatus.DONE, "Attempt done", 2L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("CLOSING_STEP_MISSING");
                    assertThat(bre.getMissingStep()).isEqualTo("CHECKIN_NOT_ACTIVATED");
                    assertThat(bre.getStepLabel()).isEqualTo("Access code handover and check-in activation are required before completing check-in.");
                });
    }

    @Test
    @DisplayName("Story 3.5: Moving Check-in task to DONE succeeds when all closing steps are satisfied")
    void testCheckInTask_doneSucceeds_whenAllStepsSatisfied() {
        when(taskRepository.findByIdWithStaff(10L)).thenReturn(Optional.of(checkInTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(100L)).thenReturn(List.of(
                new Payment("RC-101", customer, reservation, 1727932800101L, 101L, PaymentPurpose.DEPOSIT, PaymentMethod.PAYOS, new BigDecimal("103500"), PaymentStatus.SUCCEEDED),
                new Payment("RC-102", customer, reservation, 1727932800102L, 102L, PaymentPurpose.RENT, PaymentMethod.CASH, new BigDecimal("1035000"), PaymentStatus.SUCCEEDED)
        ));

        Contract activeContract = new Contract(
                "CT-1042",
                reservation,
                policy,
                "{\"code\":\"CT-1042\"}",
                "/api/v1/attachments/signed-1042.jpg",
                ContractStatus.ACTIVE,
                null,
                1
        );
        when(contractRepository.findLatestByReservationId(100L)).thenReturn(Optional.of(activeContract));

        // Reservation is CHECKED_IN
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setAccessCode("482913");

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto result = taskService.updateTaskStatus(10L, TaskStatus.DONE, "All done", 2L);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    @DisplayName("Story 3.5: CLEANING and SUPPORT tasks have no closing steps and complete directly")
    void testCleaningTask_doneSucceedsDirectly() {
        Task cleaningTask = new Task(
                TaskType.CLEANING,
                "S-3",
                staff,
                LocalDate.of(2026, 10, 5),
                TaskStatus.IN_PROGRESS
        );
        ReflectionTestUtils.setField(cleaningTask, "id", 20L);

        when(taskRepository.findByIdWithStaff(20L)).thenReturn(Optional.of(cleaningTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto result = taskService.updateTaskStatus(20L, TaskStatus.DONE, "Cleaned", 2L);
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TaskStatus.DONE);

        Task supportTask = new Task(
                TaskType.SUPPORT,
                "SR-0001",
                staff,
                LocalDate.of(2026, 10, 5),
                TaskStatus.IN_PROGRESS
        );
        ReflectionTestUtils.setField(supportTask, "id", 30L);

        when(taskRepository.findByIdWithStaff(30L)).thenReturn(Optional.of(supportTask));

        TaskDto supportResult = taskService.updateTaskStatus(30L, TaskStatus.DONE, "Fixed", 2L);
        assertThat(supportResult).isNotNull();
        assertThat(supportResult.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    @DisplayName("Story 3.5 & 6.2: CHECKOUT task done is blocked if reservation is not CLOSED")
    void testCheckoutTask_doneBlockedUntilReservationClosed() {
        Task checkoutTask = new Task(
                TaskType.CHECKOUT,
                "BK-1042",
                staff,
                LocalDate.of(2026, 11, 20),
                TaskStatus.IN_PROGRESS
        );
        ReflectionTestUtils.setField(checkoutTask, "id", 40L);

        when(taskRepository.findByIdWithStaff(40L)).thenReturn(Optional.of(checkoutTask));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));
        reservation.setStatus(ReservationStatus.CHECKOUT_REQUESTED);

        assertThatThrownBy(() -> taskService.updateTaskStatus(40L, TaskStatus.DONE, "Trying to close without settlement", 2L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("CLOSING_STEP_MISSING");
                    assertThat(bre.getMissingStep()).isEqualTo("SETTLEMENT_PENDING");
                });
    }
}
