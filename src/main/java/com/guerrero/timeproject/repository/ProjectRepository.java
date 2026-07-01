package com.guerrero.timeproject.repository;

import com.guerrero.timeproject.model.Project;
import com.guerrero.timeproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUser(User user);
}
