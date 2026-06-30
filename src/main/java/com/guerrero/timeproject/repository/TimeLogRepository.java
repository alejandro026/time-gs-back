package com.guerrero.timeproject.repository;

import com.guerrero.timeproject.model.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
    Optional<TimeLog> findFirstByTaskIdAndEndTimeIsNull(Long taskId);
    Optional<TimeLog> findFirstByEndTimeIsNull();
    List<TimeLog> findByTaskId(Long taskId);
}
