package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ls.entity.TimesheetWork;
import org.ls.dto.MonthlyProjectCount; // 导入新的 DTO

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Mapper Interface for t_wkt (工时主表)
 * 文件路径: src/main/java/org/ls/mapper/TimesheetWorkMapper.java
 * 修正：适配新的四列组合主键 (ts_id, employee, ts_date, ts_bm)。
 */
@Mapper
public interface TimesheetWorkMapper {

    /**
     * 根据复合主键查询工时记录
     *
     * @param tsId     工时申请单号
     * @param employee 员工信息
     * @param tsDate   工时日期
     * @param tsBm     工时编码 (新增的主键部分)
     * @return TimesheetWork 实体 或 null
     */
    TimesheetWork findById(@Param("tsId") String tsId,
                           @Param("employee") String employee,
                           @Param("tsDate") LocalDate tsDate,
                           @Param("tsBm") String tsBm); // 新增 tsBm 参数

    /**
     * 查询用于工时统计页面的数据 (选择特定字段)
     * 支持分页和过滤
     *
     * @param params 包含过滤条件和分页参数的 Map
     * @return Map 列表
     */
    List<Map<String, Object>> findTimesheetStatistics(Map<String, Object> params);

    /**
     * 统计符合条件的记录总数 (用于分页)
     *
     * @param params 包含过滤条件的 Map
     * @return 记录总数
     */
    long countTimesheetStatistics(Map<String, Object> params);

    /**
     * 根据员工信息字符串 (工号-姓名) 查询该员工最新一条工时记录中的部门名称。
     * 用于同步员工信息时确定员工归属的最新部门。
     * @param employee 员工信息字符串 (例如 "12345-张三")
     * @return 最新的部门名称，如果该员工没有工时记录则返回 null。
     */
    String findLatestDepByEmployee(@Param("employee") String employee);

    /**
     * 查询所有工时记录 (分页和可选过滤)
     *
     * @param params 包含过滤条件和分页参数的 Map
     * @return TimesheetWork 列表
     */
    List<TimesheetWork> findAll(Map<String, Object> params);

    /**
     * 统计所有记录总数 (用于分页)
     *
     * @param params 包含过滤条件的 Map
     * @return 记录总数
     */
    long countAll(Map<String, Object> params);


    // --- 用于同步维度表的方法 ---

    List<String> findDistinctDepartments();
    List<String> findDistinctEmployees();
    List<Map<String, String>> findDistinctTimesheetCodes();
    List<String> findDistinctProfitCenters();

    // --- CUD 操作 ---

    /**
     * 插入新的工时记录
     *
     * @param timesheetWork TimesheetWork 实体
     * @return 影响的行数
     */
    int insert(TimesheetWork timesheetWork);

    /**
     * 更新工时记录 (方法签名不变，WHERE 条件依赖对象中的所有主键字段)
     *
     * @param timesheetWork TimesheetWork 实体 (必须包含所有四个主键字段)
     * @return 影响的行数
     */
    int update(TimesheetWork timesheetWork);

    /**
     * 根据复合主键删除工时记录
     *
     * @param tsId     工时申请单号
     * @param employee 员工信息
     * @param tsDate   工时日期
     * @param tsBm     工时编码 (新增的主键部分)
     * @return 影响的行数
     */
    int delete(@Param("tsId") String tsId,
               @Param("employee") String employee,
               @Param("tsDate") LocalDate tsDate,
               @Param("tsBm") String tsBm); // 新增 tsBm 参数

    /**
     * 查询在指定时间范围内，累计工时超过阈值的项目工时信息。
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param minHours 最小工时数阈值
     * @return 包含项目编码(ts_bm)、项目名称(custom_project_name)、总工时(total_hours) 的 Map 列表。
     */
    List<Map<String, Object>> findKeyProjectsAboveThreshold(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minHours") int minHours);

    /**
     * 查询指定项目在指定时间范围内，按部门分组的参与员工列表。
     * @param tsBm 项目工时编码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含部门ID(dep_id)、部门名称(dep_name)、员工列表(employee_list - 逗号分隔) 的 Map 列表。
     */
    List<Map<String, Object>> findEmployeesForProjectByDept(
            @Param("tsBm") String tsBm,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询按利润中心和部门分组的项目工时（人天）。
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 包含利润中心备注(custom_zone_remark)、区域分类(region_category)、大区名称(region_name)、
     * 部门ID(dep_id)、部门名称(dep_name)、总人天数(total_workdays) 的 Map 列表。
     */
    List<Map<String, Object>> findProfitCenterWorkdaysByDept(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // --- 新增方法 (用于数据统计 - 图表) ---

    /**
     * 查询指定时间范围内，每个月参与的项目数量（按项目 ts_bm 去重）。
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 月份和对应项目数的 DTO 列表。
     */
    List<MonthlyProjectCount> findMonthlyProjectCounts(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

}
