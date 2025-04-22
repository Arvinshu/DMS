package org.ls.service;

import org.ls.entity.Department;

import java.util.List;
import java.util.Map;

/**
 * 部门信息服务接口
 * 定义部门信息表 (t_department) 相关的业务逻辑操作。
 * 文件路径: src/main/java/org/ls/service/DepartmentService.java
 */
public interface DepartmentService {

    /**
     * 根据部门 ID 查询部门信息。
     *
     * @param id 部门 ID (主键)。
     * @return Department 实体，如果未找到则返回 null。
     */
    Department findDepartmentById(Integer id);

    /**
     * 根据部门名称查询部门信息。
     *
     * @param depName 部门名称 (唯一)。
     * @return Department 实体，如果未找到则返回 null。
     */
    Department findDepartmentByName(String depName);

    /**
     * 查询部门列表（支持过滤和分页）。
     * 实现类中应处理分页和过滤逻辑。
     *
     * @param params 包含过滤条件和分页参数 (offset, limit) 的 Map。
     * @return Department 实体列表。
     */
    List<Department> findDepartments(Map<String, Object> params);

    /**
     * 统计符合条件的部门总数。
     * 用于分页计算。
     *
     * @param params 包含过滤条件的 Map (同 findDepartments)。
     * @return 记录总数。
     */
    long countDepartments(Map<String, Object> params);

    /**
     * 添加一个新的部门。
     * 实现类中应进行必要的验证（如 ID 和名称的唯一性检查，输入清理）。
     *
     * @param department 要添加的部门实体 (ID 需要手动设置)。
     * @return 返回操作影响的行数，通常为 1 表示成功。
     * @throws // 可能抛出业务异常，例如 ID 或名称已存在、验证失败
     */
    int addDepartment(Department department);

    /**
     * 更新现有的部门信息。
     * 实现类中应进行必要的验证（如输入清理）。
     *
     * @param department 包含更新信息和主键 ID 的部门实体。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如记录不存在或验证失败
     */
    int updateDepartment(Department department);

    /**
     * 根据部门 ID 删除部门。
     * 实现类中可能需要检查是否存在关联的员工。
     *
     * @param id 部门 ID。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如部门下存在员工不允许删除
     */
    int deleteDepartment(Integer id);
}
