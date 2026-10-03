package com.storagehub.repository;

import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCode(String code);

    @Query("SELECT r FROM Reservation r WHERE r.customer.id = :customerId ORDER BY r.id DESC")
    List<Reservation> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId);

    List<Reservation> findByCustomer_IdOrderByIdDesc(Long customerId);

    @Query("SELECT r FROM Reservation r WHERE r.customer.id = :customerId AND r.status IN :statuses")
    List<Reservation> findByCustomerIdAndStatusIn(@Param("customerId") Long customerId, @Param("statuses") Collection<ReservationStatus> statuses);

    List<Reservation> findByUnit_IdAndStatusIn(Long unitId, Collection<ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r WHERE r.unit.id = :unitId ORDER BY r.endDate DESC")
    List<Reservation> findByUnitIdOrderByEndDateDesc(@Param("unitId") Long unitId);

    @Query("SELECT r FROM Reservation r WHERE r.unit.id = :unitId AND r.status IN :statuses AND r.startDate < :endDate AND r.endDate > :startDate")
    List<Reservation> findConflictingReservations(
            @Param("unitId") Long unitId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.startDate < :date")
    List<Reservation> findByStatusAndStartDateBefore(
            @Param("status") ReservationStatus status,
            @Param("date") LocalDate date
    );

    @Query("SELECT r FROM Reservation r WHERE r.customer.id = :customerId AND r.status = :status AND r.startDate < :date")
    List<Reservation> findByCustomerIdAndStatusAndStartDateBefore(
            @Param("customerId") Long customerId,
            @Param("status") ReservationStatus status,
            @Param("date") LocalDate date
    );

    @Query("SELECT r FROM Reservation r WHERE r.unit.id = :unitId AND r.status = :status AND r.startDate < :date")
    List<Reservation> findByUnitIdAndStatusAndStartDateBefore(
            @Param("unitId") Long unitId,
            @Param("status") ReservationStatus status,
            @Param("date") LocalDate date
    );

    @Query("SELECT r FROM Reservation r WHERE r.unit.id = :unitId AND r.id <> :excludeReservationId AND r.status IN :statuses AND r.startDate >= :afterDate ORDER BY r.startDate ASC")
    List<Reservation> findUpcomingReservationsForUnit(
            @Param("unitId") Long unitId,
            @Param("excludeReservationId") Long excludeReservationId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("afterDate") LocalDate afterDate
    );
}
