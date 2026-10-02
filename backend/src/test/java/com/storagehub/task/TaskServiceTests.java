package com.storagehub.task;

import com.storagehub.dto.CreateTaskRequest;
import com.storagehub.dto.TaskDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Facility;
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
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.TaskRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.LogService;
import com.storagehub.service.NotificationService;
import com.storagehub.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTests {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private LogService logService;
    @Mock
    private NotificationService notificationService;

    private TaskService taskService;

    private User staff;
    private User customer;
    private Unit unit;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                userRepository,
                reservationRepository,
                unitRepository,
                logService,
                notificationService
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
        ReflectionTestUtils.setField(reservation, "id", 1L);
    }

    @Test
    @DisplayName("createCheckInTask creates and saves task with audit log and notification")
    void testCreateCheckInTask_success() {
        when(taskRepository.findByRefCodeAndType("BK-1042", TaskType.CHECK_IN)).thenReturn(Optional.empty());
        when(userRepository.findStaffUsers()).thenReturn(List.of(staff));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 100L);
            return t;
        });

        Task created = taskService.createCheckInTask(reservation);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getType()).isEqualTo(TaskType.CHECK_IN);
        assertThat(created.getRefCode()).isEqualTo("BK-1042");
        assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(created.getWorkDate()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(created.getAssignedStaff().getId()).isEqualTo(2L);

        verify(taskRepository).save(any(Task.class));
        verify(logService).append(
                eq(2L),
                eq(EntityType.TASK),
                eq(100L),
                eq(Action.STATUS_CHANGE),
                eq(null),
                eq("TODO"),
                anyString()
        );
        verify(notificationService).send(
                eq(2L),
                eq("TASK_ASSIGNED"),
                anyString(),
                eq("/tasks/100")
        );
    }

    @Test
    @DisplayName("createCheckInTask is idempotent when task already exists")
    void testCreateCheckInTask_idempotent() {
        Task existing = new Task(TaskType.CHECK_IN, "BK-1042", staff, LocalDate.of(2026, 10, 3), TaskStatus.TODO);
        ReflectionTestUtils.setField(existing, "id", 100L);

        when(taskRepository.findByRefCodeAndType("BK-1042", TaskType.CHECK_IN)).thenReturn(Optional.of(existing));

        Task result = taskService.createCheckInTask(reservation);

        assertThat(result.getId()).isEqualTo(100L);
        verify(taskRepository, never()).save(any(Task.class));
        verify(logService, never()).append(anyLong(), any(), anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateTaskStatus transitions task status and writes audit log")
    void testUpdateTaskStatus_success() {
        Task task = new Task(TaskType.CHECK_IN, "BK-1042", staff, LocalDate.of(2026, 10, 3), TaskStatus.TODO);
        ReflectionTestUtils.setField(task, "id", 100L);

        when(taskRepository.findByIdWithStaff(100L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));

        TaskDto updated = taskService.updateTaskStatus(100L, TaskStatus.IN_PROGRESS, "Started check-in verification", 2L);

        assertThat(updated).isNotNull();
        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        verify(taskRepository).save(task);
        verify(logService).append(
                eq(2L),
                eq(EntityType.TASK),
                eq(100L),
                eq(Action.STATUS_CHANGE),
                eq("TODO"),
                eq("IN_PROGRESS"),
                eq("Started check-in verification")
        );
    }

    @Test
    @DisplayName("getTasks filters tasks and maps metadata correctly")
    void testGetTasks_filtered() {
        Task t1 = new Task(TaskType.CHECK_IN, "BK-1042", staff, LocalDate.of(2026, 10, 3), TaskStatus.TODO);
        ReflectionTestUtils.setField(t1, "id", 1L);

        Task t2 = new Task(TaskType.CLEANING, "S-3", staff, LocalDate.of(2026, 10, 3), TaskStatus.DONE);
        ReflectionTestUtils.setField(t2, "id", 2L);

        when(taskRepository.findFilteredTasks(LocalDate.of(2026, 10, 3), null, null, null)).thenReturn(List.of(t1, t2));
        when(reservationRepository.findByCode("BK-1042")).thenReturn(Optional.of(reservation));

        List<TaskDto> dtos = taskService.getTasks(LocalDate.of(2026, 10, 3), null, null, null);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).type()).isEqualTo(TaskType.CHECK_IN);
        assertThat(dtos.get(0).unitCode()).isEqualTo("S-3");
        assertThat(dtos.get(0).customerName()).isEqualTo("Lan Nguyen");
        assertThat(dtos.get(1).type()).isEqualTo(TaskType.CLEANING);
        assertThat(dtos.get(1).unitCode()).isEqualTo("S-3");
    }

    @Test
    @DisplayName("getTaskById throws ResourceNotFoundException when task not found")
    void testGetTaskById_notFound() {
        when(taskRepository.findByIdWithStaff(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");
    }

    @Test
    @DisplayName("createTask creates and saves custom task")
    void testCreateTask() {
        CreateTaskRequest request = new CreateTaskRequest(
                TaskType.CLEANING,
                "S-3",
                2L,
                LocalDate.of(2026, 10, 5),
                TaskStatus.TODO,
                "S-3",
                null,
                LocalDate.of(2026, 10, 5),
                "Afternoon",
                "Turnover cleaning",
                "Clean unit S-3 after checkout"
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 105L);
            return t;
        });

        TaskDto dto = taskService.createTask(request, 2L);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(105L);
        assertThat(dto.type()).isEqualTo(TaskType.CLEANING);
        assertThat(dto.refCode()).isEqualTo("S-3");

        verify(taskRepository).save(any(Task.class));
        verify(logService).append(eq(2L), eq(EntityType.TASK), eq(105L), eq(Action.STATUS_CHANGE), eq(null), eq("TODO"), anyString());
    }
}
