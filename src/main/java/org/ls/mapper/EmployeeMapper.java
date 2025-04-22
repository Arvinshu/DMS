package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ls.dto.EmployeeTimesheetRow; // 引入 DTO
import org.ls.entity.Employee;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Mapper Interface for t_employee (员工信息表)
 * 文件路径: src/main/java/org/ls/mapper/EmployeeMapper.java
 */
@Mapper
public interface EmployeeMapper {

    /**
     * 根据员工信息 (工号+姓名, 主键) 查询员工
     *
     * @param employee 员工信息 (主键)
     * @return Employee 实体 或 null
     */
    Employee findByEmployee(String employee);

    /**
     * 根据员工工号查询员工
     *
     * @param employeeId 员工工号 (唯一)
     * @return Employee 实体 或 null
     */
    Employee findByEmployeeId(String employeeId);

    /**
     * 查询员工列表 (支持过滤和分页)
     *
     * @param params 包含过滤条件和分页参数的 Map
     * - 可选过滤条件: employee (模糊), employeeId (模糊), employeeName (模糊), depId, active, statistics
     * - 分页参数: offset, limit
     * @return Employee 列表
     */
    List<Employee> findAll(Map<String, Object> params);

    /**
     * 统计符合条件的员工总数 (用于分页)
     *
     * @param params 包含过滤条件的 Map (同 findAll)
     * @return 记录总数
     */
    long countAll(Map<String, Object> params);

    /**
     * 根据部门 ID 查询员工列表
     *
     * @param depId 部门 ID
     * @return Employee 列表
     */
    List<Employee> findByDepartmentId(Integer depId);

    /**
     * 插入新的员工信息
     *
     * @param employee Employee 实体 (employee, employeeId, employeeName 必须提供)
     * @return 影响的行数
     */
    int insert(Employee employee);

    /**
     * 更新员工信息
     *
     * @param employee Employee 实体 (必须包含主键 employee)
     * @return 影响的行数
     */
    int update(Employee employee);

    /**
     * 根据员工信息 (主键) 删除员工
     *
     * @param employee 员工信息 (主键)
     * @return 影响的行数
     */
    int delete(String employee);

    /**
     * 统计所有 active=true 且 is_statistics=true 的员工总数。
     * @return 员工总数。
     */
    Long countActiveStatsEmployees();

    /**
     * 统计指定部门下 active=true 且 is_statistics=true 的员工数量。
     * @param depId 部门 ID
     * @return 该部门符合条件的员工数量。
     */
    Long countActiveStatsByDeptId(@Param("depId") Integer depId);

    /**
     * 查询指定部门下 active=true 且 is_statistics=true 的员工姓名列表 (employee 字段)。
     * @param depId 部门 ID
     * @return 员工信息字符串列表。
     */
    List<String> findActiveStatsEmployeeNamesByDeptId(@Param("depId") Integer depId);

    /**
     * 查询员工工时统计明细。
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含员工工时统计信息的 DTO 列表。
     */
    List<EmployeeTimesheetRow> findEmployeeTimesheetStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
