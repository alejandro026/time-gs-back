package com.guerrero.timeproject.dto;

import com.guerrero.timeproject.model.ProjectStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    private Long id;
    private String name;
    private String description;
    private String clientName;
    private ProjectStatus status;
    private LocalDateTime createdAt;
}
