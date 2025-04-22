package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO 用于封装整个利润中心交叉表的数据结构，包括表头、行数据和汇总行。
 * 文件路径: src/main/java/org/ls/dto/ProfitCenterPivotTable.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitCenterPivotTable {

    /**
     * 动态生成的部门列标题列表 (按部门 ID 排序的部门名称)。
     */
    private List<String> departmentHeaders;

    /**
     * 表格的行数据列表。
     */
    private List<ProfitCenterRow> rows;

    /**
     * 汇总行数据。
     * Key: 部门名称 (String) - 与 departmentHeaders 对应
     * Value: 该部门所有利润中心工时的总和 (BigDecimal)
     */
    private Map<String, BigDecimal> departmentTotals;

}
