package org.ls.controller;

import org.ls.dto.SyncCounts;
import org.ls.dto.SyncResult;
import org.ls.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处理数据同步相关 API 请求的控制器。
 * 文件路径: src/main/java/org/ls/controller/SyncController.java
 */
@RestController // 使用 @RestController，方法默认返回 JSON
@RequestMapping("/api/sync") // API 基础路径
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);

    private final SyncService syncService;

    @Autowired
    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * 获取用于同步页面的初始数量统计。
     * @return ResponseEntity 包含 SyncCounts DTO 或错误信息。
     */
    @GetMapping("/counts")
    public ResponseEntity<SyncCounts> getSyncCounts() {
        log.info("Received request for sync counts.");
        try {
            SyncCounts counts = syncService.getSyncCounts();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            log.error("Error getting sync counts", e);
            // 返回一个表示错误的空对象或特定错误 DTO
            return ResponseEntity.internalServerError().body(new SyncCounts()); // 或者返回 null 或错误 DTO
        }
    }

    /**
     * 触发部门数据同步。
     * @return ResponseEntity 包含 SyncResult DTO 或错误信息。
     */
    @PostMapping("/departments")
    public ResponseEntity<SyncResult> triggerDepartmentSync() {
        log.info("Received request to trigger department sync.");
        try {
            SyncResult result = syncService.syncDepartments();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during department sync", e);
            return ResponseEntity.internalServerError().body(new SyncResult(0, 0, "部门同步时发生内部错误: " + e.getMessage()));
        }
    }

    /**
     * 触发员工数据同步。
     * @return ResponseEntity 包含 SyncResult DTO 或错误信息。
     */
    @PostMapping("/employees")
    public ResponseEntity<SyncResult> triggerEmployeeSync() {
        log.info("Received request to trigger employee sync.");
        try {
            SyncResult result = syncService.syncEmployees();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during employee sync", e);
            return ResponseEntity.internalServerError().body(new SyncResult(0, 0, "员工同步时发生内部错误: " + e.getMessage()));
        }
    }

    /**
     * 触发工时编码数据同步。
     * @return ResponseEntity 包含 SyncResult DTO 或错误信息。
     */
    @PostMapping("/timesheet-codes")
    public ResponseEntity<SyncResult> triggerTimesheetCodeSync() {
        log.info("Received request to trigger timesheet code sync.");
        try {
            SyncResult result = syncService.syncTimesheetCodes();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during timesheet code sync", e);
            return ResponseEntity.internalServerError().body(new SyncResult(0, 0, "工时编码同步时发生内部错误: " + e.getMessage()));
        }
    }

    /**
     * 触发利润中心数据同步。
     * @return ResponseEntity 包含 SyncResult DTO 或错误信息。
     */
    @PostMapping("/profit-centers")
    public ResponseEntity<SyncResult> triggerProfitCenterSync() {
        log.info("Received request to trigger profit center sync.");
        try {
            SyncResult result = syncService.syncProfitCenters();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during profit center sync", e);
            return ResponseEntity.internalServerError().body(new SyncResult(0, 0, "利润中心同步时发生内部错误: " + e.getMessage()));
        }
    }

}

//        * **修改说明:**
//        * 创建了 `SyncController` 并设置了基础路径 `/api/sync`。
//        * 注入了 `SyncService`。
//        * 添加了 `GET /counts` 端点用于获取初始统计信息。
//        * 为四种同步操作分别添加了 `POST` 端点 (`/departments`, `/employees`, `/timesheet-codes`, `/profit-centers`)，它们调用对应的 Service 方法并返回 `SyncResult`。
//        * 添加了基本的日志和错误
