package com.storagehub.dto;

import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.TaskType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotNull(message = "Task type is required")
        TaskType type,
        String refCode,
        Long assignedStaffId,
        @NotNull(message = "Work date is required")
        LocalDate workDate,
        TaskStatus status,
        String unitCode,
        String customerName,
        LocalDate dueDate,
        String timeSlot,
        String title,
        String description
) {
}
