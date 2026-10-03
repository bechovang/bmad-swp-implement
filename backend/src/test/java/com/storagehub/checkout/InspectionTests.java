package com.storagehub.checkout;

import com.storagehub.dto.CheckoutTaskDetailDto;
import com.storagehub.dto.InspectionItemInput;
import com.storagehub.dto.SubmitInspectionRequest;
import com.storagehub.entity.*;
import com.storagehub.repository.*;
import com.storagehub.service.InspectionService;
import com.storagehub.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
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
public class InspectionTests {

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CheckoutRequestRepository checkoutRequestRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogService logService;

    @InjectMocks
    private InspectionService inspectionService;

    private User staffUser;
    private User customerUser;
    private Unit unit;
    private Reservation reservation;
    private CheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        staffUser = new User("Minh Tran", "staff@storagehub.dev", "0902345678", "hash",
                new Role(2, "Staff", null), UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staffUser, "id", 2L);

        customerUser = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash",
                new Role(1, "Customer", null), UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customerUser, "id", 1L);

        Facility facility = new Facility("Tan Binh Depot", "45 NV Troi", "0900000000", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("Standard", "Standard Lock");

        unit = new Unit("S-03", unitType, zone, BigDecimal.valueOf(5.0), 1, "PIN", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit, "id", 10L);

        reservation = new Reservation("BK-2026-0001", customerUser, unit, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(2),
                BigDecimal.valueOf(200000), "123456", ReservationStatus.CHECKOUT_REQUESTED);
        ReflectionTestUtils.setField(reservation, "id", 100L);

        checkoutRequest = new CheckoutRequest(reservation, LocalDate.of(2026, 11, 20), CheckoutRequestStatus.PENDING, "Key handover");
        ReflectionTestUtils.setField(checkoutRequest, "id", 50L);
    }

    @Test
    @DisplayName("Staff submits 4-point inspection with OK/MINOR/MAJOR results")
    void testSubmitInspectionSuccess() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffUser));
        when(checkoutRequestRepository.findLatestByReservationId(100L)).thenReturn(List.of(checkoutRequest));

        List<InspectionItemInput> items = List.of(
                new InspectionItemInput(InspectionItem.ACCESS_CARD, InspectionResult.OK, "Card returned intact"),
                new InspectionItemInput(InspectionItem.PADLOCK, InspectionResult.MAJOR, "Padlock missing / broken key"),
                new InspectionItemInput(InspectionItem.CLEANLINESS, InspectionResult.MINOR, "Dust on floor"),
                new InspectionItemInput(InspectionItem.STRUCTURE, InspectionResult.OK, "Walls and door good")
        );

        SubmitInspectionRequest request = new SubmitInspectionRequest(items, true, true, "Keys received");

        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> {
            Inspection insp = invocation.getArgument(0);
            ReflectionTestUtils.setField(insp, "id", 1L);
            return insp;
        });

        CheckoutTaskDetailDto result = inspectionService.submitInspection(100L, request, 2L, "STAFF");

        assertThat(result).isNotNull();
        assertThat(result.reservationCode()).isEqualTo("BK-2026-0001");
        assertThat(result.keyReturned()).isTrue();
        assertThat(result.unitEmptied()).isTrue();
        assertThat(result.hasMajorDamage()).isTrue();
        assertThat(result.majorItems()).containsExactly(InspectionItem.PADLOCK);

        verify(inspectionRepository, times(4)).save(any(Inspection.class));
        verify(logService).append(eq(2L), eq(EntityType.RESERVATION), eq(100L), eq(Action.INSPECTION_COMPLETED), isNull(), isNull(), anyString());
    }

    @Test
    @DisplayName("Customer is denied from submitting unit inspections")
    void testCustomerDeniedFromSubmittingInspection() {
        SubmitInspectionRequest request = new SubmitInspectionRequest(List.of(), true, true, null);

        assertThatThrownBy(() -> inspectionService.submitInspection(100L, request, 1L, "CUSTOMER"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only facility staff or managers");
    }

    @Test
    @DisplayName("Get checkout task detail by task ID returns inspection matrix and customer details")
    void testGetCheckoutTaskDetailByTaskId() {
        Task task = new Task(TaskType.CHECKOUT, "BK-2026-0001", staffUser, LocalDate.of(2026, 11, 20), TaskStatus.TODO);
        ReflectionTestUtils.setField(task, "id", 77L);

        when(taskRepository.findById(77L)).thenReturn(Optional.of(task));
        when(reservationRepository.findByCode("BK-2026-0001")).thenReturn(Optional.of(reservation));
        when(checkoutRequestRepository.findLatestByReservationId(100L)).thenReturn(List.of(checkoutRequest));

        Inspection insp1 = new Inspection(reservation, InspectionItem.ACCESS_CARD, InspectionResult.OK, "OK", staffUser);
        ReflectionTestUtils.setField(insp1, "id", 101L);
        when(inspectionRepository.findByReservationIdOrderByIdAsc(100L)).thenReturn(List.of(insp1));

        CheckoutTaskDetailDto result = inspectionService.getCheckoutTaskDetailByTaskId(77L, 2L, "STAFF");

        assertThat(result).isNotNull();
        assertThat(result.taskId()).isEqualTo(77L);
        assertThat(result.unitCode()).isEqualTo("S-03");
        assertThat(result.inspections()).hasSize(1);
        assertThat(result.hasMajorDamage()).isFalse();
    }
}
