package org.ls.service;

import org.ls.entity.Employee;

import java.util.List;
import java.util.Map;

/**
 * 员工信息服务接口
 * 定义员工信息表 (t_employee) 相关的业务逻辑操作。
 * 文件路径: src/main/java/org/ls/service/EmployeeService.java
 */
public interface EmployeeService {

    /**
     * 根据员工信息字符串 (工号+姓名, 主键) 查询员工。
     *
     * @param employee 员工信息字符串 (主键)。
     * @return Employee 实体，如果未找到则返回 null。
     */
    Employee findEmployeeByEmployeeStr(String employee);

    /**
     * 根据员工工号查询员工。
     *
     * @param employeeId 员工工号 (唯一)。
     * @return Employee 实体，如果未找到则返回 null。
     */
    Employee findEmployeeById(String employeeId);

    /**
     * 查询员工列表（支持过滤和分页）。
     * 实现类中应处理分页和过滤逻辑。
     *
     * @param params 包含过滤条件和分页参数 (offset, limit) 的 Map。
     * @return Employee 实体列表。
     */
    List<Employee> findEmployees(Map<String, Object> params);

    /**
     * 统计符合条件的员工总数。
     * 用于分页计算。
     *
     * @param params 包含过滤条件的 Map (同 findEmployees)。
     * @return 记录总数。
     */
    long countEmployees(Map<String, Object> params);

    /**
     * 查询指定部门下的所有员工。
     *
     * @param depId 部门 ID。
     * @return 属于该部门的 Employee 实体列表。
     */
    List<Employee> findEmployeesByDepartment(Integer depId);

    /**
     * 添加一个新员工。
     * 实现类中应进行必要的验证（如主键、工号唯一性，部门存在性，输入清理）。
     *
     * @param employee 要添加的员工实体 (employee, employeeId, employeeName 必须提供)。
     * @return 返回操作影响的行数，通常为 1 表示成功。
     * @throws // 可能抛出业务异常，例如主键或工号已存在、部门不存在、验证失败
     */
    int addEmployee(Employee employee);

    /**
     * 更新现有的员工信息。
     * 实现类中应进行必要的验证（如部门存在性，输入清理）。
     *
     * @param employee 包含更新信息和主键 employee 的员工实体。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如记录不存在、部门不存在、验证失败
     */
    int updateEmployee(Employee employee);

    /**
     * 根据员工信息字符串 (主键) 删除员工。
     * 实现类中可能需要检查该员工是否为部门负责人。
     *
     * @param employee 员工信息字符串 (主键)。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如员工是部门负责人不允许删除
     */
    int deleteEmployee(String employee);
}
