/**
 * 文件路径: src/main/java/org/ls/dto/ProjectTagDto.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目标签数据传输对象，用于 API 交互
 */
package org.ls.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ls.entity.ProjectTag; // 引入实体类方便转换

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTagDto {

    /**
     * 标签唯一标识符 (只在响应或更新时使用)
     */
    private Long tagId;

    /**
     * 标签名称 (必须，且不能为空白)
     */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 100, message = "标签名称长度不能超过100个字符")
    private String tagName;

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
     * @param entity ProjectTag 实体
     * @return ProjectTagDto 对象，如果实体为 null 则返回 null
     */
    public static ProjectTagDto fromEntity(ProjectTag entity) {
        if (entity == null) {
            return null;
        }
        return ProjectTagDto.builder()
                .tagId(entity.getTagId())
                .tagName(entity.getTagName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 DTO 对象转换为实体对象 (用于创建或更新)
     * 注意：不包含 createdAt 和 updatedAt，这些由数据库或 Service 层处理
     *
     * @return ProjectTag 实体对象
     */
    public ProjectTag toEntity() {
        return ProjectTag.builder()
                .tagId(this.tagId) // 更新时需要 ID
                .tagName(this.tagName)
                // createdAt 和 updatedAt 不在此处设置
                .build();
    }
}
