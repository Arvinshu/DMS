/**
 * 文件路径: src/main/java/org/ls/dto/ProjectListDto.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 项目列表查询结果的数据传输对象，包含计算字段
 */
package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ls.entity.Project; // 引入实体类方便转换

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectListDto {

    // --- 从 Project 实体继承的字段 ---
    private Long projectId;
    private String projectName;
    // private String projectDescription; // 列表通常不需要完整描述
    private String businessTypeName;
    private String profitCenterZone; // 可以考虑在前端转换成更友好的备注名
    private String projectManagerEmployee;
    private String tsBm;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    // private String createdBy; // 列表通常不需要
    // private String updatedBy; // 列表通常不需要

    // --- 关联和计算字段 ---

    /**
     * 项目关联的标签列表
     */
    private List<ProjectTagDto> tags;

    /**
     * 计算得出的项目当前阶段名称
     */
    private String currentStageName;

    /**
     * 计算得出的项目状态
     * (待办, 进行中, 已完成, 已暂停, 已取消)
     */
    private String projectStatus;

    /**
     * 项目概览进度 (可选)
     * 可以是简单的百分比数字，或者更复杂的数据结构供前端渲染进度条
     * 例如: Map<String, Double> stageProgress = {"策划": 1.0, "需求": 0.5, ...}
     */
    private Object progress; // 使用 Object 类型增加灵活性


    /**
     * 从实体对象和其他计算数据构建 DTO
     *
     * @param entity           Project 实体
     * @param tags             关联的标签 DTO 列表
     * @param currentStageName 计算出的当前阶段名称
     * @param projectStatus    计算出的项目状态
     * @param progress         计算出的进度信息 (可选)
     * @return ProjectListDto 对象
     */
    public static ProjectListDto fromEntity(Project entity,
                                            List<ProjectTagDto> tags,
                                            String currentStageName,
                                            String projectStatus,
                                            Object progress) {
        if (entity == null) {
            return null;
        }
        return ProjectListDto.builder()
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .businessTypeName(entity.getBusinessTypeName())
                .profitCenterZone(entity.getProfitCenterZone())
                .projectManagerEmployee(entity.getProjectManagerEmployee())
                .tsBm(entity.getTsBm())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .tags(tags) // 设置关联标签
                .currentStageName(currentStageName) // 设置计算字段
                .projectStatus(projectStatus)       // 设置计算字段
                .progress(progress)                 // 设置进度信息
                .build();
    }
}
