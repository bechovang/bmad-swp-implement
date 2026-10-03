package com.storagehub.repository;

import com.storagehub.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    List<Inspection> findByReservationIdOrderByIdAsc(Long reservationId);

    List<Inspection> findBySettlementIdOrderByIdAsc(Long settlementId);

    void deleteByReservationId(Long reservationId);
}
