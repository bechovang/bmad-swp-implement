package com.storagehub.repository;

import com.storagehub.entity.SupportTicket;
import com.storagehub.entity.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByCode(String code);

    @Query("SELECT t FROM SupportTicket t JOIN FETCH t.customer JOIN FETCH t.unit LEFT JOIN FETCH t.reservation LEFT JOIN FETCH t.assignedStaff WHERE t.id = :id")
    Optional<SupportTicket> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM SupportTicket t JOIN FETCH t.customer JOIN FETCH t.unit LEFT JOIN FETCH t.reservation LEFT JOIN FETCH t.assignedStaff WHERE t.customer.id = :customerId ORDER BY t.createdAt DESC")
    List<SupportTicket> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT t FROM SupportTicket t JOIN FETCH t.customer JOIN FETCH t.unit LEFT JOIN FETCH t.reservation LEFT JOIN FETCH t.assignedStaff WHERE (:status IS NULL OR t.status = :status) AND (:unitCode IS NULL OR t.unit.code = :unitCode) ORDER BY t.createdAt DESC")
    List<SupportTicket> findFiltered(@Param("status") SupportTicketStatus status, @Param("unitCode") String unitCode);

    @Query("SELECT COUNT(t) FROM SupportTicket t")
    long countAllTickets();
}
