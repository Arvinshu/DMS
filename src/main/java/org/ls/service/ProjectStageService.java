/**
 * 文件路径: src/main/java/org/ls/service/ProjectStageService.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目阶段服务接口，定义业务逻辑方法
 */
package org.ls.service;

import org.ls.dto.ProjectStageDto; // 使用 DTO 进行交互

import java.util.List;

public interface ProjectStageService {

    /**
     * 创建新的项目阶段
     *
     * @param projectStageDto 项目阶段 DTO
     * @return 创建后的项目阶段 DTO (包含生成的 ID 和时间戳)
     */
    ProjectStageDto createStage(ProjectStageDto projectStageDto);

    /**
     * 根据 ID 更新项目阶段信息
     *
     * @param stageId         要更新的阶段 ID
     * @param projectStageDto 包含更新信息的 DTO
     * @return 更新后的项目阶段 DTO
     * @throws RuntimeException 如果阶段不存在或更新失败
     */
    ProjectStageDto updateStage(Integer stageId, ProjectStageDto projectStageDto);

    /**
     * 根据 ID 删除项目阶段
     *
     * @param stageId 阶段 ID
     * @throws RuntimeException 如果删除失败 (例如，有关联的任务且外键设置为 RESTRICT)
     */
    void deleteStage(Integer stageId);

    /**
     * 根据 ID 获取项目阶段详情
     *
     * @param stageId 阶段 ID
     * @return 项目阶段 DTO，如果不存在则返回 null
     */
    ProjectStageDto getStageById(Integer stageId);

    /**
     * 获取项目阶段列表（分页）
     *
     * @param nameFilter 名称过滤条件
     * @param pageNum    页码 (从 1 开始)
     * @param pageSize   每页数量
     * @return 项目阶段 DTO 列表 (TODO: 后续可以封装成分页结果对象 PageResult<ProjectStageDto>)
     */
    List<ProjectStageDto> getStages(String nameFilter, int pageNum, int pageSize);

    /**
     * 获取符合条件的记录总数
     *
     * @param nameFilter 名称过滤条件
     * @return 总记录数
     */
    int countStages(String nameFilter);

    /**
     * 获取所有启用的项目阶段，按顺序排列
     *
     * @return 启用的项目阶段 DTO 列表
     */
    List<ProjectStageDto> getAllEnabledStagesOrdered();
}
