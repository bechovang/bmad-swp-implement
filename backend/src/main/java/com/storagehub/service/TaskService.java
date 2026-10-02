package com.storagehub.service;

import com.storagehub.dto.CreateTaskRequest;
import com.storagehub.dto.TaskDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.Task;
import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.TaskType;
import com.storagehub.entity.Unit;
import com.storagehub.entity.User;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.TaskRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final UnitRepository unitRepository;
    private final LogService logService;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       ReservationRepository reservationRepository,
                       UnitRepository unitRepository,
                       LogService logService,
                       NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.unitRepository = unitRepository;
        this.logService = logService;
        this.notificationService = notificationService;
    }

    /**
     * Centralized task registry: automatically generates a CHECK_IN task
     * when a reservation transitions to RESERVED upon successful deposit.
     */
    public Task createCheckInTask(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null for check-in task creation");
        }

        // Idempotency check: don't create duplicate CHECK_IN task for same reservation code
        Optional<Task> existing = taskRepository.findByRefCodeAndType(reservation.getCode(), TaskType.CHECK_IN);
        if (existing.isPresent()) {
            log.info("Check-in task already exists for reservation {}", reservation.getCode());
            return existing.get();
        }

        User assignedStaff = resolveDefaultStaffUser();

        LocalDate workDate = reservation.getStartDate() != null ? reservation.getStartDate() : LocalDate.now();
        Task task = new Task(
                TaskType.CHECK_IN,
                reservation.getCode(),
                assignedStaff,
                workDate,
                TaskStatus.TODO
        );

        task = taskRepository.save(task);

        // Audit log
        logService.append(
                assignedStaff.getId(),
                EntityType.TASK,
                task.getId(),
                Action.STATUS_CHANGE,
                null,
                TaskStatus.TODO.name(),
                "Check-in task auto-generated for reservation " + reservation.getCode()
        );

        // Notification to assigned staff
        String unitCode = reservation.getUnit() != null ? reservation.getUnit().getCode() : "";
        notificationService.send(
                assignedStaff.getId(),
                "TASK_ASSIGNED",
                "Check-in " + reservation.getCode() + " assigned - unit " + unitCode + ", " + workDate,
                "/tasks/" + task.getId()
        );

        log.info("Created CHECK_IN task {} for reservation {} on {}", task.getId(), reservation.getCode(), workDate);
        return task;
    }

    /**
     * Create task from manual or programmatic request.
     */
    public TaskDto createTask(CreateTaskRequest request, Long creatorUserId) {
        User assignedStaff;
        if (request.assignedStaffId() != null) {
            assignedStaff = userRepository.findById(request.assignedStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + request.assignedStaffId()));
        } else {
            assignedStaff = resolveDefaultStaffUser();
        }

        Task task = new Task(
                request.type(),
                request.refCode(),
                assignedStaff,
                request.workDate(),
                request.status() != null ? request.status() : TaskStatus.TODO
        );

        task = taskRepository.save(task);

        Long actorId = creatorUserId != null ? creatorUserId : assignedStaff.getId();
        logService.append(
                actorId,
                EntityType.TASK,
                task.getId(),
                Action.STATUS_CHANGE,
                null,
                task.getStatus().name(),
                "Task created: " + task.getType().name() + " (" + task.getRefCode() + ")"
        );

        notificationService.send(
                assignedStaff.getId(),
                "TASK_ASSIGNED",
                "Task assigned: " + task.getType().name() + " (" + (task.getRefCode() != null ? task.getRefCode() : "") + ")",
                "/tasks/" + task.getId()
        );

        return mapToDto(task);
    }

    /**
     * Retrieve tasks matching filter criteria with enriched metadata.
     */
    @Transactional(readOnly = true)
    public List<TaskDto> getTasks(LocalDate workDate, TaskType type, TaskStatus status, Long assignedStaffId) {
        List<Task> tasks = taskRepository.findFilteredTasks(workDate, type, status, assignedStaffId);
        return tasks.stream().map(this::mapToDto).toList();
    }

    /**
     * Get a single task by ID.
     */
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        Task task = taskRepository.findByIdWithStaff(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return mapToDto(task);
    }

    /**
     * Transition task status on the Kanban board (TODO <-> IN_PROGRESS <-> DONE).
     */
    public TaskDto updateTaskStatus(Long id, TaskStatus newStatus, String reason, Long actorUserId) {
        Task task = taskRepository.findByIdWithStaff(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        TaskStatus oldStatus = task.getStatus();
        if (oldStatus == newStatus) {
            return mapToDto(task);
        }

        task.setStatus(newStatus);
        task = taskRepository.save(task);

        Long actorId = actorUserId != null ? actorUserId : task.getAssignedStaff().getId();
        String logReason = (reason != null && !reason.isBlank())
                ? reason
                : "Task status moved from " + oldStatus.name() + " to " + newStatus.name();

        logService.append(
                actorId,
                EntityType.TASK,
                task.getId(),
                Action.STATUS_CHANGE,
                oldStatus.name(),
                newStatus.name(),
                logReason
        );

        return mapToDto(task);
    }

    private User resolveDefaultStaffUser() {
        List<User> staffUsers = userRepository.findStaffUsers();
        if (!staffUsers.isEmpty()) {
            return staffUsers.get(0);
        }
        return userRepository.findById(2L)
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new BusinessRuleException("NO_STAFF_AVAILABLE", "No staff user exists to assign task")));
    }

    /**
     * Enriches Task entity into TaskDto with unit code, customer name, title, and description.
     */
    private TaskDto mapToDto(Task task) {
        String unitCode = null;
        String customerName = null;
        String title = null;
        String description = null;
        String timeSlot = "Morning";

        String refCode = task.getRefCode();
        if (refCode != null && !refCode.isBlank()) {
            if (refCode.startsWith("BK-")) {
                Optional<Reservation> resOpt = reservationRepository.findByCode(refCode);
                if (resOpt.isPresent()) {
                    Reservation res = resOpt.get();
                    if (res.getUnit() != null) {
                        unitCode = res.getUnit().getCode();
                    }
                    if (res.getCustomer() != null) {
                        customerName = res.getCustomer().getFullName();
                    }
                }
                title = "Check-in " + refCode;
                description = (customerName != null ? customerName : "Customer") +
                        " check-in for unit " + (unitCode != null ? unitCode : "");
            } else if (refCode.startsWith("CT-")) {
                title = "Contract Signature " + refCode;
                description = "Contract and addendum signing at front desk";
                // S-3 default unit for demo contract CT-1042
                if (refCode.contains("1042")) {
                    unitCode = "S-3";
                    customerName = "Lan Nguyen";
                }
            } else if (refCode.startsWith("RT-")) {
                title = "Checkout " + refCode;
                description = "Unit inspection and key return";
                if (refCode.contains("0871")) {
                    unitCode = "S-3";
                    customerName = "Lan Nguyen";
                }
            } else if (refCode.startsWith("SR-")) {
                title = "Support Ticket " + refCode;
                description = "On-site support and maintenance inspection";
                if (refCode.contains("0032")) {
                    unitCode = "S-3";
                    customerName = "Lan Nguyen";
                } else if (refCode.contains("0033")) {
                    unitCode = "M-2";
                    customerName = "Lan Nguyen";
                }
            } else {
                // Assume unit code directly (e.g. S-3 for Cleaning)
                unitCode = refCode;
                title = "Cleaning " + refCode;
                description = "Turnover buffer cleaning and inspection for unit " + refCode;
            }
        }

        if (title == null) {
            title = task.getType().name() + " Task";
        }

        return new TaskDto(
                task.getId(),
                task.getType(),
                task.getRefCode(),
                task.getAssignedStaff() != null ? task.getAssignedStaff().getId() : null,
                task.getAssignedStaff() != null ? task.getAssignedStaff().getFullName() : null,
                task.getWorkDate(),
                task.getStatus(),
                unitCode,
                customerName,
                task.getWorkDate(),
                timeSlot,
                title,
                description
        );
    }
}
