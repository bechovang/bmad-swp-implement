package com.storagehub.repository;

import com.storagehub.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReceiptCode(String receiptCode);

    List<Payment> findByReservationId(Long reservationId);
}
