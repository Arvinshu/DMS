/**
 * 文件路径: src/main/java/org/ls/controller/api/ProjectStatisticsApiController.java
 * 开发时间: 2025-05-10 20:10:00 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 提供项目统计相关的 RESTful API 接口。
 * 这个控制器将负责接收前端的HTTP请求，
 * 调用 ProjectStatisticsService 中相应的方法来获取数据，
 * 并以统一的JSON格式（使用我们之前定义的 ApiResponse<T>）返回给前端。
 */
package org.ls.controller.api;

import org.ls.dto.ApiResponse;
import org.ls.dto.stats.DepartmentOverviewStatsDto;
import org.ls.dto.stats.AtRiskProjectDto;
import org.ls.dto.stats.TaskDeadlineStatsDto;
import org.ls.dto.stats.EmployeeWorkDetailsDto;
import org.ls.service.ProjectStatisticsService;
import org.ls.utils.DateUtils; // 主要用于常量
import org.ls.utils.StringUtils; // 用于参数校验

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/project-stats")
public class ProjectStatisticsApiController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectStatisticsApiController.class);

    private final ProjectStatisticsService projectStatisticsService;

    // 允许的项目构成分析维度
    private static final Set<String> ALLOWED_PROJECT_TYPE_DIMENSIONS = Set.of("businessTypeName", "profitCenterZone");


    @Autowired
    public ProjectStatisticsApiController(ProjectStatisticsService projectStatisticsService) {
        this.projectStatisticsService = projectStatisticsService;
    }

    /**
     * 获取所有在任务中被分配过的员工列表（去重）。
     *
     * @return ApiResponse 包含员工姓名列表
     */
    @GetMapping("/assignees")
    public ResponseEntity<ApiResponse<List<String>>> getAssignees() {
        try {
            List<String> assignees = projectStatisticsService.getDistinctTaskAssignees();
            return ResponseEntity.ok(ApiResponse.success(assignees));
        } catch (Exception e) {
            logger.error("API 获取任务负责人列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取任务负责人列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取部门整体概览统计数据。
     *
     * @param dateRange            日期范围字符串 (例如 "this_week", "custom_2023-01-01_2023-01-31")
     * @param projectTypeDimension 项目构成分析的维度 ("businessTypeName" 或 "profitCenterZone")
     * @return ApiResponse 包含部门概览数据
     */
    @GetMapping("/department-overview")
    public ResponseEntity<ApiResponse<DepartmentOverviewStatsDto>> getDepartmentOverview(
            @RequestParam(defaultValue = DateUtils.THIS_WEEK) String dateRange, // 默认查询本周
            @RequestParam(defaultValue = "businessTypeName") String projectTypeDimension) {

        if (StringUtils.isBlank(projectTypeDimension) || !ALLOWED_PROJECT_TYPE_DIMENSIONS.contains(projectTypeDimension)) {
            logger.warn("无效的项目构成分析维度: {}。将使用默认值 'businessTypeName'。", projectTypeDimension);
            projectTypeDimension = "businessTypeName"; // 或者返回错误
            // return ResponseEntity.badRequest().body(ApiResponse.error("无效的项目构成分析维度。允许的值为: " + ALLOWED_PROJECT_TYPE_DIMENSIONS));
        }

        try {
            DepartmentOverviewStatsDto overviewStats = projectStatisticsService.getDepartmentOverviewStats(dateRange, projectTypeDimension);
            return ResponseEntity.ok(ApiResponse.success(overviewStats));
        } catch (IllegalArgumentException e) {
            logger.error("API 获取部门概览数据失败，参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("获取部门概览数据失败: " + e.getMessage()));
        }
        catch (Exception e) {
            logger.error("API 获取部门概览数据失败: dateRange={}, projectTypeDimension={}", dateRange, projectTypeDimension, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取部门概览数据时发生内部错误。"));
        }
    }

    /**
     * 获取被识别为有风险的项目列表。
     *
     * @return ApiResponse 包含风险项目列表
     */
    @GetMapping("/at-risk-projects")
    public ResponseEntity<ApiResponse<List<AtRiskProjectDto>>> getAtRiskProjects() {
        try {
            List<AtRiskProjectDto> atRiskProjects = projectStatisticsService.getAtRiskProjects();
            return ResponseEntity.ok(ApiResponse.success(atRiskProjects));
        } catch (Exception e) {
            logger.error("API 获取风险项目列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取风险项目列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取即将到期和已逾期的任务列表。
     *
     * @param upcomingDueDays 定义“即将到期”的天数 (例如，未来7天内), 默认为7
     * @return ApiResponse 包含任务到期与逾期情况数据
     */
    @GetMapping("/task-deadlines")
    public ResponseEntity<ApiResponse<TaskDeadlineStatsDto>> getTaskDeadlineInfo(
            @RequestParam(defaultValue = "7") int upcomingDueDays) {

        if (upcomingDueDays <= 0) {
            logger.warn("API /task-deadlines 调用参数 upcomingDueDays 无效: {}。应为正整数。", upcomingDueDays);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("upcomingDueDays 参数必须为正整数。"));
        }

        try {
            // useGlobalOverdue 参数由Service层内部处理其默认值和逻辑
            TaskDeadlineStatsDto deadlineInfo = projectStatisticsService.getTaskDeadlineInfo(upcomingDueDays, true); // 假设默认总是查全局逾期
            return ResponseEntity.ok(ApiResponse.success(deadlineInfo));
        } catch (Exception e) {
            logger.error("API 获取任务到期与逾期信息失败: upcomingDueDays={}", upcomingDueDays, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取任务到期与逾期信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取特定员工的详细工作情况统计。
     *
     * @param employeeName 员工姓名 (或 "工号-姓名" 格式)
     * @param dateRange    日期范围字符串，用于界定统计周期
     * @return ApiResponse 包含员工个人工作详情数据
     */
    @GetMapping("/employee-details")
    public ResponseEntity<ApiResponse<EmployeeWorkDetailsDto>> getEmployeeDetails(
            @RequestParam String employeeName,
            @RequestParam(defaultValue = DateUtils.THIS_WEEK) String dateRange) {

        if (StringUtils.isBlank(employeeName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("employeeName 参数不能为空。"));
        }

        try {
            EmployeeWorkDetailsDto employeeDetails = projectStatisticsService.getEmployeeWorkDetails(employeeName, dateRange);
            if (employeeDetails == null) { // Service层可能在找不到员工时返回null
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("未找到员工 '" + employeeName + "' 的相关数据。"));
            }
            return ResponseEntity.ok(ApiResponse.success(employeeDetails));
        } catch (IllegalArgumentException e) {
            logger.error("API 获取员工详情数据失败，参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("获取员工详情数据失败: " + e.getMessage()));
        }
        catch (Exception e) {
            logger.error("API 获取员工 {} 的工作详情失败: dateRange={}", employeeName, dateRange, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取员工工作详情时发生内部错误。"));
        }
    }
}
