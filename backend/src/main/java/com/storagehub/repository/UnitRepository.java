package com.storagehub.repository;

import com.storagehub.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    @Query("SELECT u FROM Unit u JOIN FETCH u.unitType JOIN FETCH u.zone z JOIN FETCH z.facility WHERE u.code = :code")
    Optional<Unit> findByCodeWithDetails(@Param("code") String code);

    Optional<Unit> findByCode(String code);

    boolean existsByCode(String code);
}
