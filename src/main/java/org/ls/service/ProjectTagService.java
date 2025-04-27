/**
 * 文件路径: src/main/java/org/ls/service/ProjectTagService.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目标签服务接口，定义业务逻辑方法
 */
package org.ls.service;

import org.ls.dto.ProjectTagDto; // 使用 DTO 进行交互

import java.util.List;

public interface ProjectTagService {

    /**
     * 创建新的项目标签
     *
     * @param projectTagDto 项目标签 DTO
     * @return 创建后的项目标签 DTO (包含生成的 ID 和时间戳)
     */
    ProjectTagDto createTag(ProjectTagDto projectTagDto);

    /**
     * 根据 ID 更新项目标签信息
     *
     * @param tagId         要更新的标签 ID
     * @param projectTagDto 包含更新信息的 DTO
     * @return 更新后的项目标签 DTO
     * @throws RuntimeException 如果标签不存在或更新失败
     */
    ProjectTagDto updateTag(Long tagId, ProjectTagDto projectTagDto);

    /**
     * 根据 ID 删除项目标签
     *
     * @param tagId 标签 ID
     * @throws RuntimeException 如果删除失败 (例如，有关联的项目且外键设置为 CASCADE，需要考虑是否允许)
     */
    void deleteTag(Long tagId);

    /**
     * 根据 ID 获取项目标签详情
     *
     * @param tagId 标签 ID
     * @return 项目标签 DTO，如果不存在则返回 null
     */
    ProjectTagDto getTagById(Long tagId);

    /**
     * 获取项目标签列表（分页）
     *
     * @param nameFilter 名称过滤条件
     * @param pageNum    页码 (从 1 开始)
     * @param pageSize   每页数量
     * @return 项目标签 DTO 列表 (TODO: 后续可以封装成分页结果对象 PageResult<ProjectTagDto>)
     */
    List<ProjectTagDto> getTags(String nameFilter, int pageNum, int pageSize);

    /**
     * 获取符合条件的记录总数
     *
     * @param nameFilter 名称过滤条件
     * @return 总记录数
     */
    int countTags(String nameFilter);

    /**
     * 获取所有项目标签（不分页）
     *
     * @return 所有项目标签 DTO 列表
     */
    List<ProjectTagDto> getAllTags();
}
