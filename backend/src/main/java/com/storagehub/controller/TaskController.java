package com.storagehub.controller;

import com.storagehub.dto.CreateTaskRequest;
import com.storagehub.dto.TaskDto;
import com.storagehub.dto.UpdateTaskStatusRequest;
import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.TaskType;
import com.storagehub.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@PreAuthorize("hasAnyRole('STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getTasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assignedStaffId
    ) {
        List<TaskDto> tasks = taskService.getTasks(workDate, type, status, assignedStaffId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long id) {
        TaskDto task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        TaskDto task = taskService.createTask(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDto> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        TaskDto updated = taskService.updateTaskStatus(id, request.status(), request.reason(), currentUserId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/validate-reservation")
    public ResponseEntity<com.storagehub.dto.CheckInValidationDto> validateCheckInReservation(
            @PathVariable Long id,
            @Valid @RequestBody com.storagehub.dto.ValidateCheckInRequest request
    ) {
        com.storagehub.dto.CheckInValidationDto result = taskService.validateCheckInReservation(id, request.reservationCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/activate-checkin")
    public ResponseEntity<com.storagehub.dto.CheckInActivationDto> activateCheckIn(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long staffUserId = getCurrentUserId(authentication);
        com.storagehub.dto.CheckInActivationDto result = taskService.activateCheckIn(id, staffUserId);
        return ResponseEntity.ok(result);
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("User is not authenticated");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Invalid authenticated user id: " + authentication.getName());
        }
    }
}
