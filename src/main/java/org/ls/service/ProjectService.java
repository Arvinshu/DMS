/**
 * 文件路径: src/main/java/org/ls/service/ProjectService.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 项目服务接口，定义项目相关的业务逻辑方法，包括搜索和状态计算
 */
package org.ls.service;

import org.ls.dto.ProjectCreateDto;
import org.ls.dto.ProjectDetailDto;
import org.ls.dto.ProjectListDto;
// 引入分页结果类 (假设存在或需要创建)
// import org.ls.dto.PageResult;

import java.util.List;
import java.util.Map;

public interface ProjectService {

    /**
     * 创建新项目，并处理标签关联
     *
     * @param projectCreateDto 项目创建 DTO (包含 tagIds)
     * @param creatorEmployee  创建者员工信息 (例如 "工号-姓名")
     * @return 创建后的项目列表 DTO (包含计算字段和标签)
     */
    ProjectListDto createProject(ProjectCreateDto projectCreateDto, String creatorEmployee);

    /**
     * 根据 ID 更新项目信息，并处理标签关联
     *
     * @param projectId        要更新的项目 ID
     * @param projectCreateDto 包含更新信息的 DTO (包含 tagIds)
     * @param updaterEmployee  更新者员工信息
     * @return 更新后的项目列表 DTO
     * @throws RuntimeException 如果项目不存在
     */
    ProjectListDto updateProject(Long projectId, ProjectCreateDto projectCreateDto, String updaterEmployee);

    /**
     * 根据 ID 删除项目
     * 前提：项目下没有任何任务
     *
     * @param projectId 项目 ID
     * @throws RuntimeException 如果项目不存在或项目下存在任务
     */
    void deleteProject(Long projectId);

    /**
     * 根据 ID 获取项目详情 (包含任务列表)
     *
     * @param projectId 项目 ID
     * @return 项目详情 DTO，如果不存在则返回 null
     */
    ProjectDetailDto getProjectDetailById(Long projectId);

    /**
     * 根据复杂条件搜索项目列表（分页）
     *
     * @param searchParams 包含过滤条件的 Map (同 ProjectMapper.findProjectsByCriteria 的 params)
     * @param pageNum      页码 (从 1 开始)
     * @param pageSize     每页数量
     * @return 分页结果对象，包含项目列表 DTO 和总记录数 (TODO: 使用 PageResult 封装)
     * 暂时返回 Map<String, Object> 包含 "data" 和 "total"
     */
    Map<String, Object> searchProjects(Map<String, Object> searchParams, int pageNum, int pageSize);

    /**
     * 获取用于项目创建/搜索的下拉选项数据
     *
     * @return 包含各类下拉列表数据的 Map，例如:
     * { "businessTypes": List<BusinessTypeDto>, "profitCenters": List<ProfitCenterDto>, ... }
     */
    Map<String, Object> getProjectLookups();

}
