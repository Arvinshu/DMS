/**
 * 文件路径: src/main/java/org/ls/dto/stats/KpiDto.java
 * 开发时间: 2025-05-10 19:05:05 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 存储各项关键绩效指标 (KPI) 的数据。
 */
package org.ls.dto.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 对于数值类型，如果为null或0，根据需要决定是否序列化
public class KpiDto {

    // --- 部门整体KPIs ---
    /**
     * 活跃项目数 (例如：进行中、待办状态的项目)
     */
    private Integer activeProjects;

    /**
     * 进行中任务总数
     */
    private Integer inProgressTasks;

    /**
     * 总体逾期任务数
     */
    private Integer totalOverdueTasks;

    /**
     * 指定期间内完成的任务数 (例如：本周/本月完成任务)
     */
    private Integer completedTasksThisPeriod;

    // --- 员工个人KPIs ---
    /**
     * 员工待办任务数
     */
    private Integer employeeTodoTasks;

    /**
     * 员工进行中任务数
     */
    private Integer employeeInProgressTasks;

    /**
     * 员工逾期任务数
     */
    private Integer employeeOverdueTasks;

    // 可以根据需要添加更多KPI字段
    // 例如：
    // private Double averageTaskCompletionTime;
    // private Integer upcomingDeadlineTasks;
}
