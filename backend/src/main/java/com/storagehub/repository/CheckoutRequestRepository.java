package com.storagehub.repository;

import com.storagehub.entity.CheckoutRequest;
import com.storagehub.entity.CheckoutRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckoutRequestRepository extends JpaRepository<CheckoutRequest, Long> {

    List<CheckoutRequest> findByReservationId(Long reservationId);

    @Query("SELECT cr FROM CheckoutRequest cr WHERE cr.reservation.id = :reservationId AND cr.status = :status ORDER BY cr.createdAt DESC")
    List<CheckoutRequest> findByReservationIdAndStatus(@Param("reservationId") Long reservationId, @Param("status") CheckoutRequestStatus status);

    @Query("SELECT cr FROM CheckoutRequest cr WHERE cr.reservation.id = :reservationId ORDER BY cr.createdAt DESC")
    List<CheckoutRequest> findLatestByReservationId(@Param("reservationId") Long reservationId);
}
