package com.storagehub.repository;

import com.storagehub.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByCode(String code);

    List<Contract> findByReservation_Id(Long reservationId);

    Optional<Contract> findFirstByReservation_IdAndIsLatest(Long reservationId, Integer isLatest);

    @Query("SELECT c FROM Contract c WHERE c.reservation.id = :reservationId AND c.isLatest = 1")
    Optional<Contract> findLatestByReservationId(@Param("reservationId") Long reservationId);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.reservation.id = :reservationId")
    long countByReservationId(@Param("reservationId") Long reservationId);
}
