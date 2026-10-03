package com.storagehub.service;

import com.storagehub.dto.CheckoutRequestDto;
import com.storagehub.dto.CreateCheckoutRequest;
import com.storagehub.entity.*;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.CheckoutRequestRepository;
import com.storagehub.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final ZoneId ICT = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES = List.of(
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.RESERVED,
            ReservationStatus.CHECKED_IN
    );

    private final ReservationRepository reservationRepository;
    private final CheckoutRequestRepository checkoutRequestRepository;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final LogService logService;

    public CheckoutService(ReservationRepository reservationRepository,
                           CheckoutRequestRepository checkoutRequestRepository,
                           TaskService taskService,
                           NotificationService notificationService,
                           LogService logService) {
        this.reservationRepository = reservationRepository;
        this.checkoutRequestRepository = checkoutRequestRepository;
        this.taskService = taskService;
        this.notificationService = notificationService;
        this.logService = logService;
    }

    /**
     * Submit or update a checkout request for a checked-in rental (Story 6.1).
     */
    public CheckoutRequestDto requestCheckout(Long reservationId, CreateCheckoutRequest request, Long currentUserId, String role) {
        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        // Ownership check
        if ("CUSTOMER".equalsIgnoreCase(role) && (res.getCustomer() == null || !res.getCustomer().getId().equals(currentUserId))) {
            throw new AccessDeniedException("You do not have permission to request checkout for this reservation");
        }

        // State check: must be active rental
        if (res.getStatus() != ReservationStatus.CHECKED_IN && res.getStatus() != ReservationStatus.CHECKOUT_REQUESTED) {
            throw new BusinessRuleException("INVALID_STATE", "Checkout request can only be submitted for active rentals.");
        }

        LocalDate requestedDate = request.requestedDate();
        LocalDate today = LocalDate.now(ICT);
        if (requestedDate.isBefore(today)) {
            throw new BusinessRuleException("INVALID_DATE", "Requested checkout date cannot be in the past.");
        }

        String unitCode = res.getUnit() != null ? res.getUnit().getCode() : "";
        Long unitId = res.getUnit() != null ? res.getUnit().getId() : 0L;

        // Conflict boundary check against next reservation
        List<Reservation> upcoming = reservationRepository.findUpcomingReservationsForUnit(
                unitId,
                res.getId(),
                ACTIVE_RESERVATION_STATUSES,
                res.getStartDate()
        );

        if (!upcoming.isEmpty()) {
            Reservation nextBooking = upcoming.get(0);
            LocalDate conflictStartDate = nextBooking.getStartDate();
            if (!requestedDate.isBefore(conflictStartDate)) {
                LocalDate latestCheckout = conflictStartDate.minusDays(1);
                throw new BusinessRuleException(
                        "CHECKOUT_DATE_CONFLICT",
                        "Can't schedule checkout to " + requestedDate + " — Unit " + unitCode + " has an upcoming reservation starting " + conflictStartDate + ". Latest possible checkout is " + latestCheckout + ". Pick another date."
                );
            }
        }

        // Supersede previous pending checkout requests (latest request wins)
        List<CheckoutRequest> existingPending = checkoutRequestRepository.findByReservationIdAndStatus(res.getId(), CheckoutRequestStatus.PENDING);
        for (CheckoutRequest pending : existingPending) {
            pending.setStatus(CheckoutRequestStatus.CANCELLED);
        }
        checkoutRequestRepository.saveAll(existingPending);

        // Create new checkout request
        CheckoutRequest checkoutRequest = new CheckoutRequest(
                res,
                requestedDate,
                CheckoutRequestStatus.PENDING,
                request.notes() != null ? request.notes().trim() : null
        );
        checkoutRequest = checkoutRequestRepository.save(checkoutRequest);

        // Update reservation status to CHECKOUT_REQUESTED
        res.setStatus(ReservationStatus.CHECKOUT_REQUESTED);
        reservationRepository.save(res);

        // Spawn or update CHECKOUT task via task registry
        taskService.createCheckoutTask(res, requestedDate);

        // Notify customer
        notificationService.send(
                res.getCustomer().getId(),
                "CHECKOUT_REQUESTED",
                "Checkout scheduled for Unit " + unitCode + " on " + requestedDate + ". Please bring your key and padlock to the front desk.",
                "/rentals/" + res.getId()
        );

        // Log in Activity Log
        logService.append(
                currentUserId,
                EntityType.RESERVATION,
                res.getId(),
                Action.STATUS_CHANGE,
                ReservationStatus.CHECKED_IN.name(),
                ReservationStatus.CHECKOUT_REQUESTED.name(),
                "Checkout requested for " + requestedDate
        );

        log.info("Created CheckoutRequest {} for reservation {} on date {}", checkoutRequest.getId(), res.getCode(), requestedDate);
        return mapToDto(checkoutRequest);
    }

    /**
     * Get active/latest checkout request for a reservation.
     */
    @Transactional(readOnly = true)
    public CheckoutRequestDto getCheckoutRequest(Long reservationId, Long currentUserId, String role) {
        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        if ("CUSTOMER".equalsIgnoreCase(role) && (res.getCustomer() == null || !res.getCustomer().getId().equals(currentUserId))) {
            throw new AccessDeniedException("Access denied to reservation checkout request");
        }

        List<CheckoutRequest> list = checkoutRequestRepository.findLatestByReservationId(reservationId);
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("No checkout request found for reservation " + reservationId);
        }

        return mapToDto(list.get(0));
    }

    public CheckoutRequestDto mapToDto(CheckoutRequest cr) {
        Reservation res = cr.getReservation();
        return new CheckoutRequestDto(
                cr.getId(),
                res != null ? res.getId() : null,
                res != null ? res.getCode() : null,
                res != null && res.getUnit() != null ? res.getUnit().getId() : null,
                res != null && res.getUnit() != null ? res.getUnit().getCode() : null,
                cr.getRequestedDate(),
                cr.getStatus(),
                cr.getNotes(),
                cr.getCreatedAt(),
                cr.getUpdatedAt()
        );
    }
}
