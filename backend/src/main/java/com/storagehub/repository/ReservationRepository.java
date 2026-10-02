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
}
