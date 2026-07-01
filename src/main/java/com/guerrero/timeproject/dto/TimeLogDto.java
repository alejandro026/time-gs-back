package com.guerrero.timeproject.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLogDto {
    private Long id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration; // in seconds
}
