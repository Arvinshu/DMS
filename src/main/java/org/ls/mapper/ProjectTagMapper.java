/**
 * 文件路径: src/main/java/org/ls/mapper/ProjectTagMapper.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目标签 Mapper 接口，定义数据库操作方法
 */
package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ls.entity.ProjectTag;

import java.util.List;

@Mapper // 标记为 MyBatis Mapper 接口
public interface ProjectTagMapper {

    /**
     * 插入新的项目标签
     *
     * @param projectTag 项目标签实体
     * @return 影响的行数
     */
    int insert(ProjectTag projectTag);

    /**
     * 根据 ID 更新项目标签信息
     *
     * @param projectTag 项目标签实体 (必须包含 tagId)
     * @return 影响的行数
     */
    int updateById(ProjectTag projectTag);

    /**
     * 根据 ID 删除项目标签
     *
     * @param tagId 标签 ID
     * @return 影响的行数
     */
    int deleteById(@Param("tagId") Long tagId);

    /**
     * 根据 ID 查询项目标签
     *
     * @param tagId 标签 ID
     * @return 项目标签实体，如果不存在则返回 null
     */
    ProjectTag findById(@Param("tagId") Long tagId);

    /**
     * 查询所有项目标签（支持分页和搜索）
     * 用于数据维护界面
     *
     * @param nameFilter 名称过滤条件 (模糊查询)
     * @param limit      每页数量
     * @param offset     偏移量
     * @return 项目标签列表
     */
    List<ProjectTag> findAll(@Param("nameFilter") String nameFilter,
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
     * 查询所有项目标签（不分页）
     * 用于项目创建/编辑时的下拉选择
     *
     * @return 所有项目标签列表
     */
    List<ProjectTag> findAllEnabledTags(); // 名称沿用设计文档，实际查询所有标签

    /**
     * 根据项目 ID 查询关联的标签列表
     *
     * @param projectId 项目 ID
     * @return 关联的项目标签列表
     */
    List<ProjectTag> findTagsByProjectId(@Param("projectId") Long projectId);

}