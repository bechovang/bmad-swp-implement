package com.storagehub.repository;

import com.storagehub.entity.Task;
import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t JOIN FETCH t.assignedStaff WHERE t.id = :id")
    Optional<Task> findByIdWithStaff(@Param("id") Long id);

    @Query("SELECT t FROM Task t JOIN FETCH t.assignedStaff " +
            "WHERE (:workDate IS NULL OR t.workDate = :workDate) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:assignedStaffId IS NULL OR t.assignedStaff.id = :assignedStaffId) " +
            "ORDER BY t.workDate ASC, t.id ASC")
    List<Task> findFilteredTasks(
            @Param("workDate") LocalDate workDate,
            @Param("type") TaskType type,
            @Param("status") TaskStatus status,
            @Param("assignedStaffId") Long assignedStaffId
    );

    List<Task> findAllByWorkDate(LocalDate workDate);

    List<Task> findAllByAssignedStaffId(Long assignedStaffId);

    List<Task> findAllByRefCode(String refCode);

    Optional<Task> findByRefCodeAndType(String refCode, TaskType type);
}
