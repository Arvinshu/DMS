/**
 * 文件路径: src/main/java/org/ls/mapper/TaskMapper.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 任务 Mapper 接口，定义与任务相关的数据库操作方法。
 */
package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ls.entity.Task;

import java.util.List;
import java.util.Map; // 用于接收最新任务信息

@Mapper
public interface TaskMapper {

    /**
     * 插入新的任务
     *
     * @param task 任务实体
     * @return 影响的行数
     */
    int insert(Task task);

    /**
     * 根据 ID 更新任务信息
     *
     * @param task 任务实体 (必须包含 taskId)
     * @return 影响的行数
     */
    int updateById(Task task);

    /**
     * 根据 ID 删除任务
     *
     * @param taskId 任务 ID
     * @return 影响的行数
     */
    int deleteById(@Param("taskId") Long taskId);

    /**
     * 根据 ID 查询任务
     *
     * @param taskId 任务 ID
     * @return 任务实体，如果不存在则返回 null
     */
    Task findById(@Param("taskId") Long taskId);

    /**
     * 根据项目 ID 查询其下所有任务
     * 按阶段排序号升序，同一阶段内按创建时间升序
     *
     * @param projectId 项目 ID
     * @return 该项目下的任务列表
     */
    List<Task> findTasksByProjectId(@Param("projectId") Long projectId);

    /**
     * 查询项目下最新更新的任务的关键信息（状态和阶段ID）
     * 用于计算项目状态和当前阶段
     *
     * @param projectId 项目 ID
     * @return 包含 task_status 和 stage_id 的 Map，如果项目无任务则返回 null
     */
    Map<String, Object> findLatestTaskInfoByProjectId(@Param("projectId") Long projectId);

    /**
     * *** 新增: 根据项目ID计算任务数量 ***
     * 用于删除项目前的检查
     * todo 需要将 countTasksByProjectId 方法也添加到 TaskService 和 TaskServiceImpl 中
     * @param projectId 项目 ID
     * @return 任务数量
     */
    int countTasksByProjectId(@Param("projectId") Long projectId); // 添加方法声明


}