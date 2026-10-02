package com.storagehub.dto;

import com.storagehub.entity.TaskType;
import com.storagehub.entity.TaskStatus;

import java.time.LocalDate;

public record TaskDto(
        Long id,
        TaskType type,
        String refCode,
        Long assignedStaffId,
        String assignedStaffName,
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
