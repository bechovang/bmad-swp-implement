package com.storagehub.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storagehub.dto.CreateSupportTicketRequest;
import com.storagehub.dto.SupportTicketDto;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.*;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.TaskService;
import com.storagehub.service.TicketService;
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
public class SupportTicketCreationTests {

    @Mock
    private SupportTicketRepository supportTicketRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private StaffAssignmentRepository staffAssignmentRepository;
    @Mock
    private EscalationRepository escalationRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskService taskService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LogService logService;

    private TicketService ticketService;

    private User customer;
    private User staff;
    private Zone zone1;
    private Unit unit1;
    private Reservation reservation1;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                supportTicketRepository,
                unitRepository,
                userRepository,
                reservationRepository,
                staffAssignmentRepository,
                escalationRepository,
                taskRepository,
                taskService,
                notificationService,
                logService
        );

        Role customerRole = new Role(1, "Customer", "Customer role");
        Role staffRole = new Role(2, "Staff", "Staff role");

        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        staff = new User("Minh Tran", "minh@storagehub.dev", "0902345678", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 2L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        zone1 = new Zone(facility, "A", 1);
        ReflectionTestUtils.setField(zone1, "id", 1);

        UnitType unitType = new UnitType("S", "Small unit");
        ReflectionTestUtils.setField(unitType, "id", 2);

        unit1 = new Unit("S-3", unitType, zone1, BigDecimal.valueOf(5.0), 1, "PIN", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit1, "id", 1L);

        reservation1 = new Reservation("BK-1042", customer, unit1, LocalDate.now().minusDays(5), LocalDate.now().plusDays(20), BigDecimal.valueOf(100000), "123456", ReservationStatus.CHECKED_IN);
        ReflectionTestUtils.setField(reservation1, "id", 1L);
    }

    @Test
    @DisplayName("Customer successfully creates support ticket: routes to on-duty staff, creates SUPPORT task, notifies parties")
    void testCreateSupportTicketSuccess() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest(
                1L,
                IncidentType.LOST_ACCESS,
                "Smart keypad is unresponsive and does not accept PIN"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit1));
        when(reservationRepository.findByCustomerIdAndStatusIn(eq(1L), any())).thenReturn(List.of(reservation1));
        when(supportTicketRepository.countAllTickets()).thenReturn(0L);

        StaffAssignment assignment = new StaffAssignment(staff, zone1, Shift.MORNING, LocalDate.now());
        when(staffAssignmentRepository.findByZoneAndDateAndShift(eq(1), any(), any()))
                .thenReturn(Optional.of(assignment));

        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        SupportTicketDto dto = ticketService.createTicket(request, 1L);

        assertThat(dto).isNotNull();
        assertThat(dto.code()).startsWith("SR-");
        assertThat(dto.incidentType()).isEqualTo(IncidentType.LOST_ACCESS);
        assertThat(dto.status()).isEqualTo(SupportTicketStatus.OPEN);
        assertThat(dto.unitCode()).isEqualTo("S-3");
        assertThat(dto.assignedStaffName()).isEqualTo("Minh Tran");

        // Verify task creation
        verify(taskService, times(1)).createSupportTask(any(SupportTicket.class));

        // Verify notifications sent to customer and staff
        verify(notificationService, times(1)).send(eq(1L), eq("TICKET_RECEIVED"), any(), eq("/support"));
        verify(notificationService, times(1)).send(eq(2L), eq("TASK_ASSIGNED"), any(), eq("/tasks"));

        // Verify activity log appended
        verify(logService, times(1)).append(eq(1L), eq(EntityType.TICKET), eq(10L), eq(Action.STATUS_CHANGE), isNull(), eq("OPEN"), any());
    }

    @Test
    @DisplayName("Ticket creation fails when unit does not belong to customer's active rental")
    void testCreateSupportTicketUnownedUnitFails() {
        UnitType unitType = new UnitType("M", "Medium unit");
        Unit unit2 = new Unit("M-2", unitType, zone1, BigDecimal.valueOf(8.0), 1, "QR", UnitStatus.AVAILABLE);
        ReflectionTestUtils.setField(unit2, "id", 2L);

        CreateSupportTicketRequest request = new CreateSupportTicketRequest(
                2L,
                IncidentType.DEVICE_ISSUE,
                "Door handle broken"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(unitRepository.findById(2L)).thenReturn(Optional.of(unit2));
        // Customer active rentals do not contain unit 2
        when(reservationRepository.findByCustomerIdAndStatusIn(eq(1L), any())).thenReturn(List.of(reservation1));

        assertThatThrownBy(() -> ticketService.createTicket(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("INVALID_UNIT_OR_RENTAL"));

        verify(supportTicketRepository, never()).save(any());
        verify(taskService, never()).createSupportTask(any());
    }

    @Test
    @DisplayName("Ticket creation succeeds with fallback assignment when no staff is scheduled in the zone")
    void testCreateSupportTicketFallbackStaff() {
        CreateSupportTicketRequest request = new CreateSupportTicketRequest(
                1L,
                IncidentType.SECURITY,
                "Locker hinge loose"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit1));
        when(reservationRepository.findByCustomerIdAndStatusIn(eq(1L), any())).thenReturn(List.of(reservation1));
        when(supportTicketRepository.countAllTickets()).thenReturn(1L);

        // No zone assignment, fallback to staff user list
        when(staffAssignmentRepository.findByZoneAndDateAndShift(eq(1), any(), any())).thenReturn(Optional.empty());
        when(staffAssignmentRepository.findByDateAndShift(any(), any())).thenReturn(List.of());
        when(staffAssignmentRepository.findByWorkDate(any())).thenReturn(List.of());
        when(userRepository.findStaffUsers()).thenReturn(List.of(staff));

        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        SupportTicketDto dto = ticketService.createTicket(request, 1L);

        assertThat(dto).isNotNull();
        assertThat(dto.assignedStaffName()).isEqualTo("Minh Tran");
        verify(taskService, times(1)).createSupportTask(any(SupportTicket.class));
    }

    @Test
    @DisplayName("getTickets filters by customer ID when caller has CUSTOMER role")
    void testGetTicketsCustomerScope() {
        SupportTicket ticket = new SupportTicket("SR-0032", customer, unit1, reservation1, IncidentType.DEVICE_ISSUE, "Door issue", staff);
        ticket.setId(1L);

        when(supportTicketRepository.findByCustomerId(1L)).thenReturn(List.of(ticket));

        List<SupportTicketDto> result = ticketService.getTickets(1L, "CUSTOMER", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("SR-0032");
        verify(supportTicketRepository, times(1)).findByCustomerId(1L);
    }
}
