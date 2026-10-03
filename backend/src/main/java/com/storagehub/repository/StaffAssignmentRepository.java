package com.storagehub.repository;

import com.storagehub.entity.Shift;
import com.storagehub.entity.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    @Query("SELECT sa FROM StaffAssignment sa JOIN FETCH sa.staff JOIN FETCH sa.zone WHERE sa.zone.id = :zoneId AND sa.workDate = :workDate AND sa.shift = :shift")
    Optional<StaffAssignment> findByZoneAndDateAndShift(@Param("zoneId") Integer zoneId,
                                                         @Param("workDate") LocalDate workDate,
                                                         @Param("shift") Shift shift);

    @Query("SELECT sa FROM StaffAssignment sa JOIN FETCH sa.staff JOIN FETCH sa.zone WHERE sa.workDate = :workDate AND sa.shift = :shift")
    List<StaffAssignment> findByDateAndShift(@Param("workDate") LocalDate workDate,
                                             @Param("shift") Shift shift);

    @Query("SELECT sa FROM StaffAssignment sa JOIN FETCH sa.staff JOIN FETCH sa.zone WHERE sa.workDate = :workDate")
    List<StaffAssignment> findByWorkDate(@Param("workDate") LocalDate workDate);
}
