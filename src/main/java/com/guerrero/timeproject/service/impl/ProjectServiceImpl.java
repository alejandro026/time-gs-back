package com.guerrero.timeproject.service.impl;

import com.guerrero.timeproject.dto.ProjectDto;
import com.guerrero.timeproject.model.Project;
import com.guerrero.timeproject.repository.ProjectRepository;
import com.guerrero.timeproject.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return convertToDto(project);
    }

    @Override
    @Transactional
    public ProjectDto createProject(ProjectDto projectDto) {
        Project project = Project.builder()
                .name(projectDto.getName())
                .description(projectDto.getDescription())
                .clientName(projectDto.getClientName())
                .status(projectDto.getStatus())
                .build();
        Project saved = projectRepository.save(project);
        return convertToDto(saved);
    }

    @Override
    @Transactional
    public ProjectDto updateProject(Long id, ProjectDto projectDto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        
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
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
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
