package com.storagehub.repository;

import com.storagehub.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReceiptCode(String receiptCode);

    List<Payment> findByReservationId(Long reservationId);

    @Query("SELECT p FROM Payment p WHERE p.reservation.id = :reservationId ORDER BY p.id ASC")
    List<Payment> findByReservationIdOrderByCreatedAtAsc(@Param("reservationId") Long reservationId);

    List<Payment> findByReservation_IdOrderByIdAsc(Long reservationId);
}
