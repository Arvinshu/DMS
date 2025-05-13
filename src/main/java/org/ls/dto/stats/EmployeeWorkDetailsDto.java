/**
 * 文件路径: src/main/java/org/ls/dto/stats/EmployeeWorkDetailsDto.java
 * 开发时间: 2025-05-10 19:05:50 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 封装员工个人工作详情的完整数据。
 */
package org.ls.dto.stats;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWorkDetailsDto {

    /**
     * 员工个人的关键绩效指标 (KPIs)
     * 例如：待办任务数、进行中任务数、逾期任务数
     */
    private KpiDto kpis;

    /**
     * 按项目分组的员工任务列表
     * 用于详细展示员工在各个项目中的具体任务情况
     */
    private List<EmployeeProjectTaskDto> tasksByProject;

    // 可以添加员工相关的其他统计信息
    // 例如：
    // private List<ChartDataPointDto> taskStatusDistributionForEmployee; // 该员工的任务状态分布
    // private Double averageWorkloadPerProject; // 平均每个项目的工时投入（如果集成工时）
}
