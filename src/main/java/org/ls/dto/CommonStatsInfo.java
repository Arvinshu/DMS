package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO 用于封装部门工作统计页面顶部的公共信息区数据。
 * 文件路径: src/main/java/org/ls/dto/CommonStatsInfo.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonStatsInfo {

    /**
     * 一级部门负责人姓名。
     * (如果找不到或有多个，可能为 null 或特定提示)
     */
    private String primaryDepartmentHeadName;

    /**
     * 部门总在职且参与统计的员工人数。
     */
    private Long totalEmployeeCount;

    /**
     * 统计区间的开始日期。
     */
    private LocalDate startDate;

    /**
     * 统计区间的结束日期。
     */
    private LocalDate endDate;

    /**
     * 统计结束日期所在的年份周数。
     */
    private Integer currentWeekOfYear;

    // 可以根据需要添加其他公共信息字段
    // private String departmentStructureSummary; // 例如部门结构摘要
}
