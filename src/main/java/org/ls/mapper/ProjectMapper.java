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

}