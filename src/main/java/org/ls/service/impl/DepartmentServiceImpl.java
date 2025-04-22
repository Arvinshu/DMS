package org.ls.service.impl;

import org.ls.entity.Department;
import org.ls.mapper.DepartmentMapper;
import org.ls.mapper.EmployeeMapper;
import org.ls.service.DepartmentService;
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
 * 部门信息服务实现类 (修正版)
 * - 修正 updateDepartment 方法，增加对 depName 唯一性检查（如果名称被修改）。
 * 文件路径: src/main/java/org/ls/service/impl/DepartmentServiceImpl.java
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;

    @Autowired
    public DepartmentServiceImpl(DepartmentMapper departmentMapper, EmployeeMapper employeeMapper) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Department findDepartmentById(Integer id) {
        if (id == null) {
            log.warn("查询部门失败：ID 为空。");
            return null;
        }
        return departmentMapper.findById(id);
    }

    @Override
    public Department findDepartmentByName(String depName) {
        if (StringUtils.isBlank(depName)) {
            log.warn("查询部门失败：部门名称为空。");
            return null;
        }
        // 注意：如果 depName 可能包含用户输入，应考虑清理
        // String sanitizedName = StringUtils.simpleSanitize(depName);
        // return departmentMapper.findByName(sanitizedName);
        return departmentMapper.findByName(depName); // 假设 depName 在此场景下是可信的或已清理
    }

    @Override
    public List<Department> findDepartments(Map<String, Object> params) {
        // Service 层通常不直接处理 XSS，Controller 层应负责输入清理
        // 但如果需要，可以在这里再次清理 params 中的字符串值
        try {
            return departmentMapper.findAll(params);
        } catch (Exception e) {
            log.error("查询部门列表时出错: params={}, error={}", params, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countDepartments(Map<String, Object> params) {
        try {
            return departmentMapper.countAll(params);
        } catch (Exception e) {
            log.error("统计部门数量时出错: params={}, error={}", params, e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    @Transactional
    public int addDepartment(Department department) {
        if (department == null || department.getId() == null || StringUtils.isBlank(department.getDepName())) {
            log.error("添加部门失败：关键参数为空或无效。 Department: {}", department);
            throw new IllegalArgumentException("添加部门失败：部门ID和名称不能为空。");
        }

        // 清理部门名称 (如果认为它可能来自用户输入)
        department.setDepName(StringUtils.simpleSanitize(department.getDepName()));
        if (StringUtils.isNotBlank(department.getDepLevel())) {
            department.setDepLevel(StringUtils.simpleSanitize(department.getDepLevel()));
        }

        // 检查 ID 是否已存在
        if (departmentMapper.findById(department.getId()) != null) {
            log.warn("添加部门失败：部门 ID [{}] 已存在。", department.getId());
            throw new IllegalStateException("部门 ID [" + department.getId() + "] 已存在，无法添加。");
        }
        // 检查名称是否已存在
        if (departmentMapper.findByName(department.getDepName()) != null) {
            log.warn("添加部门失败：部门名称 [{}] 已存在。", department.getDepName());
            throw new IllegalStateException("部门名称 [" + department.getDepName() + "] 已存在，无法添加。");
        }

        // 验证负责人/副经理 ID 是否存在 (可选，但建议)
        if (StringUtils.isNotBlank(department.getManagerId()) && employeeMapper.findByEmployee(department.getManagerId()) == null) {
            log.warn("添加部门失败：负责人 [{}] 不存在。", department.getManagerId());
            throw new IllegalArgumentException("指定的负责人不存在。");
        }
        if (StringUtils.isNotBlank(department.getAssistantManagerId()) && employeeMapper.findByEmployee(department.getAssistantManagerId()) == null) {
            log.warn("添加部门失败：副经理 [{}] 不存在。", department.getAssistantManagerId());
            throw new IllegalArgumentException("指定的副经理不存在。");
        }

        department.setUpdatedAt(null); // 确保插入时 updated_at 为 null

        try {
            log.info("准备添加部门: {}", department);
            return departmentMapper.insert(department);
        } catch (Exception e) {
            log.error("添加部门时数据库操作出错: {}, error={}", department, e.getMessage(), e);
            throw new RuntimeException("添加部门时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int updateDepartment(Department department) {
        if (department == null || department.getId() == null) {
            log.error("更新部门失败：ID 为空。 Department: {}", department);
            throw new IllegalArgumentException("更新部门失败：部门 ID 不能为空。");
        }

        // 1. 检查部门是否存在
        Department existing = departmentMapper.findById(department.getId());
        if (existing == null) {
            log.warn("更新部门失败：部门 ID [{}] 不存在。", department.getId());
            // throw new ResourceNotFoundException("部门不存在"); // 或者返回 0
            return 0;
        }

        // 2. 清理可能被修改的文本字段
        boolean nameChanged = false;
        if (StringUtils.isNotBlank(department.getDepName())) {
            department.setDepName(StringUtils.simpleSanitize(department.getDepName()));
            // 检查部门名称是否被修改
            if (!Objects.equals(existing.getDepName(), department.getDepName())) {
                nameChanged = true;
            }
        } else {
            // 如果传入的名称为空，则不允许更新（或根据业务设为现有名称）
            department.setDepName(null); // 避免更新为 null 或空字符串
            // throw new IllegalArgumentException("部门名称不能为空。");
        }

        if (StringUtils.isNotBlank(department.getDepLevel())) {
            department.setDepLevel(StringUtils.simpleSanitize(department.getDepLevel()));
        } else {
            department.setDepLevel(null); // 避免更新为空
        }


        // 3. 如果部门名称被修改，检查新名称的唯一性
        if (nameChanged) {
            Department departmentWithNewName = departmentMapper.findByName(department.getDepName());
            // 如果找到了同名部门，并且这个部门不是当前正在修改的部门，则名称冲突
            if (departmentWithNewName != null && !Objects.equals(departmentWithNewName.getId(), department.getId())) {
                log.warn("更新部门失败：部门名称 [{}] 已被部门 ID [{}] 使用。", department.getDepName(), departmentWithNewName.getId());
                throw new IllegalStateException("部门名称 [" + department.getDepName() + "] 已存在，无法更新。");
            }
        }

        // 4. 验证负责人/副经理 ID 是否存在 (可选，但建议)
        //    注意：允许将负责人设置为空字符串或 null，需要正确处理
        if (department.getManagerId() != null) { // 如果传入了 managerId (包括空字符串)
            if (StringUtils.isBlank(department.getManagerId())) {
                department.setManagerId(null); // 将空字符串转为 null
            } else if (employeeMapper.findByEmployee(department.getManagerId()) == null) {
                log.warn("更新部门失败：负责人 [{}] 不存在。", department.getManagerId());
                throw new IllegalArgumentException("指定的负责人不存在。");
            }
        } // 如果没传 managerId，则不更新

        if (department.getAssistantManagerId() != null) { // 如果传入了 assistantManagerId
            if (StringUtils.isBlank(department.getAssistantManagerId())) {
                department.setAssistantManagerId(null); // 将空字符串转为 null
            } else if (employeeMapper.findByEmployee(department.getAssistantManagerId()) == null) {
                log.warn("更新部门失败：副经理 [{}] 不存在。", department.getAssistantManagerId());
                throw new IllegalArgumentException("指定的副经理不存在。");
            }
        } // 如果没传 assistantManagerId，则不更新

        // 5. 执行更新 (Mapper XML 中的 update 语句已包含更新 dep_name 的逻辑)
        //    不需要手动设置 updatedAt，Mapper XML 使用 CURRENT_TIMESTAMP
        try {
            log.info("准备更新部门 ID [{}]: {}", department.getId(), department);
            return departmentMapper.update(department);
        } catch (Exception e) {
            log.error("更新部门 ID [{}] 时数据库操作出错: {}, error={}", department.getId(), department, e.getMessage(), e);
            throw new RuntimeException("更新部门时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int deleteDepartment(Integer id) {
        if (id == null) {
            log.warn("删除部门失败：ID 为空。");
            return 0;
        }

        // 检查部门是否存在
        Department existing = departmentMapper.findById(id);
        if (existing == null) {
            log.warn("删除部门失败：部门 ID [{}] 不存在。", id);
            return 0;
        }

        // 检查是否有员工关联到此部门
        long employeeCount = employeeMapper.countAll(Map.of("depId", id));
        if (employeeCount > 0) {
            log.warn("删除部门失败：部门 ID [{}] 下存在 {} 名员工。", id, employeeCount);
            throw new IllegalStateException("无法删除部门：该部门下仍有关联员工。");
        }

        try {
            log.info("准备删除部门: ID={}", id);
            return departmentMapper.delete(id);
        } catch (Exception e) {
            log.error("删除部门时数据库操作出错: ID={}, error={}", id, e.getMessage(), e);
            throw new RuntimeException("删除部门时发生数据库错误。", e);
        }
    }
}

// **修改说明:**
// 在 `updateDepartment` 方法中：
// 增加了对 `department` 存在的检查。
// 增加了对 `depName` 是否被修改的判断 (`nameChanged`)。
// 如果 `depName` 被修改，则调用 `departmentMapper.findByName` 检查新名称是否已被**其他**部门使用，如果是则抛出 `IllegalStateException`。
// 对传入的 `managerId` 和 `assistantManagerId` 进行了处理：如果传入的是空字符串，则将其设为 `null`（表示清除负责人/副经理）；如果传入的是非空字符串，则验证对应的员工是否存在。如果根本没有传入这两个字段（即 `department` 对象中这两个属性为 `null`），则不会进行更新或验证（由 Mapper XML 中的 `<if>` 控制）。
// 添加了对输入字段的 XSS