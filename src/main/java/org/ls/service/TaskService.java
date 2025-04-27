/**
 * 文件路径: src/main/java/org/ls/service/TaskService.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 任务服务接口，定义任务相关的业务逻辑方法
 */
package org.ls.service;

import org.ls.dto.TaskDto;

import java.util.List;
import java.util.Map;

public interface TaskService {

    /**
     * 创建新任务
     *
     * @param taskDto 任务 DTO
     * @return 创建后的任务 DTO
     */
    TaskDto createTask(TaskDto taskDto);

    /**
     * 根据 ID 更新任务信息
     *
     * @param taskId  要更新的任务 ID
     * @param taskDto 包含更新信息的 DTO
     * @return 更新后的任务 DTO
     * @throws RuntimeException 如果任务不存在
     */
    TaskDto updateTask(Long taskId, TaskDto taskDto);

    /**
     * 根据 ID 删除任务
     *
     * @param taskId 任务 ID
     */
    void deleteTask(Long taskId);

    /**
     * 根据 ID 获取任务详情
     *
     * @param taskId 任务 ID
     * @return 任务 DTO，如果不存在则返回 null
     */
    TaskDto getTaskById(Long taskId);

    /**
     * 根据项目 ID 获取其下所有任务列表
     *
     * @param projectId 项目 ID
     * @return 任务 DTO 列表
     */
    List<TaskDto> getTasksByProjectId(Long projectId);

    /**
     * 获取项目下最新更新的任务的关键信息 (状态和阶段ID)
     *
     * @param projectId 项目 ID
     * @return 包含 "task_status" 和 "stage_id" 的 Map，或 null
     */
    Map<String, Object> findLatestTaskInfoByProjectId(Long projectId);

}
