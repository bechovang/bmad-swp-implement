package com.storagehub.repository;

import com.storagehub.entity.Reservation;
import com.storagehub.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCode(String code);

    List<Reservation> findByUnit_IdAndStatusIn(Long unitId, Collection<ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r WHERE r.unit.id = :unitId ORDER BY r.endDate DESC")
    List<Reservation> findByUnitIdOrderByEndDateDesc(@Param("unitId") Long unitId);
}
