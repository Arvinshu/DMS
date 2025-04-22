package org.ls.controller;

import org.ls.dto.BatchInsertResult; // 导入 DTO
import org.ls.entity.TimesheetWork;
import org.ls.service.TimesheetService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections; // 引入 Collections
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // 引入 Objects 用于比较

/**
 * 处理工时数据 (t_wkt) 相关 API 请求的控制器 (主键修正版)
 * 文件路径: src/main/java/org/ls/controller/TimesheetApiController.java
 * 修正：适配新的四列组合主键 (ts_id, employee, ts_date, ts_bm)。
 */
@RestController
@RequestMapping("/api/timesheets")
public class TimesheetApiController {

    private static final Logger log = LoggerFactory.getLogger(TimesheetApiController.class);

    private final TimesheetService timesheetService;

    @Autowired
    public TimesheetApiController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    /**
     * 获取工时统计数据（分页和过滤）
     * (此方法不受主键变更影响，保持不变)
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getTimesheetStatistics(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, Object> params
    ) {
        log.debug("接收到工时统计查询请求: page={}, size={}, params={}", page, size, params);
        params.forEach((key, value) -> {
            if (value instanceof String) {
                params.put(key, StringUtils.simpleSanitize((String) value));
            }
        });
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("limit", size);

        try {
            List<Map<String, Object>> data = timesheetService.findTimesheetStatistics(params);
            long totalRecords = timesheetService.countTimesheetStatistics(params);
            long totalPages = (totalRecords + size - 1) / size;
            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", totalPages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询工时统计数据时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "查询失败", "message", e.getMessage()));
        }
    }

    /**
     * 根据复合主键获取单个工时记录
     * (修正：URL 路径和方法签名增加 tsBm)
     * @param tsId 工时申请单号
     * @param employee 员工信息
     * @param tsDate 工时日期 (格式: yyyy-MM-dd)
     * @param tsBm 工时编码 (新增路径变量)
     * @return ResponseEntity 包含 TimesheetWork 或 404
     */
    @GetMapping("/{tsId}/{employee}/{tsDate}/{tsBm}") // 增加 tsBm 路径变量
    public ResponseEntity<TimesheetWork> getTimesheetById(
            @PathVariable String tsId,
            @PathVariable String employee,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tsDate,
            @PathVariable String tsBm) { // 新增 tsBm 参数

        // 清理路径变量
        String cleanTsId = StringUtils.simpleSanitize(tsId);
        String cleanEmployee = StringUtils.simpleSanitize(employee);
        String cleanTsBm = StringUtils.simpleSanitize(tsBm); // 清理 tsBm

        // 调用 Service 时传递所有四个主键参数
        TimesheetWork timesheet = timesheetService.findTimesheetById(cleanTsId, cleanEmployee, tsDate, cleanTsBm);
        if (timesheet != null) {
            return ResponseEntity.ok(timesheet);
        } else {
            log.warn("工时记录未找到，Key: tsId={}, employee={}, tsDate={}, tsBm={}", cleanTsId, cleanEmployee, tsDate, cleanTsBm);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 添加新的工时记录
     * (此方法不受主键变更影响，保持不变，Service 层会验证所有主键)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addTimesheet(@RequestBody TimesheetWork timesheetWork) {
        try {
            int result = timesheetService.addTimesheet(timesheetWork);
            if (result > 0) {
                // 可以考虑返回更详细的定位信息，包含所有主键
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                        "message", "工时记录添加成功",
                        "tsId", timesheetWork.getTsId(),
                        "employee", timesheetWork.getEmployee(),
                        "tsDate", timesheetWork.getTsDate(),
                        "tsBm", timesheetWork.getTsBm()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "工时记录添加失败"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("添加工时记录验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("添加工时记录时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "添加工时记录时发生内部错误"));
        }
    }

    /**
     * 更新工时记录
     * (修正：URL 路径和方法签名增加 tsBm，并验证路径与 Body 中的主键一致性)
     * @param tsId 工时申请单号 (来自路径)
     * @param employee 员工信息 (来自路径)
     * @param tsDate 工时日期 (来自路径)
     * @param tsBm 工时编码 (来自路径, 新增)
     * @param timesheetWork 从请求体 JSON 映射的 TimesheetWork 对象 (包含要更新的字段及主键)
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PutMapping("/{tsId}/{employee}/{tsDate}/{tsBm}") // 增加 tsBm 路径变量
    public ResponseEntity<Map<String, Object>> updateTimesheet(
            @PathVariable String tsId,
            @PathVariable String employee,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tsDate,
            @PathVariable String tsBm, // 新增 tsBm 参数
            @RequestBody TimesheetWork timesheetWork) {

        // 清理路径变量
        String cleanTsId = StringUtils.simpleSanitize(tsId);
        String cleanEmployee = StringUtils.simpleSanitize(employee);
        String cleanTsBm = StringUtils.simpleSanitize(tsBm); // 清理 tsBm

        // 验证 Body 中的主键与路径变量是否一致
        if (!Objects.equals(cleanTsId, timesheetWork.getTsId()) ||
                !Objects.equals(cleanEmployee, timesheetWork.getEmployee()) ||
                !Objects.equals(tsDate, timesheetWork.getTsDate()) ||
                !Objects.equals(cleanTsBm, timesheetWork.getTsBm())) {
            log.warn("更新工时记录失败：URL 路径中的主键与请求体中的主键不匹配。URL: ({}, {}, {}, {}), Body: ({}, {}, {}, {})",
                    cleanTsId, cleanEmployee, tsDate, cleanTsBm,
                    timesheetWork.getTsId(), timesheetWork.getEmployee(), timesheetWork.getTsDate(), timesheetWork.getTsBm());
            return ResponseEntity.badRequest().body(Map.of("message", "URL路径与请求体中的主键不匹配"));
        }

        // 设置清理后的主键到对象中（可选，如果 Body 中可能未清理）
        timesheetWork.setTsId(cleanTsId);
        timesheetWork.setEmployee(cleanEmployee);
        timesheetWork.setTsDate(tsDate);
        timesheetWork.setTsBm(cleanTsBm);


        try {
            int result = timesheetService.updateTimesheet(timesheetWork);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "工时记录更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "工时记录未找到或未更新"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("更新工时记录验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新工时记录时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "更新工时记录时发生内部错误"));
        }
    }

    /**
     * 删除工时记录
     * (修正：URL 路径和方法签名增加 tsBm)
     * @param tsId 工时申请单号
     * @param employee 员工信息
     * @param tsDate 工时日期
     * @param tsBm 工时编码 (新增路径变量)
     * @return ResponseEntity (204 No Content 或 404 Not Found)
     */
    @DeleteMapping("/{tsId}/{employee}/{tsDate}/{tsBm}") // 增加 tsBm 路径变量
    public ResponseEntity<Void> deleteTimesheet(
            @PathVariable String tsId,
            @PathVariable String employee,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tsDate,
            @PathVariable String tsBm) { // 新增 tsBm 参数

        // 清理路径变量
        String cleanTsId = StringUtils.simpleSanitize(tsId);
        String cleanEmployee = StringUtils.simpleSanitize(employee);
        String cleanTsBm = StringUtils.simpleSanitize(tsBm); // 清理 tsBm

        try {
            // 调用 Service 时传递所有四个主键参数
            int result = timesheetService.deleteTimesheet(cleanTsId, cleanEmployee, tsDate, cleanTsBm);
            if (result > 0) {
                return ResponseEntity.noContent().build(); // 204 No Content
            } else {
                log.warn("尝试删除的工时记录未找到，Key: tsId={}, employee={}, tsDate={}, tsBm={}", cleanTsId, cleanEmployee, tsDate, cleanTsBm);
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        } catch (IllegalArgumentException e) { // Service 层可能抛出验证异常
            log.warn("删除工时记录验证失败: {}", e.getMessage());
            // 根据需要返回 400 Bad Request
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("删除工时记录时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- 新增批量导入端点 ---

    /**
     * 接收前端提交的批量工时数据并进行导入处理。
     * @param timesheets 从请求体 JSON 数组映射的 TimesheetWork 对象列表。
     * @return ResponseEntity 包含一个 BatchInsertResult 列表，指示每条记录的导入状态。
     */
    @PostMapping("/batch")
    public ResponseEntity<List<BatchInsertResult>> batchAddTimesheets(@RequestBody List<TimesheetWork> timesheets) {
        if (timesheets == null || timesheets.isEmpty()) {
            log.warn("接收到空的批量导入请求。");
            // 返回空列表或表示请求无效的错误
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
        log.info("接收到批量导入请求，共 {} 条记录。", timesheets.size());

        try {
            // 调用 Service 层进行批量处理
            List<BatchInsertResult> results = timesheetService.batchAddTimesheets(timesheets);
            log.info("批量导入处理完成，返回 {} 条结果。", results.size());
            // 直接返回 Service 层处理的结果列表
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // 捕获 Service 层可能抛出的意外错误（尽管 Service 内部已尽量处理）
            log.error("批量导入时发生意外错误: {}", e.getMessage(), e);
            // 返回一个表示整个批处理失败的通用错误响应可能不合适，
            // 因为 Service 设计为返回单条结果。
            // 这里可以选择返回 500 错误和一个包含错误信息的列表。
            // 或者，如果 Service 保证总能返回 List<BatchInsertResult>，则这里不需要 catch Exception。
            // 保持当前设计，假设 Service 能处理并返回结果列表。
            // 如果需要更复杂的错误处理，可以调整 Service 返回值或在这里构建错误响应列表。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(
                            new BatchInsertResult(-1, false, "批量处理时发生内部错误: " + e.getMessage(), null, null, null, null)
                    ));
        }
    }

    // --- 用于数据同步准备的 API 端点 (保持不变) ---
    @GetMapping("/distinct/departments")
    public ResponseEntity<List<String>> getDistinctDepartments() {
        return ResponseEntity.ok(timesheetService.getDistinctDepartments());
    }
    // ... (其他 distinct 方法保持不变) ...
    @GetMapping("/distinct/employees")
    public ResponseEntity<List<String>> getDistinctEmployees() {
        return ResponseEntity.ok(timesheetService.getDistinctEmployees());
    }

    @GetMapping("/distinct/timesheet-codes")
    public ResponseEntity<List<Map<String, String>>> getDistinctTimesheetCodes() {
        return ResponseEntity.ok(timesheetService.getDistinctTimesheetCodes());
    }

    @GetMapping("/distinct/profit-centers")
    public ResponseEntity<List<String>> getDistinctProfitCenters() {
        return ResponseEntity.ok(timesheetService.getDistinctProfitCenters());
    }

}
