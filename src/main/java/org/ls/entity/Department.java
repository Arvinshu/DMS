package org.ls.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类：部门信息表 (t_department)
 * 文件路径: src/main/java/org/ls/entity/Department.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门唯一标识，需手工维护的数字编号 (id)
     * 主键
     */
    private Integer id;

    /**
     * 部门名称 (dep_name)
     * 对 t_wkt.dep 进行去重后同步至该列。
     * 此列数据是从其他表同步过来，在页面只展示不维护。
     * 具有唯一约束。
     */
    private String depName;

    /**
     * 部门层级 (dep_level)
     * 例如：一级部门/二级部门/三级部门
     */
    private String depLevel;

    /**
     * 部门负责人ID (manager_id)
     * 关联 t_employee.employee
     */
    private String managerId;

    /**
     * 部门副经理ID (assistant_manager_id)
     * 关联 t_employee.employee
     */
    private String assistantManagerId;

    /**
     * 记录创建时间 (created_at)
     * 数据库自动生成默认值
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间 (updated_at)
     * 需应用程序或触发器维护
     */
    private LocalDateTime updatedAt;

    /**
     * 启用状态 (active)
     * true-启用，false-停用
     * 默认为 true
     */
    private boolean active;

    /**
     * 是否参与统计 (statistics)
     * true-参与，false-不参与
     * 默认为 true
     */
    private boolean statistics;

    // --- 非数据库字段，用于关联查询或显示 ---
    /**
     * 部门负责人姓名 (非数据库字段)
     * 需要联表查询获得
     */
    private transient String managerName;

    /**
     * 部门副经理姓名 (非数据库字段)
     * 需要联表查询获得
     */
    private transient String assistantManagerName;


    // Lombok @Data 自动生成 getter/setter/toString/equals/hashCode
}
