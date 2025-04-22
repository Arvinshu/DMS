package org.ls.service;

import org.ls.dto.BatchInsertResult;
import org.ls.entity.TimesheetWork;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 工时记录服务接口
 * 定义工时主表 (t_wkt) 相关的业务逻辑操作。
 * 文件路径: src/main/java/org/ls/service/TimesheetService.java
 * 修正：适配新的四列组合主键 (ts_id, employee, ts_date, ts_bm)。
 */
public interface TimesheetService {

    /**
     * 根据复合主键查询单个工时记录。
     *
     * @param tsId     工时申请单号
     * @param employee 员工信息
     * @param tsDate   工时日期
     * @param tsBm     工时编码 (新增的主键部分)
     * @return TimesheetWork 实体，如果未找到则返回 null。
     */
    TimesheetWork findTimesheetById(String tsId, String employee, LocalDate tsDate, String tsBm); // 新增 tsBm 参数

    /**
     * 查询用于工时统计的数据列表（包含特定字段）。
     *
     * @param params 包含过滤条件和分页参数 (offset, limit) 的 Map。
     * @return 包含工时统计数据的 Map 列表。
     */
    List<Map<String, Object>> findTimesheetStatistics(Map<String, Object> params);

    /**
     * 统计符合条件的工时统计记录总数。
     *
     * @param params 包含过滤条件的 Map。
     * @return 记录总数。
     */
    long countTimesheetStatistics(Map<String, Object> params);

    /**
     * 查询所有工时记录（分页和可选过滤）。
     *
     * @param params 包含过滤条件和分页参数 (offset, limit) 的 Map。
     * @return TimesheetWork 实体列表。
     */
    List<TimesheetWork> findAllTimesheets(Map<String, Object> params);

    /**
     * 统计所有工时记录的总数（可能需要过滤）。
     *
     * @param params 包含过滤条件的 Map。
     * @return 记录总数。
     */
    long countAllTimesheets(Map<String, Object> params);

    /**
     * 添加一条新的工时记录。
     *
     * @param timesheetWork 要添加的工时记录实体。
     * @return 返回操作影响的行数。
     */
    int addTimesheet(TimesheetWork timesheetWork);

    /**
     * 更新现有的工时记录 (方法签名不变，实现需要传入包含所有主键的对象)
     *
     * @param timesheetWork 包含更新信息和复合主键的工时记录实体。
     * @return 返回操作影响的行数。
     */
    int updateTimesheet(TimesheetWork timesheetWork);

    /**
     * 根据复合主键删除工时记录。
     *
     * @param tsId     工时申请单号
     * @param employee 员工信息
     * @param tsDate   工时日期
     * @param tsBm     工时编码 (新增的主键部分)
     * @return 返回操作影响的行数。
     */
    int deleteTimesheet(String tsId, String employee, LocalDate tsDate, String tsBm); // 新增 tsBm 参数

    // --- 用于数据同步准备的方法 ---

    List<String> getDistinctDepartments();

    List<String> getDistinctEmployees();

    List<Map<String, String>> getDistinctTimesheetCodes();

    List<String> getDistinctProfitCenters();

    // --- 新增批量导入方法 ---

    /**
     * 批量添加工时记录。
     * 逐条尝试插入，并返回每条记录的处理结果。
     *
     * @param timesheets 要导入的工时记录列表。
     * @return 包含每条记录导入结果的列表。
     */
    List<BatchInsertResult> batchAddTimesheets(List<TimesheetWork> timesheets);
}
