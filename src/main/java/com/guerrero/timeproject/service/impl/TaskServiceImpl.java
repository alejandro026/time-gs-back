package com.guerrero.timeproject.service.impl;

import com.guerrero.timeproject.dto.TaskDto;
import com.guerrero.timeproject.dto.TimeLogDto;
import com.guerrero.timeproject.model.Project;
import com.guerrero.timeproject.model.Task;
import com.guerrero.timeproject.model.TaskStatus;
import com.guerrero.timeproject.model.TimeLog;
import com.guerrero.timeproject.model.User;
import com.guerrero.timeproject.repository.ProjectRepository;
import com.guerrero.timeproject.repository.TaskRepository;
import com.guerrero.timeproject.repository.TimeLogRepository;
import com.guerrero.timeproject.repository.UserRepository;
import com.guerrero.timeproject.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks() {
        User user = getAuthenticatedUser();
        return taskRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this task");
        }

        return convertToDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByProjectId(Long projectId) {
        User user = getAuthenticatedUser();
        
        // Verify project ownership
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this project");
        }

        return taskRepository.findByUserAndProjectId(user, projectId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskDto createTask(TaskDto taskDto) {
        User user = getAuthenticatedUser();
        Project project = null;
        if (taskDto.getProjectId() != null) {
            project = projectRepository.findById(taskDto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskDto.getProjectId()));
            if (!project.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access to this project");
            }
        }

        Task task = Task.builder()
                .title(taskDto.getTitle())
                .description(taskDto.getDescription())
                .category(taskDto.getCategory())
                .status(taskDto.getStatus() != null ? taskDto.getStatus() : TaskStatus.TODO)
                .totalTimeSpent(taskDto.getTotalTimeSpent() != null ? taskDto.getTotalTimeSpent() : 0L)
                .project(project)
                .user(user) // Link user
                .build();

        Task saved = taskRepository.save(task);

        // If created directly in progress, start timer
        if (saved.getStatus() == TaskStatus.IN_PROGRESS) {
            startTimerForTask(saved, user);
        }

        return convertToDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTask(Long id, TaskDto taskDto) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this task");
        }

        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setCategory(taskDto.getCategory());
        
        // Manual time adjustment support
        if (taskDto.getTotalTimeSpent() != null) {
            task.setTotalTimeSpent(taskDto.getTotalTimeSpent());
        }

        // Project linkage verification
        if (taskDto.getProjectId() != null) {
            Project project = projectRepository.findById(taskDto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + taskDto.getProjectId()));
            if (!project.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized access to this project");
            }
            task.setProject(project);
        } else {
            task.setProject(null);
        }

        // Status transition logic
        if (taskDto.getStatus() != null && taskDto.getStatus() != task.getStatus()) {
            handleStatusTransition(task, taskDto.getStatus(), user);
        }

        Task saved = taskRepository.save(task);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public TaskDto updateTaskStatus(Long id, TaskStatus status) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this task");
        }

        if (task.getStatus() != status) {
            handleStatusTransition(task, status, user);
            task = taskRepository.save(task);
        }

        return convertToDto(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        
        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this task");
        }

        // Stop active timer if any before deleting
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            timeLogRepository.findFirstByTaskIdAndEndTimeIsNull(task.getId()).ifPresent(timeLogRepository::delete);
        }
        
        // Delete all logs associated with this task
        List<TimeLog> logs = timeLogRepository.findByTaskId(task.getId());
        timeLogRepository.deleteAll(logs);
        
        taskRepository.delete(task);
    }

    private void handleStatusTransition(Task task, TaskStatus newStatus, User user) {
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.IN_PROGRESS) {
            startTimerForTask(task, user);
        } else if (oldStatus == TaskStatus.IN_PROGRESS) {
            stopActiveTimerForTask(task);
        }
    }

    private void startTimerForTask(Task task, User user) {
        // Stop any currently running timer globally for this user
        timeLogRepository.findFirstByTaskUserAndEndTimeIsNull(user).ifPresent(activeLog -> {
            LocalDateTime now = LocalDateTime.now();
            activeLog.setEndTime(now);
            long duration = Duration.between(activeLog.getStartTime(), now).toSeconds();
            activeLog.setDuration(duration);
            timeLogRepository.save(activeLog);

            Task activeTask = activeLog.getTask();
            activeTask.setTotalTimeSpent(activeTask.getTotalTimeSpent() + duration);
            activeTask.setStatus(TaskStatus.TODO); // move it back to TODO
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
        String activeTimerStartTime = null;
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            Optional<TimeLog> activeLog = timeLogRepository.findFirstByTaskIdAndEndTimeIsNull(task.getId());
            if (activeLog.isPresent()) {
                LocalDateTime startLocal = activeLog.get().getStartTime();
                activeTimerStartTime = startLocal.atZone(java.time.ZoneId.systemDefault()).toInstant().toString();
            }
        }

        List<com.guerrero.timeproject.dto.TimeLogDto> logs = timeLogRepository.findByTaskId(task.getId()).stream()
                .map(log -> com.guerrero.timeproject.dto.TimeLogDto.builder()
                        .id(log.getId())
                        .startTime(log.getStartTime())
                        .endTime(log.getEndTime())
                        .duration(log.getDuration())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .category(task.getCategory())
                .status(task.getStatus())
                .totalTimeSpent(task.getTotalTimeSpent())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .createdAt(task.getCreatedAt())
                .activeTimerStartTime(activeTimerStartTime)
                .timeLogs(logs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeLogDto> getTaskLogs(Long taskId) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        return timeLogRepository.findByTaskId(taskId).stream()
                .map(log -> TimeLogDto.builder()
                        .id(log.getId())
                        .startTime(log.getStartTime())
                        .endTime(log.getEndTime())
                        .duration(log.getDuration())
                        .build())
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime())) // desc
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskDto addManualLog(Long taskId, TimeLogDto logDto) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (logDto.getStartTime() == null || logDto.getEndTime() == null) {
            throw new IllegalArgumentException("Start and End times are required");
        }

        long seconds = Duration.between(logDto.getStartTime(), logDto.getEndTime()).toSeconds();
        if (seconds < 0) {
            throw new IllegalArgumentException("Start time must be before End time");
        }

        TimeLog log = TimeLog.builder()
                .task(task)
                .startTime(logDto.getStartTime())
                .endTime(logDto.getEndTime())
                .duration(seconds)
                .build();
        timeLogRepository.save(log);

        // Recalculate totals
        recalculateTaskTotalTime(task);
        Task saved = taskRepository.save(task);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public TaskDto deleteLogSegment(Long taskId, Long logId) {
        User user = getAuthenticatedUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        TimeLog log = timeLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found"));
        if (!log.getTask().getId().equals(task.getId())) {
            throw new IllegalArgumentException("Log does not belong to this task");
        }

        // Delete the segment
        timeLogRepository.delete(log);

        // Recalculate totals
        recalculateTaskTotalTime(task);
        Task saved = taskRepository.save(task);
        return convertToDto(saved);
    }

    private void recalculateTaskTotalTime(Task task) {
        // Find all saved logs that are completed (endTime is not null)
        List<TimeLog> logs = timeLogRepository.findByTaskId(task.getId());
        long totalSeconds = logs.stream()
                .filter(log -> log.getEndTime() != null)
                .mapToLong(log -> log.getDuration() != null ? log.getDuration() : 0L)
                .sum();
        task.setTotalTimeSpent(totalSeconds);
    }
}
