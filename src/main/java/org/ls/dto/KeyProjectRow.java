package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO 用于封装二级部门重点工作统计表格中的单行数据。
 * 文件路径: src/main/java/org/ls/dto/KeyProjectRow.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyProjectRow {

    /**
     * 项目工时编码 (ts_bm)。
     */
    private String projectCode;

    /**
     * 项目名称 (来自 t_timesheet_code.custom_project_name)。
     */
    private String projectName;

    /**
     * 累计投入人天数。
     */
    private BigDecimal totalWorkdays;

    /**
     * 动态存储各部门参与人员列表。
     * Key: 部门名称 (String)
     * Value: 参与该项目的该部门员工列表 (List<String>, 格式 "工号-姓名")
     * 只包含实际参与了该项目的部门。
     */
    private Map<String, List<String>> employeesByDepartment;

}
