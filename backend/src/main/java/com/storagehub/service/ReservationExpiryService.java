package com.storagehub.service;

import com.storagehub.entity.Action;
import com.storagehub.entity.Contract;
import com.storagehub.entity.ContractStatus;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import com.storagehub.entity.Unit;
import com.storagehub.entity.UnitStatus;
import com.storagehub.repository.ContractRepository;
import com.storagehub.repository.ReservationRepository;
import com.storagehub.repository.UnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class ReservationExpiryService {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryService.class);
    public static final ZoneId ICT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReservationRepository reservationRepository;
    private final UnitRepository unitRepository;
    private final ContractRepository contractRepository;
    private final LogService logService;
    private final NotificationService notificationService;

    public ReservationExpiryService(
            ReservationRepository reservationRepository,
            UnitRepository unitRepository,
            ContractRepository contractRepository,
            LogService logService,
            NotificationService notificationService
    ) {
        this.reservationRepository = reservationRepository;
        this.unitRepository = unitRepository;
        this.contractRepository = contractRepository;
        this.logService = logService;
        this.notificationService = notificationService;
    }

    /**
     * Checks if a reservation has passed its start date in Asia/Ho_Chi_Minh timezone
     * without check-in (status == RESERVED and startDate < todayInICT), and lazily expires it.
     *
     * @param reservation the reservation to check
     * @return true if reservation was expired, false otherwise
     */
    public boolean checkAndExpireReservation(Reservation reservation) {
        if (reservation == null) {
            return false;
        }

        LocalDate todayInICT = LocalDate.now(ICT_ZONE);
        if (reservation.getStatus() == ReservationStatus.RESERVED
                && reservation.getStartDate() != null
                && reservation.getStartDate().isBefore(todayInICT)) {
            expireReservation(reservation);
            return true;
        }
        return false;
    }

    /**
     * Idempotently expires a RESERVED reservation, releases its unit to AVAILABLE,
     * closes draft/printed contracts, logs audit activity, and notifies the customer.
     */
    public void expireReservation(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            return;
        }

        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);

        Unit unit = reservation.getUnit();
        if (unit != null && unit.getStatus() == UnitStatus.RESERVED) {
            unit.setStatus(UnitStatus.AVAILABLE);
            unitRepository.save(unit);
        }

        contractRepository.findLatestByReservationId(reservation.getId()).ifPresent(contract -> {
            if (contract.getStatus() == ContractStatus.DRAFT || contract.getStatus() == ContractStatus.PRINTED) {
                contract.setStatus(ContractStatus.CLOSED);
                contractRepository.save(contract);
            }
        });

        Long customerId = reservation.getCustomer() != null ? reservation.getCustomer().getId() : null;
        if (customerId != null) {
            logService.append(
                    customerId,
                    EntityType.RESERVATION,
                    reservation.getId(),
                    Action.STATUS_CHANGE,
                    ReservationStatus.RESERVED.name(),
                    ReservationStatus.EXPIRED.name(),
                    "Reservation expired due to no-show on check-in date"
            );

            notificationService.send(
                    customerId,
                    "RESERVATION_EXPIRED",
                    "Reservation " + reservation.getCode() + " expired due to no-show. Deposit forfeited.",
                    "/rentals/" + reservation.getId()
            );
        }

        log.info("Reservation {} lazily expired due to no-show on start date {}", reservation.getCode(), reservation.getStartDate());
    }

    /**
     * Checks and expires all past RESERVED reservations for a specific customer.
     */
    public void expirePastReservationsForCustomer(Long customerId) {
        LocalDate todayInICT = LocalDate.now(ICT_ZONE);
        List<Reservation> pastReserved = reservationRepository.findByCustomerIdAndStatusAndStartDateBefore(
                customerId, ReservationStatus.RESERVED, todayInICT
        );
        for (Reservation r : pastReserved) {
            expireReservation(r);
        }
    }

    /**
     * Checks and expires all past RESERVED reservations for a specific unit.
     */
    public void expirePastReservationsForUnit(Long unitId) {
        LocalDate todayInICT = LocalDate.now(ICT_ZONE);
        List<Reservation> pastReserved = reservationRepository.findByUnitIdAndStatusAndStartDateBefore(
                unitId, ReservationStatus.RESERVED, todayInICT
        );
        for (Reservation r : pastReserved) {
            expireReservation(r);
        }
    }

    /**
     * Checks and expires all past RESERVED reservations across all units.
     */
    public void expireAllPastReservedReservations() {
        LocalDate todayInICT = LocalDate.now(ICT_ZONE);
        List<Reservation> pastReserved = reservationRepository.findByStatusAndStartDateBefore(
                ReservationStatus.RESERVED, todayInICT
        );
        for (Reservation r : pastReserved) {
            expireReservation(r);
        }
    }
}
