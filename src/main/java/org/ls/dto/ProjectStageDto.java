/**
 * 文件路径: src/main/java/org/ls/dto/ProjectStageDto.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目阶段数据传输对象，用于 API 交互
 */
package org.ls.dto;

import jakarta.validation.constraints.NotBlank; // 使用 Jakarta EE 9+ 验证注解
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ls.entity.ProjectStage; // 引入实体类方便转换

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStageDto {

    /**
     * 阶段唯一标识符 (只在响应或更新时使用)
     */
    private Integer stageId;

    /**
     * 阶段排序号 (必须)
     */
    @NotNull(message = "阶段排序号不能为空")
    private Integer stageOrder;

    /**
     * 阶段名称 (必须，且不能为空白)
     */
    @NotBlank(message = "阶段名称不能为空")
    @Size(max = 100, message = "阶段名称长度不能超过100个字符")
    private String stageName;

    /**
     * 阶段描述 (可选)
     */
    private String stageDescription;

    /**
     * 是否可用 (必须)
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean isEnabled;

    /**
     * 记录创建时间 (只在响应时使用)
     */
    private OffsetDateTime createdAt;

    /**
     * 记录最后更新时间 (只在响应时使用)
     */
    private OffsetDateTime updatedAt;

    /**
     * 从实体对象转换为 DTO 对象
     *
     * @param entity ProjectStage 实体
     * @return ProjectStageDto 对象，如果实体为 null 则返回 null
     */
    public static ProjectStageDto fromEntity(ProjectStage entity) {
        if (entity == null) {
            return null;
        }
        return ProjectStageDto.builder()
                .stageId(entity.getStageId())
                .stageOrder(entity.getStageOrder())
                .stageName(entity.getStageName())
                .stageDescription(entity.getStageDescription())
                .isEnabled(entity.getIsEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 DTO 对象转换为实体对象 (用于创建或更新)
     * 注意：不包含 createdAt 和 updatedAt，这些由数据库或 Service 层处理
     *
     * @return ProjectStage 实体对象
     */
    public ProjectStage toEntity() {
        return ProjectStage.builder()
                .stageId(this.stageId) // 更新时需要 ID
                .stageOrder(this.stageOrder)
                .stageName(this.stageName)
                .stageDescription(this.stageDescription)
                .isEnabled(this.isEnabled)
                // createdAt 和 updatedAt 不在此处设置
                .build();
    }
}
