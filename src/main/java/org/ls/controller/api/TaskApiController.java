/**
 * 文件路径: src/main/java/org/ls/controller/api/TaskApiController.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 提供任务相关的 RESTful API 接口
 */
package org.ls.controller.api;

import jakarta.validation.Valid;
import org.ls.dto.TaskDto;
import org.ls.service.TaskService;
import org.ls.utils.StringUtils; // For input sanitization
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks") // Base path for task-related APIs
public class TaskApiController {

    private static final Logger log = LoggerFactory.getLogger(TaskApiController.class);

    @Autowired
    private TaskService taskService;

    /**
     * 创建新任务
     *
     * @param taskDto 包含新任务信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含创建后的任务 DTO 和 201 Created 状态
     */
    @PostMapping
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskDto taskDto) {
        log.info("API request received: Create task for project ID {}", taskDto.getProjectId());
        // Sanitize string inputs in DTO
        taskDto.setTaskName(StringUtils.simpleSanitize(taskDto.getTaskName()));
        taskDto.setTaskDescription(StringUtils.simpleSanitize(taskDto.getTaskDescription()));
        taskDto.setPriority(StringUtils.simpleSanitize(taskDto.getPriority()));
        taskDto.setAssigneeEmployee(StringUtils.simpleSanitize(taskDto.getAssigneeEmployee()));
        taskDto.setTaskStatus(StringUtils.simpleSanitize(taskDto.getTaskStatus()));
        taskDto.setAttachments(StringUtils.simpleSanitize(taskDto.getAttachments())); // Basic sanitize for attachments info

        TaskDto createdTask = taskService.createTask(taskDto);
        // Return 201 Created status and the newly created resource
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    /**
     * 根据 ID 获取任务详情
     *
     * @param taskId 任务 ID
     * @return ResponseEntity 包含任务 DTO 或 404 Not Found
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long taskId) {
        log.info("API request received: Get task by ID - {}", taskId);
        TaskDto taskDto = taskService.getTaskById(taskId);
        if (taskDto != null) {
            return ResponseEntity.ok(taskDto);
        } else {
            log.warn("Task not found for ID: {}", taskId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 更新现有任务
     *
     * @param taskId  要更新的任务 ID
     * @param taskDto 包含更新信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含更新后的任务 DTO 或 404 Not Found
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDto> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskDto taskDto) {
        log.info("API request received: Update task - ID: {}", taskId);
        // Sanitize string inputs in DTO
        taskDto.setTaskName(StringUtils.simpleSanitize(taskDto.getTaskName()));
        taskDto.setTaskDescription(StringUtils.simpleSanitize(taskDto.getTaskDescription()));
        taskDto.setPriority(StringUtils.simpleSanitize(taskDto.getPriority()));
        taskDto.setAssigneeEmployee(StringUtils.simpleSanitize(taskDto.getAssigneeEmployee()));
        taskDto.setTaskStatus(StringUtils.simpleSanitize(taskDto.getTaskStatus()));
        taskDto.setAttachments(StringUtils.simpleSanitize(taskDto.getAttachments()));

        try {
            TaskDto updatedTask = taskService.updateTask(taskId, taskDto);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            log.warn("Failed to update task, ID {} not found or other error: {}", taskId, e.getMessage());
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.notFound().build();
            }
            throw e; // Let global handler deal with other errors
        }
    }

    /**
     * 删除任务
     *
     * @param taskId 要删除的任务 ID
     * @return ResponseEntity 无内容 (204 No Content) 或 404 Not Found
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        log.warn("API request received: Delete task - ID: {}", taskId);
        try {
            // Check if task exists before deleting (optional, service might handle it)
            // TaskDto existingTask = taskService.getTaskById(taskId);
            // if (existingTask == null) {
            //     return ResponseEntity.notFound().build();
            // }
            taskService.deleteTask(taskId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // Handle potential exceptions from service layer if needed
            log.error("Error deleting task with ID: {}", taskId, e);
            if (e.getMessage().contains("不存在")) { // If service throws specific not found exception
                return ResponseEntity.notFound().build();
            }
            throw e; // Let global handler deal with other errors
        }
    }

    /**
     * 根据项目 ID 获取任务列表
     * Note: This endpoint might be better placed under ProjectApiController
     * like /api/projects/{projectId}/tasks, but keeping it separate for now.
     *
     * @param projectId 项目 ID
     * @return ResponseEntity 包含任务 DTO 列表
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDto>> getTasksByProjectId(@PathVariable Long projectId) {
        log.info("API request received: Get tasks for project ID {}", projectId);
        List<TaskDto> tasks = taskService.getTasksByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }
}
