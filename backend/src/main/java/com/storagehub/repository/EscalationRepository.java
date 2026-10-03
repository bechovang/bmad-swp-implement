package com.storagehub.repository;

import com.storagehub.entity.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    Optional<Escalation> findByTicketId(Long ticketId);
    Optional<Escalation> findByTicketCode(String ticketCode);
}
