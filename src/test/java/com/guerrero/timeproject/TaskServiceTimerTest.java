package com.guerrero.timeproject;

import com.guerrero.timeproject.dto.TaskDto;
import com.guerrero.timeproject.model.Role;
import com.guerrero.timeproject.model.TaskStatus;
import com.guerrero.timeproject.model.User;
import com.guerrero.timeproject.repository.UserRepository;
import com.guerrero.timeproject.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@WithMockUser(username = "testuser")
class TaskServiceTimerTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Save mock user in transactional DB context so getAuthenticatedUser() finds it
        if (!userRepository.existsByUsername("testuser")) {
            User user = User.builder()
                    .username("testuser")
                    .password("password")
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
        }
    }

    @Test
    void testTimerLifecycle() throws InterruptedException {
        // 1. Create a task in TODO status
        TaskDto taskDto = TaskDto.builder()
                .title("Test Timer Lifecycle Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .build();
        
        TaskDto created = taskService.createTask(taskDto);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(created.getTotalTimeSpent()).isEqualTo(0L);
        assertThat(created.getActiveTimerStartTime()).isNull();

        // 2. Transition it to IN_PROGRESS
        TaskDto started = taskService.updateTaskStatus(created.getId(), TaskStatus.IN_PROGRESS);
        assertThat(started.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(started.getActiveTimerStartTime()).isNotNull();

        // Wait a short moment (e.g. 1.2 seconds) to accumulate elapsed time
        Thread.sleep(1200);

        // 3. Transition it back to TODO (which stops the timer)
        TaskDto stopped = taskService.updateTaskStatus(created.getId(), TaskStatus.TODO);
        assertThat(stopped.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(stopped.getActiveTimerStartTime()).isNull();
        // Since we slept for ~1.2 seconds, totalTimeSpent should be greater than 0 (likely 1 or 2 seconds)
        assertThat(stopped.getTotalTimeSpent()).isGreaterThan(0L);
    }
}
