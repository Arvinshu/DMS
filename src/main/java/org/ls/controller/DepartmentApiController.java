package org.ls.controller;

import org.ls.entity.Department;
import org.ls.service.DepartmentService;
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
 * 处理部门数据 (t_department) 相关 API 请求的控制器
 * 文件路径: src/main/java/org/ls/controller/DepartmentApiController.java
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentApiController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentApiController.class);

    private final DepartmentService departmentService;

    @Autowired
    public DepartmentApiController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 获取部门列表（分页和过滤）
     * @param page 当前页码
     * @param size 每页大小
     * @param params 过滤参数 Map
     * @return ResponseEntity 包含部门列表和分页信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDepartments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("接收到部门查询请求: page={}, size={}, params={}", page, size, params);
        // Service 层会处理参数清理
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("limit", size);

        try {
            List<Department> data = departmentService.findDepartments(params);
            long totalRecords = departmentService.countDepartments(params);
            long totalPages = (totalRecords + size - 1) / size;

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", totalPages);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询部门列表时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "查询失败", "message", e.getMessage()));
        }
    }

    /**
     * 根据 ID 获取单个部门信息
     * @param id 部门 ID
     * @return ResponseEntity 包含 Department 或 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Integer id) {
        Department department = departmentService.findDepartmentById(id);
        if (department != null) {
            return ResponseEntity.ok(department);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 添加新部门
     * @param department 从请求体 JSON 映射的 Department 对象
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addDepartment(@RequestBody Department department) {
        try {
            // Service 层处理验证和清理
            int result = departmentService.addDepartment(department);
            if (result > 0) {
                // 返回创建的资源信息和状态码 201
                // Department createdDept = departmentService.findDepartmentById(department.getId()); // 重新查询以获取完整信息
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "部门添加成功", "id", department.getId()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "部门添加失败"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("添加部门验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("添加部门时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "添加部门时发生内部错误"));
        }
    }

    /**
     * 更新部门信息
     * @param id 部门 ID
     * @param department 从请求体 JSON 映射的 Department 对象 (包含要更新的字段)
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDepartment(@PathVariable Integer id, @RequestBody Department department) {
        department.setId(id); // 确保 ID 一致
        try {
            // Service 层处理验证和清理
            int result = departmentService.updateDepartment(department);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "部门更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "部门未找到或未更新"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("更新部门验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新部门时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "更新部门时发生内部错误"));
        }
    }

    /**
     * 删除部门
     * @param id 部门 ID
     * @return ResponseEntity (204 No Content, 404 Not Found, 或 400 Bad Request)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDepartment(@PathVariable Integer id) {
        try {
            int result = departmentService.deleteDepartment(id);
            if (result > 0) {
                return ResponseEntity.noContent().build(); // 204
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "部门未找到")); // 404
            }
        } catch (IllegalStateException e) {
            log.warn("删除部门失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); // 400
        } catch (Exception e) {
            log.error("删除部门时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "删除部门时发生内部错误")); // 500
        }
    }
}
