package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ls.entity.Department;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Mapper Interface for t_department (部门信息表)
 * 文件路径: src/main/java/org/ls/mapper/DepartmentMapper.java
 */
@Mapper
public interface DepartmentMapper {

    /**
     * 根据部门 ID 查询部门信息
     *
     * @param id 部门 ID (主键)
     * @return Department 实体 或 null
     */
    Department findById(Integer id);

    /**
     * 根据部门名称查询部门信息
     *
     * @param depName 部门名称 (唯一)
     * @return Department 实体 或 null
     */
    Department findByName(String depName);

    /**
     * 查询部门列表 (支持过滤和分页)
     *
     * @param params 包含过滤条件和分页参数的 Map
     * - 可选过滤条件: id, depName (模糊), depLevel, managerId, assistantManagerId, active, statistics
     * - 分页参数: offset, limit
     * @return Department 列表
     */
    List<Department> findAll(Map<String, Object> params);

    /**
     * 统计符合条件的部门总数 (用于分页)
     *
     * @param params 包含过滤条件的 Map (同 findAll)
     * @return 记录总数
     */
    long countAll(Map<String, Object> params);

    /**
     * 插入新的部门信息
     *
     * @param department Department 实体 (ID 需要手动设置)
     * @return 影响的行数
     */
    int insert(Department department);

    /**
     * 更新部门信息
     *
     * @param department Department 实体 (必须包含 ID)
     * @return 影响的行数
     */
    int update(Department department);

    /**
     * 根据 ID 删除部门信息
     *
     * @param id 部门 ID
     * @return 影响的行数
     */
    int delete(Integer id);

    /**
     * 查询当前 t_department 表中最大的 ID。
     * 用于生成新的部门 ID (max + 1)。
     * @return 当前最大 ID，如果表为空则返回 0。
     */
    Integer findMaxId();

    /**
     * 根据部门名称查询部门 ID。
     * 用于在同步员工信息时，根据 t_wkt 中的部门名称查找对应的部门 ID。
     * @param depName 部门名称
     * @return 对应的部门 ID，如果未找到则返回 null。
     */
    Integer findIdByName(@Param("depName") String depName); // 使用 @Param 指定参数名

    /**
     * 查询一级部门负责人信息 (假设只有一个一级部门)。
     * @return 包含部门ID、名称和负责人姓名的 Map，或 null。
     */
    Map<String, Object> findPrimaryDepartmentHead(); // 返回 Map 可能更灵活

    /**
     * 查询所有状态为 active=true 且 is_statistics=true 的部门，并按 ID 排序。
     * 用于获取统计表格的动态列头或行。
     * @return Department 实体列表。
     */
    List<Department> findActiveStatsDepartmentsSorted();

}
