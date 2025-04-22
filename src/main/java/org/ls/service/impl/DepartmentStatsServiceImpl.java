package org.ls.service.impl;

// --- 添加必要的 Import 语句 ---
import org.ls.dto.*;
import org.ls.entity.Department;
import org.ls.entity.Employee;
import org.ls.entity.TimesheetCode;
import org.ls.mapper.*;
import org.ls.service.DepartmentStatsService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
// --- 结束 Import 语句 ---

/**
 * 部门工作统计服务实现类。(完整修正版)
 * 文件路径: src/main/java/org/ls/service/impl/DepartmentStatsServiceImpl.java
 * 修正：添加 import，修正字符串格式化，确保 depId 可用。
 */
@Service
public class DepartmentStatsServiceImpl implements DepartmentStatsService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentStatsServiceImpl.class);

    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final TimesheetWorkMapper timesheetWorkMapper;
    private final TimesheetCodeMapper timesheetCodeMapper;
    private final ProfitCenterMapper profitCenterMapper;

    private static final BigDecimal HOURS_PER_WORKDAY = new BigDecimal("8.0");
    private static final int KEY_PROJECT_MIN_HOURS = 240;

    @Autowired
    public DepartmentStatsServiceImpl(DepartmentMapper departmentMapper,
                                      EmployeeMapper employeeMapper,
                                      TimesheetWorkMapper timesheetWorkMapper,
                                      TimesheetCodeMapper timesheetCodeMapper,
                                      ProfitCenterMapper profitCenterMapper) {
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.timesheetWorkMapper = timesheetWorkMapper;
        this.timesheetCodeMapper = timesheetCodeMapper;
        this.profitCenterMapper = profitCenterMapper;
    }

    @Override
    public CommonStatsInfo getCommonStatsInfo(LocalDate startDate, LocalDate endDate) {
        log.info("获取公共统计信息，时间范围: {} 到 {}", startDate, endDate);
        CommonStatsInfo info = new CommonStatsInfo();
        // 处理默认日期（如果 Controller 未处理）
        LocalDate finalStartDate = (startDate == null) ? LocalDate.now().withDayOfYear(1) : startDate;
        LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;

        info.setStartDate(finalStartDate);
        info.setEndDate(finalEndDate);

        try {
            Map<String, Object> headInfo = departmentMapper.findPrimaryDepartmentHead();
            if (headInfo != null && headInfo.get("managername") != null) { // 注意 key 是小写
                info.setPrimaryDepartmentHeadName((String) headInfo.get("managername"));
            } else {
                log.warn("未能找到一级部门或其负责人信息。");
                info.setPrimaryDepartmentHeadName("未指定");
            }

            info.setTotalEmployeeCount(employeeMapper.countActiveStatsEmployees());

            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            info.setCurrentWeekOfYear(finalEndDate.get(weekFields.weekOfWeekBasedYear()));

        } catch (Exception e) {
            log.error("获取公共统计信息时出错: {}", e.getMessage(), e);
        }
        log.debug("返回公共信息: {}", info);
        return info;
    }

    @Override
    public List<EmployeeTimesheetRow> getEmployeeTimesheetDetails(LocalDate startDate, LocalDate endDate) {
        log.info("获取员工工时明细，时间范围: {} 到 {}", startDate, endDate);
        LocalDate finalStartDate = (startDate == null) ? LocalDate.now().withDayOfYear(1) : startDate;
        LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;
        try {
            // 调用 Mapper 获取数据 (Mapper SQL 已包含 depId)
            List<EmployeeTimesheetRow> details = employeeMapper.findEmployeeTimesheetStats(finalStartDate, finalEndDate);
            log.info("成功获取 {} 条员工工时明细记录。", details.size());
            return details;
        } catch (Exception e) {
            log.error("获取员工工时明细时出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DepartmentOverviewRow> getDepartmentOverview(LocalDate startDate, LocalDate endDate) {
        log.info("获取部门整体情况统计，时间范围: {} 到 {}", startDate, endDate);
        List<DepartmentOverviewRow> overviewList = new ArrayList<>();
        LocalDate finalStartDate = (startDate == null) ? LocalDate.now().withDayOfYear(1) : startDate;
        LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;

        try {
            List<Department> activeDepts = departmentMapper.findActiveStatsDepartmentsSorted();
            if (activeDepts.isEmpty()) return overviewList;
            log.debug("获取到 {} 个活动且参与统计的部门。", activeDepts.size());

            // 获取员工工时统计 (现在包含 depId)
            List<EmployeeTimesheetRow> employeeStats = getEmployeeTimesheetDetails(finalStartDate, finalEndDate);
            // 按部门 ID 分组员工统计数据
            Map<Integer, List<EmployeeTimesheetRow>> statsByDept = employeeStats.stream()
                    .filter(stat -> stat.getDepId() != null)
                    .collect(Collectors.groupingBy(EmployeeTimesheetRow::getDepId)); // 使用修正后的 getDepId()
            log.debug("员工工时按部门分组完成，共 {} 个部门有数据。", statsByDept.size());

            // 获取所有活动员工列表（按部门分组）
            Map<Integer, List<String>> employeesByDeptId = new HashMap<>();
            Map<Integer, Long> employeeCountByDeptId = new HashMap<>();
            List<Employee> allActiveStatsEmployees = employeeMapper.findAll(Map.of("active", true, "isStatistics", true));
            allActiveStatsEmployees.forEach(emp -> {
                if(emp.getDepId() != null) {
                    employeesByDeptId.computeIfAbsent(emp.getDepId(), k -> new ArrayList<>()).add(emp.getEmployee());
                    employeeCountByDeptId.merge(emp.getDepId(), 1L, Long::sum);
                }
            });
            log.debug("获取到所有活动且参与统计的员工信息并按部门分组。");

            // 获取一级部门负责人信息，用于显示负责人姓名
            Map<String, Object> primaryHeadInfo = departmentMapper.findPrimaryDepartmentHead();
            String primaryManagerName = primaryHeadInfo != null ? (String) primaryHeadInfo.get("managername") : null;


            for (Department dept : activeDepts) {
                DepartmentOverviewRow row = new DepartmentOverviewRow();
                row.setDepartmentId(dept.getId());
                row.setDepartmentName(dept.getDepName());
                row.setDepartmentLevel(dept.getDepLevel());

                // 设置负责人显示 (修正字符串格式化)
                if (dept.getManagerId() != null) {
                    // 尝试从员工列表或一级部门负责人信息中获取姓名
                    String managerName = null;
                    if (Objects.equals(dept.getDepLevel(), "一级部门") && primaryManagerName != null) {
                        managerName = primaryManagerName;
                    } else {
                        // 尝试从已加载的员工信息中查找（效率较低，或者修改 Mapper 返回负责人姓名）
                        Optional<Employee> managerOpt = allActiveStatsEmployees.stream()
                                .filter(e -> Objects.equals(e.getEmployee(), dept.getManagerId()))
                                .findFirst();
                        if (managerOpt.isPresent()) {
                            managerName = managerOpt.get().getEmployeeName();
                        }
                    }
                    // 使用 String.format 或 + 进行拼接
                    row.setManagerDisplay(String.format("%s (%s)", managerName != null ? managerName : "?", dept.getManagerId()));
                } else {
                    row.setManagerDisplay("-");
                }

                row.setEmployeeCount(employeeCountByDeptId.getOrDefault(dept.getId(), 0L));
                row.setEmployeeList(employeesByDeptId.getOrDefault(dept.getId(), Collections.emptyList()));

                List<EmployeeTimesheetRow> deptEmployeeStats = statsByDept.getOrDefault(dept.getId(), Collections.emptyList());

                // 使用 BigDecimal 进行聚合计算
                BigDecimal totalWorkdays = deptEmployeeStats.stream().map(EmployeeTimesheetRow::getTotalWorkdays).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal nonProjectWorkdays = deptEmployeeStats.stream().map(EmployeeTimesheetRow::getNonProjectWorkdays).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal projectWorkdays = totalWorkdays.subtract(nonProjectWorkdays);
                BigDecimal projectRate = BigDecimal.ZERO;
                if (totalWorkdays.compareTo(BigDecimal.ZERO) > 0) {
                    projectRate = projectWorkdays.multiply(new BigDecimal("100")).divide(totalWorkdays, 2, RoundingMode.HALF_UP);
                }

                row.setTotalWorkdays(totalWorkdays);
                row.setNonProjectWorkdays(nonProjectWorkdays);
                row.setProjectWorkdays(projectWorkdays);
                row.setProjectWorkdayRate(projectRate);

                overviewList.add(row);
            }
            log.info("部门整体情况统计完成，共 {} 条记录。", overviewList.size());

        } catch (Exception e) {
            log.error("获取部门整体情况统计时出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
        return overviewList;
    }

    @Override
    public List<KeyProjectRow> getKeyProjectStats(LocalDate startDate, LocalDate endDate) {
        log.info("获取重点项目统计，时间范围: {} 到 {}", startDate, endDate);
        List<KeyProjectRow> keyProjectList = new ArrayList<>();
        LocalDate finalStartDate = (startDate == null) ? LocalDate.now().withDayOfYear(1) : startDate;
        LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;

        try {
            List<Map<String, Object>> projects = timesheetWorkMapper.findKeyProjectsAboveThreshold(finalStartDate, finalEndDate, KEY_PROJECT_MIN_HOURS);
            log.debug("发现 {} 个重点项目。", projects.size());
            if (projects.isEmpty()) return keyProjectList;

            for (Map<String, Object> project : projects) {
                String tsBm = (String) project.get("ts_bm");
                String customProjectName = (String) project.get("custom_project_name");
                // total_hours 可能返回 Long 或 BigDecimal，取决于数据库和驱动
                Number totalHoursNum = (Number) project.get("total_hours");
                BigDecimal totalHours = (totalHoursNum != null) ? new BigDecimal(totalHoursNum.toString()) : BigDecimal.ZERO;

                if (tsBm == null) continue;

                KeyProjectRow row = new KeyProjectRow();
                row.setProjectCode(tsBm);
                // 如果 custom_project_name 为空，尝试从 t_timesheet_code 获取 ts_name
                if (StringUtils.isBlank(customProjectName)) {
                    TimesheetCode code = timesheetCodeMapper.findByTsBm(tsBm);
                    row.setProjectName(code != null ? code.getTsName() : "(名称未找到)");
                } else {
                    row.setProjectName(customProjectName);
                }
                row.setTotalWorkdays(totalHours.divide(HOURS_PER_WORKDAY, 2, RoundingMode.HALF_UP));

                List<Map<String, Object>> employeesData = timesheetWorkMapper.findEmployeesForProjectByDept(tsBm, finalStartDate, finalEndDate);
                Map<String, List<String>> employeesByDeptName = new HashMap<>();
                for (Map<String, Object> empData : employeesData) {
                    String depName = (String) empData.get("dep_name");
                    String employeeListStr = (String) empData.get("employee_list");
                    if (depName != null && employeeListStr != null) {
                        employeesByDeptName.put(depName, Arrays.asList(employeeListStr.split(", ")));
                    }
                }
                // 只添加参与部门
                row.setEmployeesByDepartment(employeesByDeptName);
                keyProjectList.add(row);
            }
            log.info("重点项目统计完成，共 {} 个项目。", keyProjectList.size());

        } catch (Exception e) {
            log.error("获取重点项目统计时出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
        return keyProjectList;
    }

    @Override
    public ProfitCenterPivotTable getProfitCenterStats(LocalDate startDate, LocalDate endDate) {
        log.info("获取利润中心交叉表统计，时间范围: {} 到 {}", startDate, endDate);
        ProfitCenterPivotTable pivotTable = new ProfitCenterPivotTable(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        LocalDate finalStartDate = (startDate == null) ? LocalDate.now().withDayOfYear(1) : startDate;
        LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;

        try {
            List<Department> activeDepts = departmentMapper.findActiveStatsDepartmentsSorted();
            List<String> departmentHeaders = activeDepts.stream().map(Department::getDepName).collect(Collectors.toList());
            Map<Integer, String> depIdToNameMap = activeDepts.stream().collect(Collectors.toMap(Department::getId, Department::getDepName));
            pivotTable.setDepartmentHeaders(departmentHeaders);
            log.debug("获取到 {} 个部门作为交叉表列头。", departmentHeaders.size());

            List<Map<String, Object>> distinctRemarks = profitCenterMapper.findDistinctRemarksSorted();
            log.debug("获取到 {} 个不同的利润中心备注作为交叉表行。", distinctRemarks.size());

            List<Map<String, Object>> rawData = timesheetWorkMapper.findProfitCenterWorkdaysByDept(finalStartDate, finalEndDate);
            log.debug("获取到 {} 条按利润中心和部门分组的工时数据。", rawData.size());

            Map<String, Map<String, BigDecimal>> pivotDataMap = new HashMap<>();
            for (Map<String, Object> record : rawData) {
                String remark = (String) record.get("custom_zone_remark");
                Integer depId = (Integer) record.get("dep_id");
                String depName = depIdToNameMap.get(depId);
                // total_workdays 可能返回 Double 或 BigDecimal
                Number workdaysNum = (Number) record.get("total_workdays");
                BigDecimal workdays = (workdaysNum != null) ? new BigDecimal(workdaysNum.toString()) : BigDecimal.ZERO;

                if (remark != null && depName != null) {
                    pivotDataMap.computeIfAbsent(remark, k -> new HashMap<>()).put(depName, workdays);
                }
            }

            int seq = 1;
            for (Map<String, Object> remarkInfo : distinctRemarks) {
                String remark = (String) remarkInfo.get("custom_zone_remark");
                ProfitCenterRow row = new ProfitCenterRow();
                row.setSequenceNumber(seq++);
                row.setProfitCenterRemark(remark);
                // row.setRegionCategory((String) remarkInfo.get("region_category"));
                // row.setRegionName((String) remarkInfo.get("region_name"));

                Map<String, BigDecimal> workdaysMap = new HashMap<>();
                Map<String, BigDecimal> remarkData = pivotDataMap.getOrDefault(remark, Collections.emptyMap());
                for (String header : departmentHeaders) {
                    workdaysMap.put(header, remarkData.getOrDefault(header, BigDecimal.ZERO));
                }
                row.setWorkdaysByDepartment(workdaysMap);
                pivotTable.getRows().add(row);
            }

            Map<String, BigDecimal> departmentTotals = new HashMap<>();
            for(String header : departmentHeaders) {
                BigDecimal total = pivotTable.getRows().stream()
                        .map(row -> row.getWorkdaysByDepartment().getOrDefault(header, BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                departmentTotals.put(header, total.setScale(2, RoundingMode.HALF_UP)); // 保留两位小数
            }
            pivotTable.setDepartmentTotals(departmentTotals);
            log.info("利润中心交叉表构建完成。");

        } catch (Exception e) {
            log.error("获取利润中心交叉表时出错: {}", e.getMessage(), e);
            return new ProfitCenterPivotTable(Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        }
        return pivotTable;
    }

    // --- 新增方法的实现 ---

    /**
     * 获取按月统计的项目数量。
     */
    @Override
    public List<MonthlyProjectCount> getMonthlyProjectCounts(LocalDate startDate, LocalDate endDate) {
        log.info("获取月度项目数量统计，时间范围: {} 到 {}", startDate, endDate);
        LocalDate finalStartDate = (startDate == null) ? LocalDate.now().withDayOfYear(1) : startDate;
        LocalDate finalEndDate = (endDate == null) ? LocalDate.now() : endDate;

        try {
            List<MonthlyProjectCount> counts = timesheetWorkMapper.findMonthlyProjectCounts(finalStartDate, finalEndDate);
            log.info("成功获取 {} 条月度项目统计记录。", counts.size());
            // 可以在这里补充缺失月份的数据为 0，如果需要的话
            Map<Integer, Long> countsMap = counts.stream()
                    .collect(Collectors.toMap(MonthlyProjectCount::getMonth, MonthlyProjectCount::getProjectCount));
            List<MonthlyProjectCount> fullYearCounts = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                fullYearCounts.add(new MonthlyProjectCount(month, countsMap.getOrDefault(month, 0L)));
            }
            return fullYearCounts; // 返回包含 12 个月数据的列表
        } catch (Exception e) {
            log.error("获取月度项目数量统计时出错: {}", e.getMessage(), e);
            return Collections.emptyList(); // 返回空列表表示出错
        }
    }
}
//
//        * **修改说明:**
//        * **添加了所有必要的 `import` 语句。**
//        * **修正了字符串格式化:** 将 `getDepartmentOverview` 中错误的 `` `${...}` `` 替换为了 `String.format("%s (%s)", ...)`。
//        * **处理了 `getDepId()` 错误:**
//        * 在 `getDepartmentOverview` 中，确认了 `employeeStats` 列表（来自 `getEmployeeTimesheetDetails`）现在应该包含 `depId`（因为我们修改了 DTO 和 Mapper SQL）。
//        * 使用 `EmployeeTimesheetRow::getDepId` 进行分组。
//        * **默认日期处理:** 在每个方法开头添加了逻辑，如果传入的 `startDate` 或 `endDate` 为 `null`，则使用默认值（年初/今天）。
//        * **数字类型处理:** 在处理 Mapper 返回的聚合结果（如 `total_hours`, `total_workdays`）时，使用了更健壮的 `Number` 类型接收，并转换为 `BigDecimal` 进行后续计算，以避免潜在的类型转换错误。
//        * **空值处理:** 在聚合和计算中添加了更多的 `Objects::nonNull` 检查或 `getOrDefault` 来处理可能出现的 `null` 值。
//        * **BigDecimal 精度:** 在计算比率和汇总时，明确使用 `setScale(2, RoundingMode.HALF_UP)` 来设置小数位数和舍入模式。
//        * **重点项目名称:** 在 `getKeyProjectStats` 中增加了逻辑：如果 `custom_project_name` 为空，则尝试从 `t_timesheet_code` 表中查找 `ts_name` 作为备用。
//        * **代码健壮性:** 增加了对 Mapper 返回结果和 DTO 属性的空值
