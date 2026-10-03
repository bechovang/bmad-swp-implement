package com.storagehub.service;

import com.storagehub.dto.*;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class InspectionService {

    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);

    private final InspectionRepository inspectionRepository;
    private final ReservationRepository reservationRepository;
    private final CheckoutRequestRepository checkoutRequestRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    public InspectionService(
            InspectionRepository inspectionRepository,
            ReservationRepository reservationRepository,
            CheckoutRequestRepository checkoutRequestRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            LogService logService
    ) {
        this.inspectionRepository = inspectionRepository;
        this.reservationRepository = reservationRepository;
        this.checkoutRequestRepository = checkoutRequestRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.logService = logService;
    }

    /**
     * Submit or update 4-point unit inspection and key reception checklist for a reservation.
     */
    @Transactional
    public CheckoutTaskDetailDto submitInspection(
            Long reservationId,
            SubmitInspectionRequest request,
            Long currentUserId,
            String role
    ) {
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Only facility staff or managers can submit unit inspections");
        }

        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        User staff = null;
        if (currentUserId != null) {
            staff = userRepository.findById(currentUserId).orElse(null);
        }

        // Update CheckoutRequest checklist fields if present
        List<CheckoutRequest> crList = checkoutRequestRepository.findLatestByReservationId(reservationId);
        CheckoutRequest latestCr = crList.isEmpty() ? null : crList.get(0);
        if (latestCr != null) {
            if (request.keyReturned() != null) {
                latestCr.setKeyReturned(request.keyReturned());
            }
            if (request.unitEmptied() != null) {
                latestCr.setUnitEmptied(request.unitEmptied());
            }
            if (request.generalNotes() != null && !request.generalNotes().isBlank()) {
                latestCr.setNotes(request.generalNotes().trim());
            }
            checkoutRequestRepository.save(latestCr);
        }

        // Wipe previous inspections for idempotency/re-inspection
        inspectionRepository.deleteByReservationId(reservationId);
        inspectionRepository.flush();

        List<Inspection> savedInspections = new ArrayList<>();
        if (request.items() != null) {
            for (InspectionItemInput input : request.items()) {
                Inspection insp = new Inspection(
                        res,
                        input.item(),
                        input.result(),
                        input.note() != null ? input.note().trim() : null,
                        staff
                );
                savedInspections.add(inspectionRepository.save(insp));
            }
        }

        // Log to Activity Log
        logService.append(
                currentUserId,
                EntityType.RESERVATION,
                res.getId(),
                Action.INSPECTION_COMPLETED,
                null,
                null,
                "Unit inspection completed for " + (res.getUnit() != null ? res.getUnit().getCode() : "Unit")
        );

        log.info("Saved {} inspection records for reservation {}", savedInspections.size(), res.getCode());
        return buildCheckoutTaskDetailDto(res, latestCr, savedInspections);
    }

    /**
     * Get checkout task detail by reservation ID.
     */
    @Transactional(readOnly = true)
    public CheckoutTaskDetailDto getCheckoutTaskDetailByReservation(Long reservationId, Long currentUserId, String role) {
        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        if ("CUSTOMER".equalsIgnoreCase(role) && (res.getCustomer() == null || !res.getCustomer().getId().equals(currentUserId))) {
            throw new AccessDeniedException("Access denied to checkout inspection details");
        }

        List<CheckoutRequest> crList = checkoutRequestRepository.findLatestByReservationId(reservationId);
        CheckoutRequest latestCr = crList.isEmpty() ? null : crList.get(0);
        List<Inspection> inspections = inspectionRepository.findByReservationIdOrderByIdAsc(reservationId);

        return buildCheckoutTaskDetailDto(res, latestCr, inspections);
    }

    /**
     * Get checkout task detail by Task ID.
     */
    @Transactional(readOnly = true)
    public CheckoutTaskDetailDto getCheckoutTaskDetailByTaskId(Long taskId, Long currentUserId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        String refCode = task.getRefCode();
        Reservation res = reservationRepository.findByCode(refCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for refCode: " + refCode));

        List<CheckoutRequest> crList = checkoutRequestRepository.findLatestByReservationId(res.getId());
        CheckoutRequest latestCr = crList.isEmpty() ? null : crList.get(0);
        List<Inspection> inspections = inspectionRepository.findByReservationIdOrderByIdAsc(res.getId());

        CheckoutTaskDetailDto dto = buildCheckoutTaskDetailDto(res, latestCr, inspections);
        return new CheckoutTaskDetailDto(
                task.getId(),
                task.getStatus().name(),
                dto.reservationId(),
                dto.reservationCode(),
                dto.customerId(),
                dto.customerName(),
                dto.customerPhone(),
                dto.unitId(),
                dto.unitCode(),
                dto.requestedDate(),
                dto.keyReturned(),
                dto.unitEmptied(),
                dto.inspections(),
                dto.hasMajorDamage(),
                dto.majorItems()
        );
    }

    private CheckoutTaskDetailDto buildCheckoutTaskDetailDto(
            Reservation res,
            CheckoutRequest cr,
            List<Inspection> inspections
    ) {
        List<InspectionItemDto> inspectionDtos = inspections.stream()
                .map(this::mapInspectionToDto)
                .toList();

        List<InspectionItem> majorItems = inspections.stream()
                .filter(i -> i.getResult() == InspectionResult.MAJOR)
                .map(Inspection::getItem)
                .toList();

        boolean hasMajor = !majorItems.isEmpty();

        return new CheckoutTaskDetailDto(
                null,
                null,
                res.getId(),
                res.getCode(),
                res.getCustomer() != null ? res.getCustomer().getId() : null,
                res.getCustomer() != null ? res.getCustomer().getFullName() : null,
                res.getCustomer() != null ? res.getCustomer().getPhone() : null,
                res.getUnit() != null ? res.getUnit().getId() : null,
                res.getUnit() != null ? res.getUnit().getCode() : null,
                cr != null ? cr.getRequestedDate() : res.getEndDate(),
                cr != null && cr.getKeyReturned() != null ? cr.getKeyReturned() : false,
                cr != null && cr.getUnitEmptied() != null ? cr.getUnitEmptied() : false,
                inspectionDtos,
                hasMajor,
                majorItems
        );
    }

    public InspectionItemDto mapInspectionToDto(Inspection i) {
        return new InspectionItemDto(
                i.getId(),
                i.getItem(),
                i.getResult(),
                i.getNote(),
                i.getInspectorStaff() != null ? i.getInspectorStaff().getId() : null,
                i.getInspectorStaff() != null ? i.getInspectorStaff().getFullName() : null,
                i.getCreatedAt()
        );
    }
}
