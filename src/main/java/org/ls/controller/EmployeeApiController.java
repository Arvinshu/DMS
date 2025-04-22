package org.ls.controller;

import org.ls.entity.Employee;
import org.ls.service.EmployeeService;
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
 * 处理员工数据 (t_employee) 相关 API 请求的控制器
 * 文件路径: src/main/java/org/ls/controller/EmployeeApiController.java
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeApiController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeApiController.class);

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeApiController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * 获取员工列表（分页和过滤）
     * @param page 当前页码
     * @param size 每页大小
     * @param params 过滤参数 Map
     * @return ResponseEntity 包含员工列表和分页信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getEmployees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, Object> params) {
        log.debug("接收到员工查询请求: page={}, size={}, params={}", page, size, params);
        // Service 层会处理参数清理
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("limit", size);

        try {
            List<Employee> data = employeeService.findEmployees(params);
            long totalRecords = employeeService.countEmployees(params);
            long totalPages = (totalRecords + size - 1) / size;

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("totalRecords", totalRecords);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", totalPages);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询员工列表时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "查询失败", "message", e.getMessage()));
        }
    }

    /**
     * 根据员工信息字符串 (主键) 获取单个员工信息
     * @param employeeStr 员工信息字符串 (URL编码可能需要处理特殊字符)
     * @return ResponseEntity 包含 Employee 或 404
     */
    @GetMapping("/{employeeStr}")
    public ResponseEntity<Employee> getEmployeeByStr(@PathVariable String employeeStr) {
        // 对路径变量进行清理
        String cleanEmployeeStr = StringUtils.simpleSanitize(employeeStr);
        Employee employee = employeeService.findEmployeeByEmployeeStr(cleanEmployeeStr);
        if (employee != null) {
            return ResponseEntity.ok(employee);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 添加新员工
     * @param employee 从请求体 JSON 映射的 Employee 对象
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addEmployee(@RequestBody Employee employee) {
        try {
            // Service 层处理验证和清理
            int result = employeeService.addEmployee(employee);
            if (result > 0) {
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "员工添加成功", "employee", employee.getEmployee()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "员工添加失败"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("添加员工验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("添加员工时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "添加员工时发生内部错误"));
        }
    }

    /**
     * 更新员工信息
     * @param employeeStr 员工信息字符串 (主键)
     * @param employee 从请求体 JSON 映射的 Employee 对象 (包含要更新的字段)
     * @return ResponseEntity 包含成功信息或错误信息
     */
    @PutMapping("/{employeeStr}")
    public ResponseEntity<Map<String, Object>> updateEmployee(@PathVariable String employeeStr, @RequestBody Employee employee) {
        // 清理路径变量
        String cleanEmployeeStr = StringUtils.simpleSanitize(employeeStr);
        employee.setEmployee(cleanEmployeeStr); // 确保主键一致
        try {
            // Service 层处理验证和清理
            int result = employeeService.updateEmployee(employee);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "员工更新成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "员工未找到或未更新"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("更新员工验证失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("更新员工时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "更新员工时发生内部错误"));
        }
    }

    /**
     * 删除员工
     * @param employeeStr 员工信息字符串 (主键)
     * @return ResponseEntity (204 No Content, 404 Not Found, 或 400 Bad Request)
     */
    @DeleteMapping("/{employeeStr}")
    public ResponseEntity<Map<String, Object>> deleteEmployee(@PathVariable String employeeStr) {
        // 清理路径变量
        String cleanEmployeeStr = StringUtils.simpleSanitize(employeeStr);
        try {
            int result = employeeService.deleteEmployee(cleanEmployeeStr);
            if (result > 0) {
                return ResponseEntity.noContent().build(); // 204
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "员工未找到")); // 404
            }
        } catch (IllegalStateException e) {
            log.warn("删除员工失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); // 400
        } catch (Exception e) {
            log.error("删除员工时发生内部错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "删除员工时发生内部错误")); // 500
        }
    }
}
