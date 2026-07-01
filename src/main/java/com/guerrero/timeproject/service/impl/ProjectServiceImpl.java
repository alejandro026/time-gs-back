package com.guerrero.timeproject.service.impl;

import com.guerrero.timeproject.dto.ProjectDto;
import com.guerrero.timeproject.model.Project;
import com.guerrero.timeproject.model.User;
import com.guerrero.timeproject.repository.ProjectRepository;
import com.guerrero.timeproject.repository.UserRepository;
import com.guerrero.timeproject.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
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
    public List<ProjectDto> getAllProjects() {
        User user = getAuthenticatedUser();
        return projectRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        User user = getAuthenticatedUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this project");
        }
        
        return convertToDto(project);
    }

    @Override
    @Transactional
    public ProjectDto createProject(ProjectDto projectDto) {
        User user = getAuthenticatedUser();
        Project project = Project.builder()
                .name(projectDto.getName())
                .description(projectDto.getDescription())
                .clientName(projectDto.getClientName())
                .status(projectDto.getStatus())
                .user(user) // Link user
                .build();
        Project saved = projectRepository.save(project);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public ProjectDto updateProject(Long id, ProjectDto projectDto) {
        User user = getAuthenticatedUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this project");
        }
        
        project.setName(projectDto.getName());
        project.setDescription(projectDto.getDescription());
        project.setClientName(projectDto.getClientName());
        if (projectDto.getStatus() != null) {
            project.setStatus(projectDto.getStatus());
        }
        
        Project saved = projectRepository.save(project);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        User user = getAuthenticatedUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to this project");
        }
        
        projectRepository.delete(project);
    }

    private ProjectDto convertToDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .clientName(project.getClientName())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
