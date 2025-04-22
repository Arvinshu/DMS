package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO 用于封装数据同步前各表的数量统计信息。
 * 文件路径: src/main/java/org/ls/dto/SyncCounts.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncCounts {

    // 源表 (t_wkt) 中的不同维度数据量
    private long sourceDepartmentCount;   // t_wkt 中不重复的部门名称数量
    private long sourceEmployeeCount;     // t_wkt 中不重复的员工信息数量
    private long sourceTimesheetCodeCount;// t_wkt 中不重复的工时编码数量
    private long sourceProfitCenterCount; // t_wkt 中不重复的利润中心数量

    // 目标维表中的当前数据量
    private long targetDepartmentCount;   // t_department 表的总记录数
    private long targetEmployeeCount;     // t_employee 表的总记录数
    private long targetTimesheetCodeCount;// t_timesheet_code 表的总记录数
    private long targetProfitCenterCount; // t_profit_center 表的总记录数

}


//        * **说明:**
//        * 使用 Lombok 简化代码。
//        * 包含了源表 (`t_wkt`) 中不同维度（部门、员工、工时编码、利润中心）的**唯一值数量**。
//        * 包含了目标维表中当前的**总记录数量**。
//        * 前端可以使用这些数据来展示同步前的