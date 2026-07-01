package com.guerrero.timeproject.controller;

import com.guerrero.timeproject.dto.TaskDto;
import com.guerrero.timeproject.model.TaskStatus;
import com.guerrero.timeproject.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDto>> getTasksByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskDto taskDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDto> updateTaskStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // Time Log Auditing Endpoints
    @GetMapping("/{taskId}/logs")
    public ResponseEntity<List<com.guerrero.timeproject.dto.TimeLogDto>> getTaskLogs(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskLogs(taskId));
    }

    @PostMapping("/{taskId}/logs")
    public ResponseEntity<TaskDto> addManualLog(
            @PathVariable Long taskId,
            @Valid @RequestBody com.guerrero.timeproject.dto.TimeLogDto logDto) {
        return ResponseEntity.ok(taskService.addManualLog(taskId, logDto));
    }

    @DeleteMapping("/{taskId}/logs/{logId}")
    public ResponseEntity<TaskDto> deleteLogSegment(
            @PathVariable Long taskId,
            @PathVariable Long logId) {
        return ResponseEntity.ok(taskService.deleteLogSegment(taskId, logId));
    }
}
