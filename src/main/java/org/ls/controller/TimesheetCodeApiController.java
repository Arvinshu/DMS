package org.ls.controller;

import org.ls.entity.TimesheetCode;
import org.ls.service.TimesheetCodeService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理工时编码数据 (t_timesheet_code) 相关 API 请求的控制器
 * 文件路径: src/main/java/org/ls/controller/TimesheetCodeApiController.java
 */
@RestController
@RequestMapping("/api/timesheet-codes") // 使用连字符风格的 URL
public class TimesheetCodeApiController {

    private static final Logger log = LoggerFactory.getLogger(TimesheetCodeApiController.class);

    private final TimesheetCodeService timesheetCodeService;

    @Autowired
    public TimesheetCodeApiController(TimesheetCodeService timesheetCodeService) {
        this.timesheetCodeService = timesheetCodeService;
    }

    /**
     * 获取工时编码列表（分页和过滤）
     * @param page 当前页码
     * @param size 每页大小
     * @param params 过滤参数 Map
     * @return ResponseEntity 包含工时编码列表和分页信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTimesheetCodes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("接收到工时编码查询请求: page={}, size={}, params={}", page, size, params);
        // Service 层会处理参数清理
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("limit", size);

        try {
            List<TimesheetCode> data = timesheetCodeService.findTimesheetCodes(params);
            long totalRecords = timesheetCodeService.countTimesheetCodes(params);
            long totalPages = (totalRecords + size - 1) / size;

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", totalPages);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询工时编码列表时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "查询失败", "message", e.getMessage()));
        }
    }

    /**
     * 根据工时编码 (主键) 获取单个工时编码信息
     * @param tsBm 工时编码 (URL编码可能需要处理特殊字符)
     * @return ResponseEntity 包含 TimesheetCode 或 404
     */
    @GetMapping("/{tsBm}")
    public ResponseEntity<TimesheetCode> getTimesheetCodeByTsBm(@PathVariable String tsBm) {
        // 清理路径变量
        String cleanTsBm = StringUtils.simpleSanitize(tsBm);
        TimesheetCode timesheetCode = timesheetCodeService.findTimesheetCodeByTsBm(cleanTsBm);
        if (timesheetCode != null) {
            return ResponseEntity.ok(timesheetCode);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 添加新工时编码
     * @param timesheetCode 从请求体 JSON 映射的 TimesheetCode 对象
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addTimesheetCode(@RequestBody TimesheetCode timesheetCode) {
        try {
            // Service 层处理验证和清理
            int result = timesheetCodeService.addTimesheetCode(timesheetCode);
            if (result > 0) {
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "工时编码添加成功", "tsBm", timesheetCode.getTsBm()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "工时编码添加失败"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("添加工时编码验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("添加工时编码时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "添加工时编码时发生内部错误"));
        }
    }

    /**
     * 更新工时编码信息
     * @param tsBm 工时编码 (主键)
     * @param timesheetCode 从请求体 JSON 映射的 TimesheetCode 对象 (包含要更新的字段)
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PutMapping("/{tsBm}")
    public ResponseEntity<Map<String, Object>> updateTimesheetCode(@PathVariable String tsBm, @RequestBody TimesheetCode timesheetCode) {
        // 清理路径变量
        String cleanTsBm = StringUtils.simpleSanitize(tsBm);
        timesheetCode.setTsBm(cleanTsBm); // 确保主键一致
        try {
            // Service 层处理验证和清理
            int result = timesheetCodeService.updateTimesheetCode(timesheetCode);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "工时编码更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "工时编码未找到或未更新"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("更新工时编码验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新工时编码时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "更新工时编码时发生内部错误"));
        }
    }

    /**
     * 删除工时编码
     * @param tsBm 工时编码 (主键)
     * @return ResponseEntity (204 No Content 或 404 Not Found)
     */
    @DeleteMapping("/{tsBm}")
    public ResponseEntity<Void> deleteTimesheetCode(@PathVariable String tsBm) {
        // 清理路径变量
        String cleanTsBm = StringUtils.simpleSanitize(tsBm);
        try {
            int result = timesheetCodeService.deleteTimesheetCode(cleanTsBm);
            if (result > 0) {
                return ResponseEntity.noContent().build(); // 204
            } else {
                return ResponseEntity.notFound().build(); // 404
            }
        } catch (Exception e) {
            log.error("删除工时编码时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
