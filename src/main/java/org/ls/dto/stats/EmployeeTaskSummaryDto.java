/**
 * 文件路径: src/main/java/org/ls/dto/stats/EmployeeTaskSummaryDto.java
 * 开发时间: 2025-05-10 19:05:40 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 存储员工个人任务的简要信息，用于在按项目分组的列表中展示。
 */
package org.ls.dto.stats;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTaskSummaryDto {

    /**
     * 任务的唯一标识符
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务当前状态 (例如："待办", "进行中", "已完成")
     */
    private String status;

    /**
     * 任务的计划截止日期
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    /**
     * 任务的优先级 (例如："High", "Medium", "Low")
     */
    private String priority;

    /**
     * 标记任务是否已逾期
     * true 表示已逾期，false 表示未逾期。
     */
    private boolean isOverdue;

    // 可以根据前端展示需求添加其他字段，如任务描述的摘要等
    // private String taskDescriptionSummary;
}
