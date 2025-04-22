package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO 用于封装利润中心交叉表中的单行数据。
 * 文件路径: src/main/java/org/ls/dto/ProfitCenterRow.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitCenterRow {

    /**
     * 行序号。
     */
    private int sequenceNumber;

    /**
     * 利润中心备注 (行头显示)。
     */
    private String profitCenterRemark;

    // 可以选择性地包含用于排序的字段
    // private String regionCategory;
    // private String regionName;

    /**
     * 存储该利润中心下各部门的工时数据。
     * Key: 部门名称 (String) - 与 ProfitCenterPivotTable 中的 departmentHeaders 对应
     * Value: 该部门在该利润中心投入的总人天数 (BigDecimal)
     */
    private Map<String, BigDecimal> workdaysByDepartment;

}
