package com.storagehub.repository;

import com.storagehub.entity.PolicyStatus;
import com.storagehub.entity.RentalPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RentalPolicyRepository extends JpaRepository<RentalPolicy, Integer> {

    Optional<RentalPolicy> findByVersion(String version);

    Optional<RentalPolicy> findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            PolicyStatus status, LocalDate date);
}
