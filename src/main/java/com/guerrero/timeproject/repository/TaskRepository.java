package com.guerrero.timeproject.repository;

import com.guerrero.timeproject.model.Task;
import com.guerrero.timeproject.model.TaskStatus;
import com.guerrero.timeproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
    List<Task> findByUserAndProjectId(User user, Long projectId);
    List<Task> findByUserAndStatus(User user, TaskStatus status);
}
