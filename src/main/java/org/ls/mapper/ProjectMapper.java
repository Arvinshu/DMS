/**
 * 文件路径: src/main/java/org/ls/mapper/ProjectMapper.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 项目 Mapper 接口，定义与项目相关的数据库操作方法，包括标签关联。
 */
package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ls.entity.Project;
import org.ls.entity.ProjectTag; // 需要引入 ProjectTag 用于关联查询

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMapper {

    /**
     * 插入新的项目
     *
     * @param project 项目实体
     * @return 影响的行数
     */
    int insert(Project project);

    /**
     * 根据 ID 更新项目信息
     *
     * @param project 项目实体 (必须包含 projectId)
     * @return 影响的行数
     */
    int updateById(Project project);

    /**
     * 根据 ID 删除项目
     * 注意：关联的任务和标签映射会通过数据库外键级联删除
     *
     * @param projectId 项目 ID
     * @return 影响的行数
     */
    int deleteById(@Param("projectId") Long projectId);

    /**
     * 根据 ID 查询项目基础信息 (不包含标签列表，标签通过 collection 映射加载)
     *
     * @param projectId 项目 ID
     * @return 项目实体，如果不存在则返回 null
     */
    Project findById(@Param("projectId") Long projectId);

    /**
     * 批量插入项目与标签的关联关系
     *
     * @param projectId 项目 ID
     * @param tagIds    标签 ID 列表
     * @return 影响的行数
     */
    int insertProjectTags(@Param("projectId") Long projectId, @Param("tagIds") List<Long> tagIds);

    /**
     * 根据项目 ID 删除所有标签关联关系
     *
     * @param projectId 项目 ID
     * @return 影响的行数
     */
    int deleteProjectTagsByProjectId(@Param("projectId") Long projectId);

//    /**
//     * 根据项目 ID 查询关联的标签列表 (供 resultMap collection 使用)
//     *
//     * @param projectId 项目 ID
//     * @return 关联的项目标签列表
//     */
//    List<ProjectTag> findTagsByProjectId(@Param("projectId") Long projectId);


    /**
     * 根据复杂条件查询项目列表（分页）
     *
     * @param params 包含过滤条件的 Map:
     * - nameFilter (String): 项目名称模糊查询
     * - businessTypeName (String): 业务类型完全匹配
     * - profitCenterZone (String): 利润中心完全匹配
     * - projectManager (String): 项目负责人完全匹配
     * - tsBm (String): 工时代码完全匹配
     * - tagIds (List<Long>): 标签 ID 列表 (AND 逻辑)
     * - tagCount (Integer): 标签数量 (用于 AND 逻辑的 HAVING COUNT)
     * - limit (int): 每页数量
     * - offset (int): 偏移量
     * - sortBy (String): 排序字段 (e.g., "created_at")
     * - sortOrder (String): 排序顺序 ("ASC" or "DESC")
     * @return 符合条件的项目列表 (包含关联的标签)
     */
    List<Project> findProjectsByCriteria(@Param("params") Map<String, Object> params);

    /**
     * 计算符合复杂条件的项目总数（用于分页）
     *
     * @param params 包含过滤条件的 Map (同 findProjectsByCriteria)
     * @return 符合条件的项目总数
     */
    int countProjectsByCriteria(@Param("params") Map<String, Object> params);

    /**
     * 根据项目ID计算其下的任务数量
     * 用于删除项目前的检查
     *
     * @param projectId 项目 ID
     * @return 任务数量
     */
    int countTasksByProjectId(@Param("projectId") Long projectId);

    // --- 新增的统计相关方法 ---

    /**
     * 根据状态列表和创建日期范围统计项目数量。
     * 注意: 'project_status' 字段的来源和定义需明确。
     * 如果 t_project 表中没有直接的 project_status 列，
     * Service层需要处理状态的判断逻辑，此Mapper方法可能需要调整或不直接使用。
     * 此处假设调用者能提供有效的 statusList 用于直接在SQL中筛选 (如果表结构支持)。
     *
     * @param params 查询参数Map，应包含:
     * - statusList (List<String>): 项目状态列表
     * - startDate (LocalDate): 创建日期开始
     * - endDate (LocalDate): 创建日期结束
     * @return 符合条件的项目数量
     */
    Integer countProjectsByStatusAndDateRange(Map<String, Object> params);

    /**
     * 按指定维度（如业务类型、利润中心）对项目进行分组统计数量。
     *
     * @param params 查询参数Map，应包含:
     * - groupByField (String): 用于分组的数据库列名 (例如: "business_type_name", "profit_center_zone")。
     * Service层必须校验此参数的合法性以防SQL注入，或在XML中使用choose。
     * - startDate (LocalDate): 创建日期开始 (可选)
     * - endDate (LocalDate): 创建日期结束 (可选)
     * @return List of Maps, 每个Map包含 "label" (分组字段的值) 和 "value" (该分组下的项目数量)
     */
    List<Map<String, Object>> countProjectsGroupedByField(Map<String, Object> params);


    /**
     * 查询所有状态为非“已完成”或“已取消”的项目基础信息。
     * "活跃"的具体定义由Service层进一步根据业务需求筛选（例如结合任务状态）。
     * 此方法主要用于获取项目列表，后续由Service层进行风险评估等详细处理。
     *
     * @param params 查询参数Map (可选，例如用于分页或特定基础字段筛选)
     * - activeStatusCodes (List<String>): 用于定义哪些项目状态码被认为是活跃的。
     * 如果项目状态不是直接存储在t_project，此参数可能无用，
     * Service层需要先获取所有项目，再判断活跃性。
     * 这里假设t_project有一个可用于判断活跃性的状态字段。
     * @return 活跃项目的基础信息列表
     */
    List<Project> selectActiveProjectsForRiskAssessment(Map<String, Object> params);

    // --- 新增方法: 用于统计服务中获取项目列表 ---
    /**
     * 根据指定条件查询项目列表，主要用于统计分析模块。
     * 例如，获取在特定日期范围内创建的项目。
     *
     * @param params 查询参数Map，可包含:
     * - createdDateStart (LocalDate): 创建日期开始
     * - createdDateEnd (LocalDate): 创建日期结束
     * - (其他未来可能需要的筛选条件)
     * @return 符合条件的项目实体列表 (List<Project>)
     */
    List<Project> findProjectsForStatistics(Map<String, Object> params);
}