/**
 * 文件路径: src/main/java/org/ls/dto/ProjectDetailDto.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 项目详情查询结果的数据传输对象，可能包含完整的任务列表
 */
package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ls.entity.Project;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailDto {

    // --- 包含 ProjectListDto 中的所有字段 ---
    private Long projectId;
    private String projectName;
    private String projectDescription; // 详情页需要描述
    private String businessTypeName;
    private String profitCenterZone;
    private String projectManagerEmployee;
    private String tsBm;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy; // 详情页可能需要创建人
    private String updatedBy; // 详情页可能需要更新人
    private List<ProjectTagDto> tags;
    private String currentStageName;
    private String projectStatus;
    private Object progress;

    // --- 详情页特有字段 ---

    /**
     * 项目下的完整任务列表 (按阶段分组可能在前端处理，这里返回扁平列表)
     */
    private List<TaskDto> tasks;

    // 可以添加其他详情信息，例如关联文档、评论等


    /**
     * 从实体对象和其他计算/关联数据构建 DTO
     *
     * @param entity           Project 实体
     * @param tags             关联的标签 DTO 列表
     * @param tasks            关联的任务 DTO 列表
     * @param currentStageName 计算出的当前阶段名称
     * @param projectStatus    计算出的项目状态
     * @param progress         计算出的进度信息 (可选)
     * @return ProjectDetailDto 对象
     */
    public static ProjectDetailDto fromEntity(Project entity,
                                              List<ProjectTagDto> tags,
                                              List<TaskDto> tasks,
                                              String currentStageName,
                                              String projectStatus,
                                              Object progress) {
        if (entity == null) {
            return null;
        }
        return ProjectDetailDto.builder()
                // 基础信息
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .projectDescription(entity.getProjectDescription())
                .businessTypeName(entity.getBusinessTypeName())
                .profitCenterZone(entity.getProfitCenterZone())
                .projectManagerEmployee(entity.getProjectManagerEmployee())
                .tsBm(entity.getTsBm())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                // 关联和计算信息
                .tags(tags)
                .tasks(tasks) // 设置任务列表
                .currentStageName(currentStageName)
                .projectStatus(projectStatus)
                .progress(progress)
                .build();
    }
}
