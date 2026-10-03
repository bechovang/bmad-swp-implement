package com.storagehub.support;

import com.storagehub.dto.EscalateSupportTicketRequest;
import com.storagehub.dto.ResolveSupportTicketRequest;
import com.storagehub.dto.SupportTicketDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Escalation;
import com.storagehub.entity.Facility;
import com.storagehub.entity.IncidentType;
import com.storagehub.entity.Role;
import com.storagehub.entity.SupportTicket;
import com.storagehub.entity.SupportTicketStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportTicketHandlingTests {

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
    private Unit unit;
    private SupportTicket ticket;
    private Task task;

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
        Role managerRole = new Role(3, "Facility Manager", "FM role");

        customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567", "hash", customerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);

        staff = new User("Minh Tran", "staff@storagehub.dev", "0902345678", "hash", staffRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(staff, "id", 2L);

        manager = new User("Tuan Le", "manager@storagehub.dev", "0903456789", "hash", managerRole, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(manager, "id", 3L);

        Facility facility = new Facility("Tan Binh Depot", "45 Nguyen Van Troi", "02839991122", 1);
        Zone zone = new Zone(facility, "A", 1);
        UnitType unitType = new UnitType("S", "Small unit");
        unit = new Unit("S-3", unitType, zone, BigDecimal.valueOf(5.0), 1, "PIN", UnitStatus.RENTED);
        ReflectionTestUtils.setField(unit, "id", 1L);

        ticket = new SupportTicket(
                "SR-0032",
                customer,
                unit,
                null,
                IncidentType.DEVICE_ISSUE,
                "Door latch jammed on S-3",
                staff
        );
        ReflectionTestUtils.setField(ticket, "id", 10L);

        task = new Task(TaskType.SUPPORT, "SR-0032", staff, LocalDate.now(), TaskStatus.TODO);
        ReflectionTestUtils.setField(task, "id", 100L);
    }

    @Test
    @DisplayName("resolveTicket marks ticket as RESOLVED, saves note, marks task as DONE, and notifies customer")
    void resolveTicketSuccessfully() {
        when(supportTicketRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.findByRefCodeAndType("SR-0032", TaskType.SUPPORT)).thenReturn(Optional.of(task));

        ResolveSupportTicketRequest req = new ResolveSupportTicketRequest("Door hinge lubricated and latch repaired");
        SupportTicketDto result = ticketService.resolveTicket(10L, req, 2L);

        assertThat(result.status()).isEqualTo(SupportTicketStatus.RESOLVED);
        assertThat(result.resolutionNote()).isEqualTo("Door hinge lubricated and latch repaired");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);

        verify(logService).append(eq(2L), eq(EntityType.TICKET), eq(10L), eq(Action.STATUS_CHANGE),
                eq("OPEN"), eq("RESOLVED"), eq("Door hinge lubricated and latch repaired"));
        verify(notificationService).send(eq(1L), eq("TICKET_RESOLVED"),
                eq("Resolved — Door hinge lubricated and latch repaired. See ticket for details."), eq("/support"));
    }

    @Test
    @DisplayName("escalateTicket creates Escalation record, sets ESCALATED status, and notifies manager")
    void escalateTicketSuccessfully() {
        when(supportTicketRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(ticket));
        when(escalationRepository.findByTicketId(10L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalateSupportTicketRequest req = new EscalateSupportTicketRequest("Flooding in unit M-2. Customer relocation needed.");
        SupportTicketDto result = ticketService.escalateTicket(10L, req, 2L);

        assertThat(result.status()).isEqualTo(SupportTicketStatus.ESCALATED);
        verify(escalationRepository).save(any(Escalation.class));
        verify(logService).append(eq(2L), eq(EntityType.TICKET), eq(10L), eq(Action.STATUS_CHANGE),
                eq("OPEN"), eq("ESCALATED"), eq("Escalated to Facility Manager: Flooding in unit M-2. Customer relocation needed."));
        verify(notificationService).send(eq(3L), eq("TICKET_ESCALATED"),
                eq("Escalated to Facility Manager — Flooding in unit M-2. Customer relocation needed."), eq("/tasks"));
    }

    @Test
    @DisplayName("escalateTicket rejects blank notes")
    void escalateTicketBlankNoteFails() {
        EscalateSupportTicketRequest req = new EscalateSupportTicketRequest("   ");
        assertThatThrownBy(() -> ticketService.escalateTicket(10L, req, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("note must not be blank");
    }

    @Test
    @DisplayName("escalateTicket rejects re-escalating an already escalated ticket (409 Conflict)")
    void reEscalationFails() {
        when(supportTicketRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(ticket));
        when(escalationRepository.findByTicketId(10L)).thenReturn(Optional.of(new Escalation()));

        EscalateSupportTicketRequest req = new EscalateSupportTicketRequest("Second escalate reason");
        assertThatThrownBy(() -> ticketService.escalateTicket(10L, req, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .matches(ex -> "ALREADY_ESCALATED".equals(((BusinessRuleException) ex).getCode()));
    }
}
