package com.storagehub.service;

import com.storagehub.dto.CreateSupportTicketRequest;
import com.storagehub.dto.SupportTicketDto;
import com.storagehub.entity.Action;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Shift;
import com.storagehub.entity.StaffAssignment;
import com.storagehub.entity.SupportTicket;
import com.storagehub.entity.SupportTicketStatus;
import com.storagehub.entity.Unit;
import com.storagehub.entity.User;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.StaffAssignmentRepository;
import com.storagehub.repository.SupportTicketRepository;
import com.storagehub.repository.UnitRepository;
import com.storagehub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final ZoneId ICT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SupportTicketRepository supportTicketRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final LogService logService;

    public TicketService(SupportTicketRepository supportTicketRepository,
                         UnitRepository unitRepository,
                         UserRepository userRepository,
                         ReservationRepository reservationRepository,
                         StaffAssignmentRepository staffAssignmentRepository,
                         TaskService taskService,
                         NotificationService notificationService,
                         LogService logService) {
        this.supportTicketRepository = supportTicketRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.taskService = taskService;
        this.notificationService = notificationService;
        this.logService = logService;
    }

    /**
     * Customer creates and routes a support ticket (Story 5.1).
     */
    public SupportTicketDto createTicket(CreateSupportTicketRequest request, Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + request.unitId()));

        // Validate that customer currently rents this unit (CHECKED_IN reservation)
        List<Reservation> activeReservations = reservationRepository.findByCustomerIdAndStatusIn(
                customerId, List.of(ReservationStatus.CHECKED_IN, ReservationStatus.CHECKOUT_REQUESTED));

        Optional<Reservation> matchingReservation = activeReservations.stream()
                .filter(r -> r.getUnit() != null && r.getUnit().getId().equals(unit.getId()))
                .findFirst();

        if (matchingReservation.isEmpty()) {
            throw new BusinessRuleException("INVALID_UNIT_OR_RENTAL",
                    "You do not have an active rental for unit " + unit.getCode() + ". Support tickets can only be opened for active rentals.");
        }

        Reservation reservation = matchingReservation.get();

        // Generate unique ticket code SR-xxxx
        String code = generateTicketCode();

        // Shift routing based on Unit's Zone and current ICT time
        LocalDate todayIct = LocalDate.now(ICT_ZONE);
        LocalTime nowIct = LocalTime.now(ICT_ZONE);
        Shift currentShift = determineCurrentShift(nowIct);

        Integer zoneId = unit.getZone() != null ? unit.getZone().getId() : null;
        User assignedStaff = resolveAssignedStaff(zoneId, todayIct, currentShift);

        SupportTicket ticket = new SupportTicket(
                code,
                customer,
                unit,
                reservation,
                request.incidentType(),
                request.description().trim(),
                assignedStaff
        );

        ticket = supportTicketRepository.save(ticket);

        // Create SUPPORT task card on Kanban board via TaskService registry
        taskService.createSupportTask(ticket);

        // Audit Log
        logService.append(
                customerId,
                EntityType.TICKET,
                ticket.getId(),
                Action.STATUS_CHANGE,
                null,
                SupportTicketStatus.OPEN.name(),
                "Support ticket " + code + " submitted: " + request.incidentType().name()
        );

        // Customer notification
        notificationService.send(
                customerId,
                "TICKET_RECEIVED",
                "Support request " + code + " received for unit " + unit.getCode() + ". Our team is on it.",
                "/support"
        );

        // Staff notification (if assigned) or Manager notification (fallback)
        if (assignedStaff != null) {
            notificationService.send(
                assignedStaff.getId(),
                "TASK_ASSIGNED",
                "Support " + code + " assigned - " + request.incidentType().name() + " in unit " + unit.getCode(),
                "/tasks"
            );
        } else {
            // Notify Facility Manager
            userRepository.findById(3L).ifPresent(manager ->
                notificationService.send(
                    manager.getId(),
                    "UNASSIGNED_TICKET",
                    "Unassigned ticket " + code + " for unit " + unit.getCode() + " needs staff assignment.",
                    "/support"
                )
            );
        }

        log.info("Created Support Ticket {} ({}) for customer {} assigned to staff {}",
                ticket.getId(), code, customerId, assignedStaff != null ? assignedStaff.getFullName() : "UNASSIGNED");

        return mapToDto(ticket);
    }

    /**
     * Retrieve tickets for a customer or filtered for staff/admin.
     */
    @Transactional(readOnly = true)
    public List<SupportTicketDto> getTickets(Long currentUserId, String role, SupportTicketStatus status, String unitCode) {
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            return supportTicketRepository.findByCustomerId(currentUserId).stream()
                    .map(this::mapToDto)
                    .toList();
        }
        return supportTicketRepository.findFiltered(status, unitCode).stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Get single ticket by ID with permission check.
     */
    @Transactional(readOnly = true)
    public SupportTicketDto getTicketById(Long id, Long currentUserId, String role) {
        SupportTicket ticket = supportTicketRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with id: " + id));

        if ("CUSTOMER".equalsIgnoreCase(role) && !ticket.getCustomer().getId().equals(currentUserId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not have permission to view this support ticket");
        }

        return mapToDto(ticket);
    }

    private User resolveAssignedStaff(Integer zoneId, LocalDate workDate, Shift shift) {
        if (zoneId != null) {
            Optional<StaffAssignment> assignment = staffAssignmentRepository.findByZoneAndDateAndShift(zoneId, workDate, shift);
            if (assignment.isPresent()) {
                return assignment.get().getStaff();
            }
        }

        // Fallback 1: Any staff on duty today during this shift
        List<StaffAssignment> shiftStaff = staffAssignmentRepository.findByDateAndShift(workDate, shift);
        if (!shiftStaff.isEmpty()) {
            return shiftStaff.get(0).getStaff();
        }

        // Fallback 2: Any staff working today
        List<StaffAssignment> todayStaff = staffAssignmentRepository.findByWorkDate(workDate);
        if (!todayStaff.isEmpty()) {
            return todayStaff.get(0).getStaff();
        }

        // Fallback 3: First available staff user in system
        List<User> staffUsers = userRepository.findStaffUsers();
        if (!staffUsers.isEmpty()) {
            return staffUsers.get(0);
        }

        return null;
    }

    private Shift determineCurrentShift(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 6 && hour < 14) {
            return Shift.MORNING;
        } else if (hour >= 14 && hour < 22) {
            return Shift.AFTERNOON;
        } else {
            return Shift.EVENING;
        }
    }

    private synchronized String generateTicketCode() {
        long count = supportTicketRepository.countAllTickets() + 32; // Offset to match demo sequence SR-0032...
        return String.format("SR-%04d", count + 1);
    }

    public SupportTicketDto mapToDto(SupportTicket ticket) {
        return new SupportTicketDto(
                ticket.getId(),
                ticket.getCode(),
                ticket.getCustomer() != null ? ticket.getCustomer().getId() : null,
                ticket.getCustomer() != null ? ticket.getCustomer().getFullName() : null,
                ticket.getUnit() != null ? ticket.getUnit().getId() : null,
                ticket.getUnit() != null ? ticket.getUnit().getCode() : null,
                ticket.getReservation() != null ? ticket.getReservation().getId() : null,
                ticket.getReservation() != null ? ticket.getReservation().getCode() : null,
                ticket.getIncidentType(),
                ticket.getStatus(),
                ticket.getDescription(),
                ticket.getAssignedStaff() != null ? ticket.getAssignedStaff().getId() : null,
                ticket.getAssignedStaff() != null ? ticket.getAssignedStaff().getFullName() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
