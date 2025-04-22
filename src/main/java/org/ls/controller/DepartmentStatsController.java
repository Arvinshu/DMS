package org.ls.controller;

import org.ls.dto.*; // 引入所有相关的 DTO
import org.ls.service.DepartmentStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections; // 引入 Collections 用于返回空列表
import java.util.List;
import java.util.Map;

/**
 * 处理部门工作统计相关 API 请求的控制器。
 * 文件路径: src/main/java/org/ls/controller/DepartmentStatsController.java
 */
@RestController
@RequestMapping("/api/stats") // API 基础路径
public class DepartmentStatsController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentStatsController.class);

    private final DepartmentStatsService departmentStatsService;

    @Autowired
    public DepartmentStatsController(DepartmentStatsService departmentStatsService) {
        this.departmentStatsService = departmentStatsService;
    }

    /**
     * 获取页面顶部的公共统计信息。
     * @param startDate 开始日期 (可选, 格式 YYYY-MM-DD)
     * @param endDate 结束日期 (可选, 格式 YYYY-MM-DD)
     * @return ResponseEntity 包含 CommonStatsInfo DTO 或错误信息。
     */
    @GetMapping("/common-info")
    public ResponseEntity<CommonStatsInfo> getCommonInfo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("收到获取公共统计信息请求，日期范围: {} 到 {}", startDate, endDate);
        // 如果日期为空，Service 层会处理默认值（年初至今）
        try {
            CommonStatsInfo info = departmentStatsService.getCommonStatsInfo(startDate, endDate);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("获取公共统计信息时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 或者返回包含错误信息的 DTO
        }
    }

    /**
     * 获取员工工时明细统计数据。
     * @param startDate 开始日期 (可选, 格式 YYYY-MM-DD)
     * @param endDate 结束日期 (可选, 格式 YYYY-MM-DD)
     * @return ResponseEntity 包含 EmployeeTimesheetRow DTO 列表或错误信息。
     */
    @GetMapping("/employee-details")
    public ResponseEntity<List<EmployeeTimesheetRow>> getEmployeeDetails(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("收到获取员工工时明细请求，日期范围: {} 到 {}", startDate, endDate);
        try {
            List<EmployeeTimesheetRow> data = departmentStatsService.getEmployeeTimesheetDetails(startDate, endDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("获取员工工时明细时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * 获取部门整体情况统计数据。
     * @param startDate 开始日期 (可选, 格式 YYYY-MM-DD)
     * @param endDate 结束日期 (可选, 格式 YYYY-MM-DD)
     * @return ResponseEntity 包含 DepartmentOverviewRow DTO 列表或错误信息。
     */
    @GetMapping("/department-overview")
    public ResponseEntity<List<DepartmentOverviewRow>> getDepartmentOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("收到获取部门整体情况请求，日期范围: {} 到 {}", startDate, endDate);
        try {
            List<DepartmentOverviewRow> data = departmentStatsService.getDepartmentOverview(startDate, endDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("获取部门整体情况时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * 获取重点项目统计数据。
     * @param startDate 开始日期 (可选, 格式 YYYY-MM-DD)
     * @param endDate 结束日期 (可选, 格式 YYYY-MM-DD)
     * @return ResponseEntity 包含 KeyProjectRow DTO 列表或错误信息。
     */
    @GetMapping("/key-projects")
    public ResponseEntity<List<KeyProjectRow>> getKeyProjects(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("收到获取重点项目统计请求，日期范围: {} 到 {}", startDate, endDate);
        try {
            List<KeyProjectRow> data = departmentStatsService.getKeyProjectStats(startDate, endDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("获取重点项目统计时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * 获取按利润中心和部门统计的交叉表数据。
     * @param startDate 开始日期 (可选, 格式 YYYY-MM-DD)
     * @param endDate 结束日期 (可选, 格式 YYYY-MM-DD)
     * @return ResponseEntity 包含 ProfitCenterPivotTable DTO 或错误信息。
     */
    @GetMapping("/profit-center-pivot")
    public ResponseEntity<ProfitCenterPivotTable> getProfitCenterPivot(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("收到获取利润中心交叉表请求，日期范围: {} 到 {}", startDate, endDate);
        try {
            ProfitCenterPivotTable data = departmentStatsService.getProfitCenterStats(startDate, endDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("获取利润中心交叉表时发生错误", e);
            // 返回一个空的 DTO 结构可能比返回 null 更好，以避免前端 JS 出错
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProfitCenterPivotTable(Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()));
        }
    }


    // --- 新增月度项目统计端点 ---
    /**
     * 获取按月统计的项目数量。
     * @param startDate 开始日期 (可选, 格式 YYYY-MM-DD)
     * @param endDate 结束日期 (可选, 格式 YYYY-MM-DD)
     * @return ResponseEntity 包含 MonthlyProjectCount DTO 列表或错误信息。
     */
    @GetMapping("/monthly-project-counts")
    public ResponseEntity<List<MonthlyProjectCount>> getMonthlyProjectCounts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("收到获取月度项目统计请求，日期范围: {} 到 {}", startDate, endDate);
        try {
            List<MonthlyProjectCount> data = departmentStatsService.getMonthlyProjectCounts(startDate, endDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("获取月度项目统计时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

}

//        * **说明:**
//        * 使用 `@RestController`，所有方法默认返回 JSON。
//        * 基础路径为 `/api/stats`。
//        * 注入了 `DepartmentStatsService`。
//        * 为公共信息区和四个统计表格分别创建了 `@GetMapping` 端点。
//        * 使用 `@RequestParam(required = false)` 使 `startDate` 和 `endDate` 成为可选参数。
//        * 使用 `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` 告知 Spring 如何将请求参数中的字符串（预期为 "YYYY-MM-DD" 格式）解析为 `LocalDate` 对象。如果参数未提供或格式错误，它们将为 `null`。Service 层会处理 `null` 日期并应用默认值（年初至今）。
//        * 调用对应的 Service 方法获取数据。
//        * 将 Service 返回的 DTO 包装在 `ResponseEntity.ok()` 中返回给前端。
//        * 添加了基本的 `try-catch` 块来捕获 Service 层可能抛出的未处理异常，并返回 500 错误状态码和空的 DTO 或列表，以防止前端 JS 因 `null` 而报错。
//        * 添加了日志