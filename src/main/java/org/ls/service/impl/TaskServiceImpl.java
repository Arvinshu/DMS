/**
 * 文件路径: src/main/java/org/ls/service/impl/TaskServiceImpl.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 任务服务实现类
 */
package org.ls.service.impl;

import org.ls.dto.TaskDto;
import org.ls.entity.Task;
import org.ls.mapper.TaskMapper;
import org.ls.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskMapper taskMapper;

    @Override
    @Transactional
    public TaskDto createTask(TaskDto taskDto) {
        log.info("Creating new task for project ID: {}", taskDto.getProjectId());
        Task entity = taskDto.toEntity();
        entity.setTaskId(null); // 确保是插入
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        int affectedRows = taskMapper.insert(entity);
        if (affectedRows == 0) {
            log.error("Failed to insert task: {}", taskDto.getTaskName());
            throw new RuntimeException("创建任务失败");
        }
        log.info("Task created successfully with ID: {}", entity.getTaskId());
        return TaskDto.fromEntity(entity);
        // TODO: 考虑创建任务后是否需要更新项目的状态？（可以在 Controller 或 ProjectService 中处理）
    }

    @Override
    @Transactional
    public TaskDto updateTask(Long taskId, TaskDto taskDto) {
        log.info("Updating task with ID: {}", taskId);
        Task existingTask = taskMapper.findById(taskId);
        if (existingTask == null) {
            log.warn("Task not found for ID: {}", taskId);
            throw new RuntimeException("任务不存在，无法更新");
        }

        Task entityToUpdate = taskDto.toEntity();
        entityToUpdate.setTaskId(taskId); // 确保 ID 正确
        entityToUpdate.setUpdatedAt(OffsetDateTime.now()); // 设置更新时间

        int affectedRows = taskMapper.updateById(entityToUpdate);
        if (affectedRows == 0) {
            log.error("Failed to update task with ID: {}", taskId);
            throw new RuntimeException("更新任务失败");
        }
        log.info("Task updated successfully for ID: {}", taskId);
        // TODO: 考虑更新任务后是否需要更新项目的状态？
        return TaskDto.fromEntity(taskMapper.findById(taskId));
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        log.warn("Attempting to delete task with ID: {}", taskId);
        Task existingTask = taskMapper.findById(taskId);
        if (existingTask == null) {
            log.warn("Task not found for ID: {}, skipping deletion.", taskId);
            return;
        }

        int affectedRows = taskMapper.deleteById(taskId);
        if (affectedRows == 0) {
            log.error("Failed to delete task with ID: {}", taskId);
            throw new RuntimeException("删除任务失败");
        }
        log.info("Task deleted successfully for ID: {}", taskId);
        // TODO: 考虑删除任务后是否需要更新项目的状态？
    }

    @Override
    public TaskDto getTaskById(Long taskId) {
        log.debug("Fetching task by ID: {}", taskId);
        Task entity = taskMapper.findById(taskId);
        return TaskDto.fromEntity(entity);
    }

    @Override
    public List<TaskDto> getTasksByProjectId(Long projectId) {
        log.debug("Fetching tasks for project ID: {}", projectId);
        List<Task> entities = taskMapper.findTasksByProjectId(projectId);
        return entities.stream()
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findLatestTaskInfoByProjectId(Long projectId) {
        log.debug("Finding latest task info for project ID: {}", projectId);
        return taskMapper.findLatestTaskInfoByProjectId(projectId);
    }
}
