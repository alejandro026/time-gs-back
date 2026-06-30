package com.guerrero.timeproject.service.impl;

import com.guerrero.timeproject.dto.TaskDto;
import com.guerrero.timeproject.model.Project;
import com.guerrero.timeproject.model.Task;
import com.guerrero.timeproject.model.TaskStatus;
import com.guerrero.timeproject.model.TimeLog;
import com.guerrero.timeproject.repository.ProjectRepository;
import com.guerrero.timeproject.repository.TaskRepository;
import com.guerrero.timeproject.repository.TimeLogRepository;
import com.guerrero.timeproject.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TimeLogRepository timeLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        return convertToDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskDto createTask(TaskDto taskDto) {
        Project project = null;
        if (taskDto.getProjectId() != null) {
            project = projectRepository.findById(taskDto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskDto.getProjectId()));
        }

        Task task = Task.builder()
                .title(taskDto.getTitle())
                .description(taskDto.getDescription())
                .status(taskDto.getStatus() != null ? taskDto.getStatus() : TaskStatus.TODO)
                .totalTimeSpent(0L)
                .project(project)
                .build();

        Task saved = taskRepository.save(task);

        // If created directly in progress, start timer
        if (saved.getStatus() == TaskStatus.IN_PROGRESS) {
            startTimerForTask(saved);
        }

        return convertToDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTask(Long id, TaskDto taskDto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        
        // Manual time adjustment support
        if (taskDto.getTotalTimeSpent() != null) {
            task.setTotalTimeSpent(taskDto.getTotalTimeSpent());
        }

        // Project linkage
        if (taskDto.getProjectId() != null) {
            Project project = projectRepository.findById(taskDto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskDto.getProjectId()));
            task.setProject(project);
        } else {
            task.setProject(null);
        }

        // Status transition logic
        if (taskDto.getStatus() != null && taskDto.getStatus() != task.getStatus()) {
            handleStatusTransition(task, taskDto.getStatus());
        }

        Task saved = taskRepository.save(task);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTaskStatus(Long id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        if (task.getStatus() != status) {
            handleStatusTransition(task, status);
            task = taskRepository.save(task);
        }

        return convertToDto(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        
        // Stop active timer if any before deleting
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            timeLogRepository.findFirstByTaskIdAndEndTimeIsNull(task.getId()).ifPresent(timeLogRepository::delete);
        }
        
        // Delete all logs associated with this task
        List<TimeLog> logs = timeLogRepository.findByTaskId(task.getId());
        timeLogRepository.deleteAll(logs);
        
        taskRepository.delete(task);
    }

    private void handleStatusTransition(Task task, TaskStatus newStatus) {
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.IN_PROGRESS) {
            startTimerForTask(task);
        } else if (oldStatus == TaskStatus.IN_PROGRESS) {
            stopActiveTimerForTask(task);
        }
    }

    private void startTimerForTask(Task task) {
        // Stop any currently running timer globally
        timeLogRepository.findFirstByEndTimeIsNull().ifPresent(activeLog -> {
            LocalDateTime now = LocalDateTime.now();
            activeLog.setEndTime(now);
            long duration = Duration.between(activeLog.getStartTime(), now).toSeconds();
            activeLog.setDuration(duration);
            timeLogRepository.save(activeLog);

            Task activeTask = activeLog.getTask();
            activeTask.setTotalTimeSpent(activeTask.getTotalTimeSpent() + duration);
            activeTask.setStatus(TaskStatus.TODO); // move it back to TODO since we stopped its timer
            taskRepository.save(activeTask);
        });

        // Start timer for the target task
        TimeLog newLog = TimeLog.builder()
                .task(task)
                .startTime(LocalDateTime.now())
                .build();
        timeLogRepository.save(newLog);
    }

    private void stopActiveTimerForTask(Task task) {
        timeLogRepository.findFirstByTaskIdAndEndTimeIsNull(task.getId()).ifPresent(activeLog -> {
            LocalDateTime now = LocalDateTime.now();
            activeLog.setEndTime(now);
            long duration = Duration.between(activeLog.getStartTime(), now).toSeconds();
            activeLog.setDuration(duration);
            timeLogRepository.save(activeLog);

            task.setTotalTimeSpent(task.getTotalTimeSpent() + duration);
        });
    }

    private TaskDto convertToDto(Task task) {
        LocalDateTime activeTimerStartTime = null;
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            Optional<TimeLog> activeLog = timeLogRepository.findFirstByTaskIdAndEndTimeIsNull(task.getId());
            if (activeLog.isPresent()) {
                activeTimerStartTime = activeLog.get().getStartTime();
            }
        }

        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .totalTimeSpent(task.getTotalTimeSpent())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .createdAt(task.getCreatedAt())
                .activeTimerStartTime(activeTimerStartTime)
                .build();
    }
}
