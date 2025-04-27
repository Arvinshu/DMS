/**
 * 文件路径: src/main/java/org/ls/mapper/ProjectStageMapper.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目阶段 Mapper 接口，定义数据库操作方法
 */
package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ls.entity.ProjectStage;

import java.util.List;

@Mapper // 标记为 MyBatis Mapper 接口
public interface ProjectStageMapper {

    /**
     * 插入新的项目阶段
     *
     * @param projectStage 项目阶段实体
     * @return 影响的行数
     */
    int insert(ProjectStage projectStage);

    /**
     * 根据 ID 更新项目阶段信息
     *
     * @param projectStage 项目阶段实体 (必须包含 stageId)
     * @return 影响的行数
     */
    int updateById(ProjectStage projectStage);

    /**
     * 根据 ID 删除项目阶段
     *
     * @param stageId 阶段 ID
     * @return 影响的行数
     */
    int deleteById(@Param("stageId") Integer stageId);

    /**
     * 根据 ID 查询项目阶段
     *
     * @param stageId 阶段 ID
     * @return 项目阶段实体，如果不存在则返回 null
     */
    ProjectStage findById(@Param("stageId") Integer stageId);

    /**
     * 查询所有项目阶段（支持分页和搜索）
     * 用于数据维护界面
     *
     * @param nameFilter 名称过滤条件 (模糊查询)
     * @param limit      每页数量
     * @param offset     偏移量
     * @return 项目阶段列表
     */
    List<ProjectStage> findAll(@Param("nameFilter") String nameFilter,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    /**
     * 计算符合条件的记录总数（用于分页）
     *
     * @param nameFilter 名称过滤条件 (模糊查询)
     * @return 记录总数
     */
    int countAll(@Param("nameFilter") String nameFilter);

    /**
     * 查询所有启用的项目阶段，按排序号升序排列
     * 用于项目和任务管理中的下拉选择
     *
     * @return 启用的项目阶段列表
     */
    List<ProjectStage> findAllEnabledOrdered();
}
