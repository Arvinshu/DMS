package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO 用于封装员工工时明细表格中的单行数据。
 * 文件路径: src/main/java/org/ls/dto/EmployeeTimesheetRow.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTimesheetRow {

    /**
     * 员工信息 (主键，格式 "工号-姓名")。
     */
    private String employee;

    /**
     * 员工所属部门 ID。
     * 新增字段，用于在 Service 层按部门聚合。
     */
    private Integer depId;

    /**
     * 总工时数 (单位：人天)。
     * 使用 BigDecimal 保证精度。
     */
    private BigDecimal totalWorkdays;

    /**
     * 非项目工时数 (单位：人天)。
     * 使用 BigDecimal 保证精度。
     */
    private BigDecimal nonProjectWorkdays;

    /**
     * 项目工时数 (单位：人天)。
     * (可以通过 totalWorkdays - nonProjectWorkdays 计算得到)
     * 使用 BigDecimal 保证精度。
     */
    private BigDecimal projectWorkdays;

    /**
     * 项目工时率 (百分比，例如 85.50 表示 85.50%)。
     * 使用 BigDecimal 保证精度。
     */
    private BigDecimal projectWorkdayRate;

    /**
     * 员工参与的项目数量 (去重统计)。
     */
    private Long projectCount;

}
