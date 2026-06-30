package com.guerrero.timeproject.service;

import com.guerrero.timeproject.dto.TaskDto;
import com.guerrero.timeproject.model.TaskStatus;
import java.util.List;

public interface TaskService {
    List<TaskDto> getAllTasks();
    TaskDto getTaskById(Long id);
    List<TaskDto> getTasksByProjectId(Long projectId);
    TaskDto createTask(TaskDto taskDto);
    TaskDto updateTask(Long id, TaskDto taskDto);
    TaskDto updateTaskStatus(Long id, TaskStatus status);
    void deleteTask(Long id);
}
