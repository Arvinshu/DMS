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


    // --- 新增的统计相关方法 ---

    /**
     * 查询 t_task 表中去重的 assignee_employee 字段。
     *
     * @return 去重后的任务负责人列表
     */
    List<String> selectDistinctAssignees();

    /**
     * 根据传入的参数统计任务数量。
     * 参数通过 Map 传递，键名与SQL中的变量对应。
     * 例如：params.put("assigneeEmployee", "E001-张三");
     * params.put("statusList", List.of("待办", "进行中"));
     *
     * @param params 查询参数Map
     * @return 符合条件的任务数量
     */
    Integer countTasksByCriteria(Map<String, Object> params);

    /**
     * 根据传入参数查询任务列表，用于统计分析。
     * 例如：风险评估、到期/逾期列表、员工任务详情。
     *
     * @param params 查询参数Map
     * @return 符合条件的任务列表 (Task实体列表)
     */
    List<Task> selectTasksForStatistics(Map<String, Object> params);

    /**
     * 用于员工负载图，统计每个员工名下不同状态的任务数。
     *
     * @param params 查询参数Map，可包含 statusList (例如: ['待办', '进行中']) 和 dateRange (如果需要按日期筛选任务)
     * @return List of Maps, 每个Map包含 "assignee_employee", "task_status", "task_count"
     */
    List<Map<String, Object>> countTasksByAssigneeAndStatusGrouped(Map<String, Object> params);


}