/**
 * 文件路径: src/main/java/org/ls/service/ProjectStatisticsService.java
 * 开发时间: 2025-05-10 19:35:00 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 项目统计服务接口，定义了获取各种项目和任务统计数据的方法。
 */
package org.ls.service;

import org.ls.dto.stats.DepartmentOverviewStatsDto;
import org.ls.dto.stats.AtRiskProjectDto;
import org.ls.dto.stats.TaskDeadlineStatsDto;
import org.ls.dto.stats.EmployeeWorkDetailsDto;

import java.util.List;

public interface ProjectStatisticsService {

    /**
     * 获取所有在任务中被分配过的员工列表（去重）。
     *
     * @return 去重后的任务负责人字符串列表 (例如 "E001-张三")
     */
    List<String> getDistinctTaskAssignees();

    /**
     * 获取部门整体概览统计数据。
     *
     * @param dateRangeString      日期范围字符串 (例如 "this_week", "this_month", "custom_yyyy-MM-dd_yyyy-MM-dd")
     * @param projectTypeDimension 项目构成分析的维度 ("businessTypeName" 或 "profitCenterZone")
     * @return 部门整体概览数据传输对象
     */
    DepartmentOverviewStatsDto getDepartmentOverviewStats(String dateRangeString, String projectTypeDimension);

    /**
     * 获取被识别为有风险的项目列表。
     * 风险评估基于当前日期和预定义的规则。
     *
     * @return 风险项目列表数据传输对象
     */
    List<AtRiskProjectDto> getAtRiskProjects();

    /**
     * 获取即将到期和已逾期的任务列表。
     *
     * @param upcomingDueDays    定义“即将到期”的天数（例如，未来7天内）
     * @param useGlobalOverdue   一个内部开关，指示是否查询全局逾期任务。
     * true: 查询所有逾期任务。
     * false: 可能根据系统配置的默认周期筛选逾期任务（当前简化为true时查全部）。
     * @return 任务到期与逾期情况的数据传输对象
     */
    TaskDeadlineStatsDto getTaskDeadlineInfo(int upcomingDueDays, boolean useGlobalOverdue);

    /**
     * 获取特定员工的详细工作情况统计。
     *
     * @param employeeName    员工姓名 (或 "工号-姓名" 格式)
     * @param dateRangeString 日期范围字符串，用于界定统计周期
     * @return 员工个人工作详情的数据传输对象
     */
    EmployeeWorkDetailsDto getEmployeeWorkDetails(String employeeName, String dateRangeString);

}
