/**
 * 文件路径: src/main/java/org/ls/dto/stats/DepartmentOverviewStatsDto.java
 * 开发时间: 2025-05-10 19:05:20 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 封装部门整体概览统计数据。
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
public class DepartmentOverviewStatsDto {

    /**
     * 部门整体的关键绩效指标 (KPIs)
     */
    private KpiDto kpis;

    /**
     * 项目状态分布数据 (用于饼图或环形图)
     * 例如：[{"label": "进行中", "value": 10}, {"label": "已完成", "value": 5}]
     */
    private List<ChartDataPointDto> projectStatusDistribution;

    /**
     * 员工任务负载数据 (用于条形图)
     * 展示每位员工的进行中和待办任务数
     */
    private List<EmployeeLoadDto> employeeLoad;

    /**
     * 项目类型/业务线分布数据 (用于饼图或条形图)
     * 根据请求参数动态决定是按业务类型还是利润中心统计
     * 例如：[{"label": "金融科技", "value": 7}, {"label": "智慧城市", "value": 3}]
     */
    private List<ChartDataPointDto> projectTypeDistribution;

    // 可以根据需求添加更多部门级别的统计图表数据
    // 例如：
    // private List<ChartDataPointDto> projectPriorityDistribution;
    // private List<ChartDataPointDto> taskCompletionTrend; // 任务完成趋势（按时间）
}
