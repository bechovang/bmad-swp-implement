package com.storagehub.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storagehub.config.SecurityConfig;
import com.storagehub.config.WebMvcConfig;
import com.storagehub.controller.ErrorEnvelopeWriter;
import com.storagehub.controller.GlobalExceptionHandler;
import com.storagehub.controller.TaskController;
import com.storagehub.dto.CreateTaskRequest;
import com.storagehub.dto.TaskDto;
import com.storagehub.dto.UpdateTaskStatusRequest;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.TaskStatus;
import com.storagehub.entity.TaskType;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.exception.ResourceNotFoundException;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.JwtService;
import com.storagehub.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class, properties =
        "app.jwt.secret=task-test-secret-0123456789-abcdefghijklmnop")
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class, GlobalExceptionHandler.class, WebMvcConfig.class })
public class TaskControllerTests {

    private static final Long STAFF_ID = 2L;
    private static final Long CUSTOMER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private UserRepository userRepository;

    private String staffToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role staffRole = new Role(2, "Staff", null);
        User staff = new User("Minh Tran", "minh@storagehub.dev", "0902345678",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", staffRole, UserStatus.ACTIVE);
        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));

        Role customerRole = new Role(1, "Customer", null);
        User customer = new User("Lan Nguyen", "lan@storagehub.dev", "0901234567",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu", customerRole, UserStatus.ACTIVE);
        when(userRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        staffToken = jwtService.issueToken(STAFF_ID, RoleName.STAFF);
        customerToken = jwtService.issueToken(CUSTOMER_ID, RoleName.CUSTOMER);
    }

    @Test
    @DisplayName("GET /api/v1/tasks returns 200 with task list for staff")
    void testGetTasks_staffAllowed() throws Exception {
        TaskDto task1 = new TaskDto(1L, TaskType.CHECK_IN, "BK-1042", 2L, "Minh Tran",
                LocalDate.of(2026, 10, 3), TaskStatus.TODO, "S-3", "Lan Nguyen",
                LocalDate.of(2026, 10, 3), "Morning", "Check-in BK-1042", "Lan Nguyen checking in");

        when(taskService.getTasks(any(), any(), any(), any())).thenReturn(List.of(task1));

        mockMvc.perform(get("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("CHECK_IN"))
                .andExpect(jsonPath("$[0].refCode").value("BK-1042"))
                .andExpect(jsonPath("$[0].unitCode").value("S-3"))
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks returns 403 FORBIDDEN for customer token")
    void testGetTasks_customerForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 200 with task details")
    void testGetTaskById_success() throws Exception {
        TaskDto task = new TaskDto(1L, TaskType.CHECK_IN, "BK-1042", 2L, "Minh Tran",
                LocalDate.of(2026, 10, 3), TaskStatus.TODO, "S-3", "Lan Nguyen",
                LocalDate.of(2026, 10, 3), "Morning", "Check-in BK-1042", "Lan Nguyen checking in");

        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get("/api/v1/tasks/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.refCode").value("BK-1042"))
                .andExpect(jsonPath("$.unitCode").value("S-3"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 404 when task not found")
    void testGetTaskById_notFound() throws Exception {
        when(taskService.getTaskById(999L)).thenThrow(new ResourceNotFoundException("Task not found with id: 999"));

        mockMvc.perform(get("/api/v1/tasks/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status transitions status successfully")
    void testUpdateTaskStatus_success() throws Exception {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS, "Starting inspection");
        TaskDto updated = new TaskDto(1L, TaskType.CHECK_IN, "BK-1042", 2L, "Minh Tran",
                LocalDate.of(2026, 10, 3), TaskStatus.IN_PROGRESS, "S-3", "Lan Nguyen",
                LocalDate.of(2026, 10, 3), "Morning", "Check-in BK-1042", "Lan Nguyen checking in");

        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.IN_PROGRESS), eq("Starting inspection"), eq(STAFF_ID)))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/v1/tasks/1/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /api/v1/tasks creates a task and returns 201 CREATED")
    void testCreateTask_success() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                TaskType.CLEANING, "S-3", 2L, LocalDate.of(2026, 10, 5),
                TaskStatus.TODO, "S-3", null, LocalDate.of(2026, 10, 5),
                "Morning", "Cleaning S-3", "Turnover cleaning"
        );
        TaskDto created = new TaskDto(10L, TaskType.CLEANING, "S-3", 2L, "Minh Tran",
                LocalDate.of(2026, 10, 5), TaskStatus.TODO, "S-3", null,
                LocalDate.of(2026, 10, 5), "Morning", "Cleaning S-3", "Turnover cleaning");

        when(taskService.createTask(any(CreateTaskRequest.class), eq(STAFF_ID))).thenReturn(created);

        mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("CLEANING"))
                .andExpect(jsonPath("$.unitCode").value("S-3"));
    }
}
