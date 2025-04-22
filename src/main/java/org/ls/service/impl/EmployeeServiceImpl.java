package org.ls.service.impl;

import org.ls.entity.Department;
import org.ls.entity.Employee;
import org.ls.mapper.DepartmentMapper; // 引入 DepartmentMapper
import org.ls.mapper.EmployeeMapper;
import org.ls.service.EmployeeService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 员工信息服务实现类
 * 文件路径: src/main/java/org/ls/service/impl/EmployeeServiceImpl.java
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper; // 注入 DepartmentMapper

    @Autowired
    public EmployeeServiceImpl(EmployeeMapper employeeMapper, DepartmentMapper departmentMapper) {
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public Employee findEmployeeByEmployeeStr(String employee) {
        if (StringUtils.isBlank(employee)) {
            log.warn("查询员工失败：员工信息字符串为空。");
            return null;
        }
        return employeeMapper.findByEmployee(employee);
    }

    @Override
    public Employee findEmployeeById(String employeeId) {
        if (StringUtils.isBlank(employeeId)) {
            log.warn("查询员工失败：员工工号为空。");
            return null;
        }
        return employeeMapper.findByEmployeeId(employeeId);
    }

    @Override
    public List<Employee> findEmployees(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeEmployeeFilterParams(params);
        try {
            return employeeMapper.findAll(params);
        } catch (Exception e) {
            log.error("查询员工列表时出错: params={}, error={}", params, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countEmployees(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeEmployeeFilterParams(params);
        try {
            return employeeMapper.countAll(params);
        } catch (Exception e) {
            log.error("统计员工数量时出错: params={}, error={}", params, e.getMessage(), e);
            return 0L;
        }
    }

    // 辅助方法：清理员工查询参数中的字符串
    private void sanitizeEmployeeFilterParams(Map<String, Object> params) {
        if (params != null) {
            if (params.containsKey("employee") && params.get("employee") instanceof String) {
                params.put("employee", StringUtils.simpleSanitize((String) params.get("employee")));
            }
            if (params.containsKey("employeeId") && params.get("employeeId") instanceof String) {
                params.put("employeeId", StringUtils.simpleSanitize((String) params.get("employeeId")));
            }
            if (params.containsKey("employeeName") && params.get("employeeName") instanceof String) {
                params.put("employeeName", StringUtils.simpleSanitize((String) params.get("employeeName")));
            }
            if (params.containsKey("departmentName") && params.get("departmentName") instanceof String) {
                params.put("departmentName", StringUtils.simpleSanitize((String) params.get("departmentName")));
            }
        }
    }


    @Override
    public List<Employee> findEmployeesByDepartment(Integer depId) {
        if (depId == null) {
            log.warn("按部门查询员工失败：部门 ID 为空。");
            return Collections.emptyList();
        }
        try {
            return employeeMapper.findByDepartmentId(depId);
        } catch (Exception e) {
            log.error("按部门 ID [{}] 查询员工列表时出错: {}", depId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public int addEmployee(Employee employee) {
        if (employee == null || StringUtils.isBlank(employee.getEmployee())
                || StringUtils.isBlank(employee.getEmployeeId()) || StringUtils.isBlank(employee.getEmployeeName())) {
            log.error("添加员工失败：关键参数为空或无效。 Employee: {}", employee);
            throw new IllegalArgumentException("添加员工失败：员工信息、工号和姓名不能为空。");
        }

        // 检查主键是否已存在
        if (employeeMapper.findByEmployee(employee.getEmployee()) != null) {
            log.warn("添加员工失败：员工信息 [{}] 已存在。", employee.getEmployee());
            throw new IllegalStateException("员工信息已存在，无法添加。");
        }
        // 检查工号是否已存在
        if (employeeMapper.findByEmployeeId(employee.getEmployeeId()) != null) {
            log.warn("添加员工失败：员工工号 [{}] 已存在。", employee.getEmployeeId());
            throw new IllegalStateException("员工工号已存在，无法添加。");
        }
        // 检查部门是否存在
        if (employee.getDepId() != null && departmentMapper.findById(employee.getDepId()) == null) {
            log.warn("添加员工失败：部门 ID [{}] 不存在。", employee.getDepId());
            throw new IllegalArgumentException("指定的部门不存在。");
        }

        // employee, employeeId, employeeName 来自同步，理论上不需清理，但如果允许修改则需要
        // employee.setEmployeeName(StringUtils.simpleSanitize(employee.getEmployeeName()));

        employee.setUpdatedAt(null); // 确保插入时 updated_at 为 null

        try {
            log.info("准备添加员工: {}", employee);
            return employeeMapper.insert(employee);
        } catch (Exception e) {
            log.error("添加员工时数据库操作出错: {}, error={}", employee, e.getMessage(), e);
            throw new RuntimeException("添加员工时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int updateEmployee(Employee employee) {
        if (employee == null || StringUtils.isBlank(employee.getEmployee())) {
            log.error("更新员工失败：员工信息（主键）为空。 Employee: {}", employee);
            throw new IllegalArgumentException("更新员工失败：员工信息（主键）不能为空。");
        }

        // 检查员工是否存在
        Employee existing = employeeMapper.findByEmployee(employee.getEmployee());
        if (existing == null) {
            log.warn("更新员工失败：员工信息 [{}] 不存在。", employee.getEmployee());
            return 0; // 或抛出 ResourceNotFoundException
        }

        // 检查部门是否存在 (如果传入了 depId)
        if (employee.getDepId() != null && departmentMapper.findById(employee.getDepId()) == null) {
            log.warn("更新员工失败：部门 ID [{}] 不存在。", employee.getDepId());
            throw new IllegalArgumentException("指定的部门不存在。");
        }

        // 清理可能被修改的字段 (如果允许修改姓名等)
        // if (StringUtils.isNotBlank(employee.getEmployeeName())) {
        //     employee.setEmployeeName(StringUtils.simpleSanitize(employee.getEmployeeName()));
        // }

        // employeeId, employeeName 不应在此更新
        employee.setEmployeeId(null);
        employee.setEmployeeName(null);

        // 设置更新时间 (如果不由数据库触发器处理)
        // employee.setUpdatedAt(LocalDateTime.now()); // Mapper XML 中已使用 CURRENT_TIMESTAMP

        try {
            log.info("准备更新员工: {}", employee);
            return employeeMapper.update(employee);
        } catch (Exception e) {
            log.error("更新员工时数据库操作出错: {}, error={}", employee, e.getMessage(), e);
            throw new RuntimeException("更新员工时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int deleteEmployee(String employee) {
        if (StringUtils.isBlank(employee)) {
            log.warn("删除员工失败：员工信息（主键）为空。");
            return 0;
        }

        // 检查员工是否存在
        Employee existing = employeeMapper.findByEmployee(employee);
        if (existing == null) {
            log.warn("删除员工失败：员工信息 [{}] 不存在。", employee);
            return 0;
        }

        // 检查员工是否是某个部门的负责人或副经理
        // List<Department> managedDepts = departmentMapper.findAll(Map.of("managerId", employee));
        // List<Department> assistManagedDepts = departmentMapper.findAll(Map.of("assistantManagerId", employee));
        // if (!managedDepts.isEmpty() || !assistManagedDepts.isEmpty()) {
        //     log.warn("删除员工失败：员工 [{}] 是部门负责人或副经理。", employee);
        //     throw new IllegalStateException("无法删除员工：该员工是部门负责人或副经理。");
        // }
        // 注意：上面的检查需要 DepartmentMapper 支持按 managerId/assistantManagerId 查询

        try {
            log.info("准备删除员工: {}", employee);
            return employeeMapper.delete(employee);
        } catch (Exception e) {
            // 需要考虑外键约束失败的情况 (如果 t_department 设置了外键)
            log.error("删除员工时数据库操作出错: {}, error={}", employee, e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("violates foreign key constraint")) {
                log.warn("删除员工失败：员工 [{}] 可能仍被其他记录引用（如部门负责人）。", employee);
                throw new IllegalStateException("无法删除员工：该员工可能仍被其他记录引用（如部门负责人）。");
            }
            throw new RuntimeException("删除员工时发生数据库错误。", e);
        }
    }
}
