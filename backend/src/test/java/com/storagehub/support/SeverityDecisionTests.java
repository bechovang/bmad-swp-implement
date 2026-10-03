package com.storagehub.support;

import com.storagehub.dto.EscalationDto;
import com.storagehub.dto.SeverityDecisionRequest;
import com.storagehub.entity.Escalation;
import com.storagehub.entity.EscalationDecision;
import com.storagehub.entity.Facility;
import com.storagehub.entity.IncidentType;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Role;
import com.storagehub.entity.SupportTicket;
import com.storagehub.entity.SupportTicketStatus;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.entity.UnitType;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.entity.Zone;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.repository.EscalationRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.StaffAssignmentRepository;
import com.storagehub.repository.SupportTicketRepository;
import com.storagehub.repository.TaskRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeverityDecisionTests {

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
    private User manager;
    private Unit damagedUnit;
    private Unit availableUnit;
    private Reservation reservation;
    private SupportTicket ticket;
    private Escalation escalation;

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

        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash",
                new Role(1, "Customer", null), UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        staff = new User("Minh Tran", "staff@storagehub.dev", "0902345678", "hash",
                new Role(2, "Staff", null), UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 2L);

        manager = new User("Hoa Pham", "manager@storagehub.dev", "0903456789", "hash",
                new Role(3, "Facility Manager", null), UserStatus.ACTIVE);
        ReflectionTestUtils.setField(manager, "id", 3L);

        Facility facility = new Facility("Tan Binh Depot", "45 NV Troi", "0900000000", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("Standard", "Standard Lock");

        damagedUnit = new Unit("M-2", unitType, zone, BigDecimal.valueOf(6.0), 1, "PIN", UnitStatus.RENTED);
        ReflectionTestUtils.setField(damagedUnit, "id", 2L);

        availableUnit = new Unit("M-5", unitType, zone, BigDecimal.valueOf(6.0), 1, "PIN", UnitStatus.AVAILABLE);
        ReflectionTestUtils.setField(availableUnit, "id", 3L);

        reservation = new Reservation("BK-1042", customer, damagedUnit, LocalDate.now().minusDays(5), LocalDate.now().plusMonths(1),
                BigDecimal.valueOf(100000), "123456", ReservationStatus.CHECKED_IN);
        ReflectionTestUtils.setField(reservation, "id", 100L);

        ticket = new SupportTicket("SR-0033", customer, damagedUnit, reservation, IncidentType.OTHER,
                "Ceiling leak in unit M-2", staff);
        ReflectionTestUtils.setField(ticket, "id", 10L);
        ticket.setStatus(SupportTicketStatus.ESCALATED);

        escalation = new Escalation(ticket, staff, manager, "Water ingress from ceiling joint in unit M-2");
        ReflectionTestUtils.setField(escalation, "id", 1L);
    }

    @Test
    @DisplayName("FM decides SEVERE: sets unit to MAINTENANCE, relocates customer, issues PIN, creates tasks and logs")
    void testSevereDecisionAndRelocation() {
        when(escalationRepository.findById(1L)).thenReturn(Optional.of(escalation));
        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(unitRepository.findById(3L)).thenReturn(Optional.of(availableUnit));
        when(unitRepository.save(any(Unit.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(i -> i.getArgument(0));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        SeverityDecisionRequest request = new SeverityDecisionRequest(
                EscalationDecision.MAINTENANCE_RELOCATE,
                "Confirmed active roof leak. Relocating customer to M-5.",
                3L
        );

        EscalationDto dto = ticketService.processSeverityDecision(1L, request, 3L);

        assertNotNull(dto);
        assertEquals(EscalationDecision.MAINTENANCE_RELOCATE, dto.getDecision());
        assertEquals("M-5", dto.getRelocatedToUnitCode());
        assertEquals(3L, dto.getRelocatedToUnitId());
        assertEquals(UnitStatus.MAINTENANCE, damagedUnit.getStatus());
        assertEquals(UnitStatus.RENTED, availableUnit.getStatus());
        assertEquals(availableUnit, reservation.getUnit());
        assertNotNull(reservation.getAccessCode());
        assertEquals(SupportTicketStatus.IN_PROGRESS, ticket.getStatus());

        // Verify task creation
        verify(taskRepository, times(2)).save(any());

        // Verify activity log & notification
        verify(logService).append(eq(3L), eq(com.storagehub.entity.EntityType.TICKET), eq(10L),
                eq(com.storagehub.entity.Action.STATUS_CHANGE), eq("ESCALATED"), eq("IN_PROGRESS"),
                contains("RELOCATION"));
        verify(notificationService).send(eq(1L), eq("TICKET_RELOCATED"), anyString(), eq("/support"));
    }

    @Test
    @DisplayName("FM decides NOT_SEVERE: returns ticket to staff with manager instructions")
    void testNotSevereDecisionReturnsToStaff() {
        when(escalationRepository.findById(1L)).thenReturn(Optional.of(escalation));
        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(i -> i.getArgument(0));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        SeverityDecisionRequest request = new SeverityDecisionRequest(
                EscalationDecision.RETURN_TO_STAFF,
                "Minor condensation, not a leak. Staff please provide dehumidifier.",
                null
        );

        EscalationDto dto = ticketService.processSeverityDecision(1L, request, 3L);

        assertNotNull(dto);
        assertEquals(EscalationDecision.RETURN_TO_STAFF, dto.getDecision());
        assertEquals(SupportTicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(UnitStatus.RENTED, damagedUnit.getStatus()); // remains unchanged

        // Verify notification to staff
        verify(notificationService).send(eq(2L), eq("ESCALATION_RETURNED"), anyString(), eq("/tasks"));
    }

    @Test
    @DisplayName("Severe decision fails when target unit is not AVAILABLE")
    void testSevereFailsWhenTargetUnitNotAvailable() {
        availableUnit.setStatus(UnitStatus.RENTED);
        when(escalationRepository.findById(1L)).thenReturn(Optional.of(escalation));
        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(unitRepository.findById(3L)).thenReturn(Optional.of(availableUnit));

        SeverityDecisionRequest request = new SeverityDecisionRequest(
                EscalationDecision.MAINTENANCE_RELOCATE,
                "Leak confirmed.",
                3L
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                ticketService.processSeverityDecision(1L, request, 3L));

        assertEquals("UNIT_UNAVAILABLE", ex.getCode());
    }

    @Test
    @DisplayName("Duplicate decision on decided escalation throws DECISION_ALREADY_MADE (409)")
    void testDuplicateDecisionRejected() {
        escalation.setDecision(EscalationDecision.MAINTENANCE_RELOCATE);
        when(escalationRepository.findById(1L)).thenReturn(Optional.of(escalation));

        SeverityDecisionRequest request = new SeverityDecisionRequest(
                EscalationDecision.RETURN_TO_STAFF,
                "Second attempt.",
                null
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                ticketService.processSeverityDecision(1L, request, 3L));

        assertEquals("DECISION_ALREADY_MADE", ex.getCode());
    }
}
