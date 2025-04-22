package org.ls.controller;

import org.ls.entity.BusinessType;
import org.ls.service.BusinessTypeService;
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
 * 处理业务类型数据 (t_business_type) 相关 API 请求的控制器
 * 文件路径: src/main/java/org/ls/controller/BusinessTypeApiController.java
 */
@RestController
@RequestMapping("/api/business-types")
public class BusinessTypeApiController {

    private static final Logger log = LoggerFactory.getLogger(BusinessTypeApiController.class);

    private final BusinessTypeService businessTypeService;

    @Autowired
    public BusinessTypeApiController(BusinessTypeService businessTypeService) {
        this.businessTypeService = businessTypeService;
    }

    /**
     * 获取业务类型列表（分页和过滤）
     * @param page 当前页码
     * @param size 每页大小
     * @param params 过滤参数 Map
     * @return ResponseEntity 包含业务类型列表和分页信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBusinessTypes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("接收到业务类型查询请求: page={}, size={}, params={}", page, size, params);
        // Service 层会处理参数清理
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("limit", size);

        try {
            List<BusinessType> data = businessTypeService.findBusinessTypes(params);
            long totalRecords = businessTypeService.countBusinessTypes(params);
            long totalPages = (totalRecords + size - 1) / size;

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", totalPages);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询业务类型列表时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "查询失败", "message", e.getMessage()));
        }
    }

    /**
     * 根据 ID 获取单个业务类型信息
     * @param id 业务类型 ID
     * @return ResponseEntity 包含 BusinessType 或 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessType> getBusinessTypeById(@PathVariable Integer id) {
        BusinessType businessType = businessTypeService.findBusinessTypeById(id);
        if (businessType != null) {
            return ResponseEntity.ok(businessType);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 添加新业务类型
     * @param businessType 从请求体 JSON 映射的 BusinessType 对象
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addBusinessType(@RequestBody BusinessType businessType) {
        try {
            // Service 层处理验证和清理
            int result = businessTypeService.addBusinessType(businessType);
            if (result > 0 && businessType.getId() != null) { // 确保 ID 已回填
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "业务类型添加成功", "id", businessType.getId()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "业务类型添加失败"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("添加业务类型验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("添加业务类型时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "添加业务类型时发生内部错误"));
        }
    }

    /**
     * 更新业务类型信息
     * @param id 业务类型 ID
     * @param businessType 从请求体 JSON 映射的 BusinessType 对象 (包含要更新的字段)
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBusinessType(@PathVariable Integer id, @RequestBody BusinessType businessType) {
        businessType.setId(id); // 确保 ID 一致
        try {
            // Service 层处理验证和清理
            int result = businessTypeService.updateBusinessType(businessType);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "业务类型更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "业务类型未找到或未更新"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("更新业务类型验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新业务类型时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "更新业务类型时发生内部错误"));
        }
    }

    /**
     * 删除业务类型
     * @param id 业务类型 ID
     * @return ResponseEntity (204 No Content, 404 Not Found, 或 400 Bad Request)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBusinessType(@PathVariable Integer id) {
        try {
            int result = businessTypeService.deleteBusinessType(id);
            if (result > 0) {
                return ResponseEntity.noContent().build(); // 204
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "业务类型未找到")); // 404
            }
        } catch (IllegalStateException e) {
            log.warn("删除业务类型失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); // 400
        } catch (Exception e) {
            log.error("删除业务类型时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "删除业务类型时发生内部错误")); // 500
        }
    }
}
