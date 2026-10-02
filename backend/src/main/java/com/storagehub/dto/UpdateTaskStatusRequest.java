package com.storagehub.dto;

import com.storagehub.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "Task status is required")
        TaskStatus status,
        String reason
) {
}
