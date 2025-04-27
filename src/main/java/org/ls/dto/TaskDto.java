/**
 * 文件路径: src/main/java/org/ls/dto/TaskDto.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 任务数据传输对象，用于 API 创建、更新和响应
 */
package org.ls.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ls.entity.Task; // 引入实体类方便转换

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {

    /**
     * 任务唯一标识符 (只在响应或更新时使用)
     */
    private Long taskId;

    /**
     * 所属项目ID (创建时必须)
     */
    @NotNull(message = "所属项目ID不能为空")
    private Long projectId;

    /**
     * 任务名称 (必须，且不能为空白)
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 255, message = "任务名称长度不能超过255个字符")
    private String taskName;

    /**
     * 任务描述 (可选)
     */
    private String taskDescription;

    /**
     * 任务优先级 (可选, 默认为 'Medium')
     */
    @Size(max = 20, message = "优先级长度不能超过20个字符")
    private String priority = "Medium"; // 提供默认值

    /**
     * 分配给的员工 (工号+姓名) (可选)
     */
    @Size(max = 30, message = "负责人信息长度不能超过30个字符")
    private String assigneeEmployee;

    /**
     * 开始日期 (可选)
     */
    private LocalDate startDate;

    /**
     * 截止日期 (可选)
     */
    private LocalDate dueDate;

    /**
     * 所属阶段ID (创建时必须)
     */
    @NotNull(message = "所属阶段ID不能为空")
    private Integer stageId;

    /**
     * 任务状态 (必须，且不能为空白)
     */
    @NotBlank(message = "任务状态不能为空")
    @Size(max = 20, message = "任务状态长度不能超过20个字符")
    private String taskStatus = "待办"; // 提供默认值

    /**
     * 附件信息 (可选)
     */
    private String attachments;

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
     * @param entity Task 实体
     * @return TaskDto 对象，如果实体为 null 则返回 null
     */
    public static TaskDto fromEntity(Task entity) {
        if (entity == null) {
            return null;
        }
        return TaskDto.builder()
                .taskId(entity.getTaskId())
                .projectId(entity.getProjectId())
                .taskName(entity.getTaskName())
                .taskDescription(entity.getTaskDescription())
                .priority(entity.getPriority())
                .assigneeEmployee(entity.getAssigneeEmployee())
                .startDate(entity.getStartDate())
                .dueDate(entity.getDueDate())
                .stageId(entity.getStageId())
                .taskStatus(entity.getTaskStatus())
                .attachments(entity.getAttachments())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 DTO 对象转换为实体对象 (用于创建或更新)
     *
     * @return Task 实体对象
     */
    public Task toEntity() {
        return Task.builder()
                .taskId(this.taskId) // 更新时需要 ID
                .projectId(this.projectId)
                .taskName(this.taskName)
                .taskDescription(this.taskDescription)
                .priority(this.priority)
                .assigneeEmployee(this.assigneeEmployee)
                .startDate(this.startDate)
                .dueDate(this.dueDate)
                .stageId(this.stageId)
                .taskStatus(this.taskStatus)
                .attachments(this.attachments)
                // createdAt 和 updatedAt 由数据库或 Service 层处理
                .build();
    }
}
