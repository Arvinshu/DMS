package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO 用于封装部门整体情况统计表格中的单行数据。
 * 文件路径: src/main/java/org/ls/dto/DepartmentOverviewRow.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentOverviewRow {

    /**
     * 部门序号 (即部门 ID)。
     */
    private Integer departmentId;

    /**
     * 部门名称。
     */
    private String departmentName;

    /**
     * 部门层级。
     */
    private String departmentLevel;

    /**
     * 部门负责人 (格式 "姓名(工号-姓名)")。
     * 可能为 null。
     */
    private String managerDisplay; // 组合显示负责人信息

    /**
     * 部门人数 (在职且参与统计)。
     */
    private Long employeeCount;

    /**
     * 部门成员列表 (格式 "工号-姓名" 的列表)。
     * 前端需要处理换行显示。
     */
    private List<String> employeeList;

    /**
     * 部门总工时数 (人天)。
     * 由该部门成员的工时聚合计算得到。
     */
    private BigDecimal totalWorkdays;

    /**
     * 部门非项目工时数 (人天)。
     * 由该部门成员的工时聚合计算得到。
     */
    private BigDecimal nonProjectWorkdays;

    /**
     * 部门项目工时数 (人天)。
     * 由该部门成员的工时聚合计算得到。
     */
    private BigDecimal projectWorkdays;

    /**
     * 部门项目工时率 (%)。
     * 由该部门成员的工时聚合计算得到。
     */
    private BigDecimal projectWorkdayRate;

}
