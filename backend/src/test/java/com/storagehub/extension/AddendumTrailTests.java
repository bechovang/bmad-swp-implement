package com.storagehub.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.ContractDto;
import com.storagehub.dto.TaskDto;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.*;
import com.storagehub.service.ContractService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AddendumTrailTests {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private PricingEngine pricingEngine;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ContractService contractService;
    private TaskService taskService;

    private User customer;
    private User staff;
    private Unit unit;
    private Reservation reservation;
    private Contract baseContract;
    private Contract addendum;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(
                contractRepository,
                reservationRepository,
                pricingEngine,
                logService,
                notificationService,
                objectMapper
        );

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

        UnitType unitType = new UnitType("M", "Medium storage unit");
        ReflectionTestUtils.setField(unitType, "id", 2);

        unit = new Unit("M-5", unitType, zone, new BigDecimal("8.0"), 1, "PIN", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit, "id", 10L);

        RentalPolicy policy = new RentalPolicy("v3", LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE);
        ReflectionTestUtils.setField(policy, "id", 1);

        reservation = new Reservation(
                "BK-1042",
                customer,
                unit,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(20),
                new BigDecimal("103500"),
                "123456",
                ReservationStatus.CHECKED_IN
        );
        ReflectionTestUtils.setField(reservation, "id", 100L);

        baseContract = new Contract(
                "CT-1042",
                reservation,
                policy,
                "{}",
                "/api/v1/attachments/base-signed.jpg",
                ContractStatus.SIGNED,
                null,
                1
        );
        ReflectionTestUtils.setField(baseContract, "id", 200L);

        addendum = new Contract(
                "CT-1042-A1",
                reservation,
                policy,
                "{}",
                null,
                ContractStatus.AWAITING_SIGNATURE,
                baseContract,
                1
        );
        ReflectionTestUtils.setField(addendum, "id", 201L);
    }

    @Test
    @DisplayName("Story 4.3: Desk ritual attaches signed photo to addendum and sends notification")
    void testSignAddendumSuccess() {
        when(contractRepository.findById(201L)).thenReturn(Optional.of(addendum));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContractDto signedDto = contractService.signContract(201L, "/api/v1/attachments/signed-addendum.jpg", staff.getId());

        assertThat(signedDto.status()).isEqualTo(ContractStatus.SIGNED);
        assertThat(signedDto.signedPhotoUrl()).isEqualTo("/api/v1/attachments/signed-addendum.jpg");

        verify(logService).append(
                eq(staff.getId()),
                eq(EntityType.CONTRACT),
                eq(201L),
                eq(Action.STATUS_CHANGE),
                eq("AWAITING_SIGNATURE"),
                eq("SIGNED"),
                contains("signed-addendum.jpg")
        );

        verify(notificationService).send(
                eq(customer.getId()),
                eq("CONTRACT_SIGNED"),
                eq("Addendum CT-1042-A1 signed and filed."),
                eq("/rentals/100")
        );
    }

    @Test
    @DisplayName("Story 4.3: Expiring unsigned addendum transitions status to EXPIRED and logs reason")
    void testExpireAddendumSuccess() {
        when(contractRepository.findById(201L)).thenReturn(Optional.of(addendum));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContractDto expiredDto = contractService.expireContract(201L, "Customer did not visit desk within 7 days", staff.getId());

        assertThat(expiredDto.status()).isEqualTo(ContractStatus.EXPIRED);

        verify(logService).append(
                eq(staff.getId()),
                eq(EntityType.CONTRACT),
                eq(201L),
                eq(Action.STATUS_CHANGE),
                eq("AWAITING_SIGNATURE"),
                eq("EXPIRED"),
                eq("Customer did not visit desk within 7 days")
        );

        verify(notificationService).send(
                eq(customer.getId()),
                eq("CONTRACT_EXPIRED"),
                contains("Addendum CT-1042-A1 has expired"),
                eq("/rentals/100")
        );
    }

    @Test
    @DisplayName("Story 4.3: Voiding addendum requires reason and transitions to VOIDED")
    void testVoidAddendumSuccess() {
        when(contractRepository.findById(201L)).thenReturn(Optional.of(addendum));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContractDto voidedDto = contractService.voidContract(201L, "Extension cancelled by operational decision", staff.getId());

        assertThat(voidedDto.status()).isEqualTo(ContractStatus.VOIDED);

        verify(logService).append(
                eq(staff.getId()),
                eq(EntityType.CONTRACT),
                eq(201L),
                eq(Action.STATUS_CHANGE),
                eq("AWAITING_SIGNATURE"),
                eq("VOIDED"),
                eq("Voided: Extension cancelled by operational decision")
        );
    }

    @Test
    @DisplayName("Story 4.3: Voiding without reason is rejected with REASON_REQUIRED")
    void testVoidAddendumWithoutReasonRejected() {
        assertThatThrownBy(() -> contractService.voidContract(201L, "   ", staff.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Void reason is mandatory");
    }

    @Test
    @DisplayName("Story 4.3: Snap-back guard blocks moving CONTRACT task to DONE if addendum is unsigned")
    void testSnapBackBlocksUnsignedContractTask() {
        Task contractTask = new Task(TaskType.CONTRACT, "CT-1042-A1", staff, LocalDate.now(), TaskStatus.TODO);
        ReflectionTestUtils.setField(contractTask, "id", 501L);

        when(taskRepository.findByIdWithStaff(501L)).thenReturn(Optional.of(contractTask));
        when(contractRepository.findByCode("CT-1042-A1")).thenReturn(Optional.of(addendum));

        assertThatThrownBy(() -> taskService.updateTaskStatus(501L, TaskStatus.DONE, "Marking done", staff.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("CLOSING_STEP_MISSING");
                    assertThat(bre.getMissingStep()).isEqualTo("CONTRACT_UNSIGNED");
                });
    }

    @Test
    @DisplayName("Story 4.3: CONTRACT task can move to DONE once addendum is signed or expired")
    void testContractTaskCompletesWhenSigned() {
        Task contractTask = new Task(TaskType.CONTRACT, "CT-1042-A1", staff, LocalDate.now(), TaskStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(contractTask, "id", 501L);

        addendum.setStatus(ContractStatus.SIGNED);
        addendum.setSignedPhotoUrl("/api/v1/attachments/signed-copy.jpg");

        when(taskRepository.findByIdWithStaff(501L)).thenReturn(Optional.of(contractTask));
        when(contractRepository.findByCode("CT-1042-A1")).thenReturn(Optional.of(addendum));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto completedTask = taskService.updateTaskStatus(501L, TaskStatus.DONE, "Signed by customer", staff.getId());
        assertThat(completedTask.status()).isEqualTo(TaskStatus.DONE);
    }
}
