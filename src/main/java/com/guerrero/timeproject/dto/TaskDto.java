package com.guerrero.timeproject.dto;

import com.guerrero.timeproject.model.TaskStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

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
    private String activeTimerStartTime; // populated if currently IN_PROGRESS as ISO-8601 UTC string
    private String category;
    private List<TimeLogDto> timeLogs; // associated time log segments for auditing
}
