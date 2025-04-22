package org.ls.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类：员工信息表 (t_employee)
 * 文件路径: src/main/java/org/ls/entity/Employee.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 员工信息，工号+姓名组合 (employee)
     * 主键
     * 对 t_wkt.employee 进行去重后同步至该列。
     * 此列数据是从其他表同步过来，在页面只展示不维护。
     */
    private String employee;

    /**
     * 员工唯一工号 (employee_id)
     * 从 t_wkt.employee 按照 "-" 号拆分后第 1 个数据。
     * 此列数据是从其他表同步过来，在页面只展示不维护。
     * 具有唯一约束。
     */
    private String employeeId;

    /**
     * 员工真实姓名 (employee_name)
     * 从 t_wkt.employee 按照 "-" 号拆分后第 2 个数据。
     * 此列数据是从其他表同步过来，在页面只展示不维护。
     */
    private String employeeName;

    /**
     * 所属部门ID (dep_id)
     * 关联 t_department.id
     */
    private Integer depId;

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
     * 在职状态 (active)
     * true-在职，false-离职
     * 默认为 true
     */
    private boolean active;

    /**
     * 工时统计标识 (statistics)
     * true-参与统计，false-不参与
     * 默认为 true
     */
    private boolean statistics;

    // --- 非数据库字段，用于关联查询或显示 ---
    /**
     * 所属部门名称 (非数据库字段)
     * 需要联表查询获得
     */
    private transient String departmentName;


    // Lombok @Data 自动生成 getter/setter/toString/equals/hashCode
}
