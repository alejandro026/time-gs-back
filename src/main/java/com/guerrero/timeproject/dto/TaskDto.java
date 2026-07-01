package com.guerrero.timeproject.dto;

import com.guerrero.timeproject.model.TaskStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDto {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Long totalTimeSpent;
    private Long projectId;
    private String projectName;
    private LocalDateTime createdAt;
    private LocalDateTime activeTimerStartTime; // populated if currently IN_PROGRESS
    private String category;
}
