/**
 * 文件路径: src/main/java/org/ls/dto/stats/TaskDeadlineItemDto.java
 * 开发时间: 2025-05-10 19:05:30 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 存储单个即将到期或已逾期任务的详细信息。
 */
package org.ls.dto.stats;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // overdueDays 和 priority 只在特定情况下有值
public class TaskDeadlineItemDto {

    /**
     * 任务的唯一标识符
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务所属项目的唯一标识符
     */
    private Long projectId;

    /**
     * 任务所属项目的名称
     */
    private String projectName;

    /**
     * 任务负责人 (通常是 "工号-姓名" 格式)
     */
    private String assignee;

    /**
     * 任务的计划截止日期
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    /**
     * 任务的优先级 (例如："High", "Medium", "Low")
     * 对于即将到期的任务，此字段有助于识别高优先级任务。
     */
    private String priority;

    /**
     * 逾期天数 (仅用于已逾期任务)
     * 如果任务未逾期，此字段可以为 null 或 0。
     */
    private Integer overdueDays;

    // 可以添加任务状态字段，如果前端需要展示
    // private String taskStatus;
}
