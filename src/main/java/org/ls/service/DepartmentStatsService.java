package org.ls.service;

import org.ls.dto.*; // 引入所有相关的 DTO

import java.time.LocalDate;
import java.util.List;

/**
 * 部门工作统计服务接口。
 * 定义了获取统计页面所需各项数据的方法。
 * 文件路径: src/main/java/org/ls/service/DepartmentStatsService.java
 ** **说明:**
 ** 定义了五个方法，分别对应统计页面的公共信息区、员工明细表、部门概览表、重点项目表和利润中心交叉表所需的数据。
 ** 所有方法都接收 `startDate` 和 `endDate` 作为参数。
 ** 返回值是之前定义的相应 DTO 或 DTO
 */
public interface DepartmentStatsService {

    /**
     * 获取页面顶部的公共统计信息。
     * @param startDate 统计开始日期
     * @param endDate 统计结束日期
     * @return CommonStatsInfo DTO
     */
    CommonStatsInfo getCommonStatsInfo(LocalDate startDate, LocalDate endDate);

    /**
     * 获取员工工时明细统计数据。
     * @param startDate 统计开始日期
     * @param endDate 统计结束日期
     * @return EmployeeTimesheetRow DTO 列表，按项目工时率降序排列。
     */
    List<EmployeeTimesheetRow> getEmployeeTimesheetDetails(LocalDate startDate, LocalDate endDate);

    /**
     * 获取部门整体情况统计数据。
     * (将聚合 getEmployeeTimesheetDetails 的结果)
     * @param startDate 统计开始日期
     * @param endDate 统计结束日期
     * @return DepartmentOverviewRow DTO 列表，按部门 ID 排序。
     */
    List<DepartmentOverviewRow> getDepartmentOverview(LocalDate startDate, LocalDate endDate);

    /**
     * 获取重点项目统计数据 (工时 > 30 人天)。
     * @param startDate 统计开始日期
     * @param endDate 统计结束日期
     * @return KeyProjectRow DTO 列表，按累计投入人天降序排列。
     */
    List<KeyProjectRow> getKeyProjectStats(LocalDate startDate, LocalDate endDate);

    /**
     * 获取按利润中心和部门统计的交叉表数据。
     * @param startDate 统计开始日期
     * @param endDate 统计结束日期
     * @return ProfitCenterPivotTable DTO，包含表头、行数据和汇总。
     */
    ProfitCenterPivotTable getProfitCenterStats(LocalDate startDate, LocalDate endDate);

    // --- 新增方法 (用于图表) ---
    /**
     * 获取按月统计的项目数量。
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 月份和对应项目数的 DTO 列表。
     */
    List<MonthlyProjectCount> getMonthlyProjectCounts(LocalDate startDate, LocalDate endDate);


}

