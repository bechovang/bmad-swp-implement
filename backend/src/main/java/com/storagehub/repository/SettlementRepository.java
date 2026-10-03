package com.storagehub.repository;

import com.storagehub.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("SELECT s FROM Settlement s " +
           "LEFT JOIN FETCH s.reservation r " +
           "LEFT JOIN FETCH s.staff st " +
           "LEFT JOIN FETCH s.contract c " +
           "WHERE s.reservation.id = :reservationId")
    Optional<Settlement> findByReservationIdWithDetails(@Param("reservationId") Long reservationId);

    Optional<Settlement> findByReservation_Id(Long reservationId);

    Optional<Settlement> findByReceiptCode(String receiptCode);
}
