package org.ls.service;

import org.ls.dto.SyncCounts;
import org.ls.dto.SyncResult;

/**
 * 数据同步服务接口。
 * 定义了获取同步统计信息和执行各维度数据同步的方法。
 * 文件路径: src/main/java/org/ls/service/SyncService.java
 */
public interface SyncService {

    /**
     * 获取当前源表和目标表的记录数量统计信息。
     * 用于在同步页面初始化时显示。
     *
     * @return SyncCounts 包含各项统计数量的 DTO。
     */
    SyncCounts getSyncCounts();

    /**
     * 执行部门数据同步。
     * 从 t_wkt 同步新的部门到 t_department。
     *
     * @return SyncResult 包含本次同步操作的结果（新增数量等）。
     */
    SyncResult syncDepartments();

    /**
     * 执行员工数据同步。
     * 从 t_wkt 同步新的员工到 t_employee，并根据规则查找部门 ID。
     * 建议在部门同步完成后执行。
     *
     * @return SyncResult 包含本次同步操作的结果。
     */
    SyncResult syncEmployees();

    /**
     * 执行工时编码数据同步。
     * 从 t_wkt 同步新的工时编码到 t_timesheet_code。
     *
     * @return SyncResult 包含本次同步操作的结果。
     */
    SyncResult syncTimesheetCodes();

    /**
     * 执行利润中心数据同步。
     * 从 t_wkt 同步新的利润中心到 t_profit_center，并解析 zone 字符串。
     *
     * @return SyncResult 包含本次同步操作的结果。
     */
    SyncResult syncProfitCenters();

}


//        * **说明:**
//        * 定义了获取初始数量 (`getSyncCounts`) 和触发四种数据同步操作的方法签名。
//        * 同步方法的返回值是 `SyncResult` D
