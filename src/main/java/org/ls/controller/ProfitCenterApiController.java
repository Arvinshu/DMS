package org.ls.controller;

import org.ls.entity.ProfitCenter;
import org.ls.service.ProfitCenterService;
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
 * 处理利润中心数据 (t_profit_center) 相关 API 请求的控制器
 * 文件路径: src/main/java/org/ls/controller/ProfitCenterApiController.java
 */
@RestController
@RequestMapping("/api/profit-centers")
public class ProfitCenterApiController {

    private static final Logger log = LoggerFactory.getLogger(ProfitCenterApiController.class);

    private final ProfitCenterService profitCenterService;

    @Autowired
    public ProfitCenterApiController(ProfitCenterService profitCenterService) {
        this.profitCenterService = profitCenterService;
    }

    /**
     * 获取利润中心列表（分页和过滤）
     * @param page 当前页码
     * @param size 每页大小
     * @param params 过滤参数 Map
     * @return ResponseEntity 包含利润中心列表和分页信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfitCenters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("接收到利润中心查询请求: page={}, size={}, params={}", page, size, params);
        // Service 层会处理参数清理
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("limit", size);

        try {
            List<ProfitCenter> data = profitCenterService.findProfitCenters(params);
            long totalRecords = profitCenterService.countProfitCenters(params);
            long totalPages = (totalRecords + size - 1) / size;

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", totalPages);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询利润中心列表时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "查询失败", "message", e.getMessage()));
        }
    }

    /**
     * 根据 Zone (主键) 获取单个利润中心信息
     * @param zone 利润中心全名 (URL编码可能需要处理特殊字符)
     * @return ResponseEntity 包含 ProfitCenter 或 404
     */
    @GetMapping("/{zone}")
    public ResponseEntity<ProfitCenter> getProfitCenterByZone(@PathVariable String zone) {
        // 清理路径变量
        String cleanZone = StringUtils.simpleSanitize(zone);
        ProfitCenter profitCenter = profitCenterService.findProfitCenterByZone(cleanZone);
        if (profitCenter != null) {
            return ResponseEntity.ok(profitCenter);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 添加新利润中心
     * @param profitCenter 从请求体 JSON 映射的 ProfitCenter 对象
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addProfitCenter(@RequestBody ProfitCenter profitCenter) {
        try {
            // Service 层处理验证和清理
            int result = profitCenterService.addProfitCenter(profitCenter);
            if (result > 0) {
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "利润中心添加成功", "zone", profitCenter.getZone()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "利润中心添加失败"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("添加利润中心验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("添加利润中心时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "添加利润中心时发生内部错误"));
        }
    }

    /**
     * 更新利润中心信息
     * @param zone 利润中心全名 (主键)
     * @param profitCenter 从请求体 JSON 映射的 ProfitCenter 对象 (包含要更新的字段)
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PutMapping("/{zone}")
    public ResponseEntity<Map<String, Object>> updateProfitCenter(@PathVariable String zone, @RequestBody ProfitCenter profitCenter) {
        // 清理路径变量
        String cleanZone = StringUtils.simpleSanitize(zone);
        profitCenter.setZone(cleanZone); // 确保主键一致
        try {
            // Service 层处理验证和清理
            int result = profitCenterService.updateProfitCenter(profitCenter);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "利润中心更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "利润中心未找到或未更新"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("更新利润中心验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新利润中心时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "更新利润中心时发生内部错误"));
        }
    }

    /**
     * 删除利润中心
     * @param zone 利润中心全名 (主键)
     * @return ResponseEntity (204 No Content 或 404 Not Found)
     */
    @DeleteMapping("/{zone}")
    public ResponseEntity<Void> deleteProfitCenter(@PathVariable String zone) {
        // 清理路径变量
        String cleanZone = StringUtils.simpleSanitize(zone);
        try {
            int result = profitCenterService.deleteProfitCenter(cleanZone);
            if (result > 0) {
                return ResponseEntity.noContent().build(); // 204
            } else {
                return ResponseEntity.notFound().build(); // 404
            }
        } catch (Exception e) {
            log.error("删除利润中心时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
