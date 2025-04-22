package org.ls.service.impl;

import org.ls.dto.SyncCounts;
import org.ls.dto.SyncResult;
import org.ls.entity.Department; // 引入实体
import org.ls.entity.Employee;
import org.ls.entity.ProfitCenter;
import org.ls.entity.TimesheetCode;
import org.ls.mapper.*;
import org.ls.service.SyncService;
import org.ls.utils.StringUtils; // 引入工具类
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException; // 引入数据访问异常
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 根据需要决定是否启用事务

import java.util.*; // 引入 Set, List, Map 等
import java.util.stream.Collectors;

/**
 * 数据同步服务实现类。
 * 文件路径: src/main/java/org/ls/service/impl/SyncServiceImpl.java
 */
@Service
public class SyncServiceImpl implements SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncServiceImpl.class);

    // 注入所有需要交互的 Mapper
    private final TimesheetWorkMapper timesheetWorkMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final TimesheetCodeMapper timesheetCodeMapper;
    private final BusinessTypeMapper businessTypeMapper; // 虽然不直接同步，但可能需要用于关联
    private final ProfitCenterMapper profitCenterMapper;

    @Autowired
    public SyncServiceImpl(TimesheetWorkMapper timesheetWorkMapper,
                           DepartmentMapper departmentMapper,
                           EmployeeMapper employeeMapper,
                           TimesheetCodeMapper timesheetCodeMapper,
                           BusinessTypeMapper businessTypeMapper,
                           ProfitCenterMapper profitCenterMapper) {
        this.timesheetWorkMapper = timesheetWorkMapper;
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.timesheetCodeMapper = timesheetCodeMapper;
        this.businessTypeMapper = businessTypeMapper;
        this.profitCenterMapper = profitCenterMapper;
    }

    /**
     * 获取当前源表和目标表的记录数量统计信息。
     */
    @Override
    public SyncCounts getSyncCounts() {
        log.info("开始获取数据同步数量统计...");
        SyncCounts counts = new SyncCounts();
        try {
            // 获取源表 (t_wkt) 中的唯一维度数量
            // 注意：findDistinct...().size() 在大数据量下可能效率不高，但作为初始实现可以接受
            counts.setSourceDepartmentCount(timesheetWorkMapper.findDistinctDepartments().size());
            counts.setSourceEmployeeCount(timesheetWorkMapper.findDistinctEmployees().size());
            counts.setSourceTimesheetCodeCount(timesheetWorkMapper.findDistinctTimesheetCodes().size());
            counts.setSourceProfitCenterCount(timesheetWorkMapper.findDistinctProfitCenters().size());
            log.debug("源表统计: Departments={}, Employees={}, TimesheetCodes={}, ProfitCenters={}",
                    counts.getSourceDepartmentCount(), counts.getSourceEmployeeCount(),
                    counts.getSourceTimesheetCodeCount(), counts.getSourceProfitCenterCount());

            // 获取目标维表的总记录数
            // 使用 Collections.emptyMap() 作为参数，因为 countAll 不需要特定过滤条件
            counts.setTargetDepartmentCount(departmentMapper.countAll(Collections.emptyMap()));
            counts.setTargetEmployeeCount(employeeMapper.countAll(Collections.emptyMap()));
            counts.setTargetTimesheetCodeCount(timesheetCodeMapper.countAll(Collections.emptyMap()));
            counts.setTargetProfitCenterCount(profitCenterMapper.countAll(Collections.emptyMap()));
            log.debug("目标表统计: Departments={}, Employees={}, TimesheetCodes={}, ProfitCenters={}",
                    counts.getTargetDepartmentCount(), counts.getTargetEmployeeCount(),
                    counts.getTargetTimesheetCodeCount(), counts.getTargetProfitCenterCount());

            log.info("数量统计获取完成。");
        } catch (Exception e) {
            log.error("获取同步数量统计时发生错误: {}", e.getMessage(), e);
            // 可以在这里设置默认值或部分值，或者让调用者处理 null/异常
            // 返回一个包含 0 的对象，避免前端出错
            return new SyncCounts(0, 0, 0, 0, 0, 0, 0, 0);
        }
        return counts;
    }

    /**
     * 执行部门数据同步。
     * 从 t_wkt 同步新的部门到 t_department。
     * ID = max(id) + 1, Level = "二级部门"
     */
    @Override
    @Transactional // 考虑是否需要整体事务，如果单条失败不影响其他，则不需要
    public SyncResult syncDepartments() {
        // 实现部门同步逻辑
        // 1. 获取 t_wkt 不重复部门列表: timesheetWorkMapper.findDistinctDepartments()
        // 2. 获取 t_department 现有部门名称列表: departmentMapper.findAll(...) 或创建一个新方法 findDepartmentNames()
        // 3. 比较差异，找出新部门名称
        // 4. 对于新部门名称：
        //    a. 获取当前最大 ID: departmentMapper.findMaxId()
        //    b. 创建 Department 对象 (id = maxId + 1, depName = newName, depLevel = "二级部门", 其他为 null)
        //    c. 调用 departmentMapper.insert()
        //    d. 记录成功/失败数量
        // 5. 返回 SyncResult

        log.info("开始执行部门数据同步...");
        long successCount = 0;
        long failureCount = 0; // 这里的失败通常指插入过程中的意外错误

        try {
            // 1. 获取源数据 (t_wkt 不重复部门名)
            List<String> sourceDepNames = timesheetWorkMapper.findDistinctDepartments();
            if (sourceDepNames.isEmpty()) {
                return new SyncResult(0, 0, "源数据中无部门信息，无需同步。");
            }
            log.debug("从 t_wkt 获取到 {} 个不重复部门名称。", sourceDepNames.size());

            // 2. 获取目标数据 (t_department 现有部门名)
            //    直接查询所有部门实体可能效率低，但可以获取名称集合
            List<Department> targetDepartments = departmentMapper.findAll(Collections.emptyMap());
            Set<String> targetDepNames = targetDepartments.stream()
                    .map(Department::getDepName)
                    .collect(Collectors.toSet());
            log.debug("从 t_department 获取到 {} 个现有部门。", targetDepNames.size());

            // 3. 找出需要新增的部门名称
            List<String> newDepNames = sourceDepNames.stream()
                    .filter(name -> !targetDepNames.contains(name))
                    .toList(); // 使用 Java 16+ 的 toList()

            if (newDepNames.isEmpty()) {
                log.info("没有新的部门需要同步。");
                return new SyncResult(0, 0, "数据已是最新，无需同步部门。");
            }
            log.info("发现 {} 个新部门需要同步: {}", newDepNames.size(), newDepNames);

            // 4. 获取当前最大 ID
            Integer maxId = departmentMapper.findMaxId();
            int nextId = (maxId == null ? 0 : maxId) + 1;
            log.info("当前最大部门 ID: {}, 下一个可用 ID: {}", maxId, nextId);

            // 5. 逐条插入新部门
            for (String newName : newDepNames) {
                try {
                    Department newDept = new Department();
                    newDept.setId(nextId++); // 设置新 ID 并自增
                    newDept.setDepName(newName);
                    newDept.setDepLevel("二级部门"); // 默认值
                    newDept.setManagerId(null); // 默认值
                    newDept.setAssistantManagerId(null); // 默认值
                    newDept.setActive(true); // 默认值
                    newDept.setStatistics(true); // 默认值
                    // createdAt 由数据库默认生成, updatedAt 为 NULL

                    departmentMapper.insert(newDept);
                    successCount++;
                    log.debug("成功插入新部门: ID={}, Name={}", newDept.getId(), newDept.getDepName());
                } catch (DataAccessException e) {
                    // 捕获数据库访问异常 (包括可能的约束冲突，虽然理论上不会发生)
                    failureCount++;
                    log.error("插入新部门 [{}] 时发生数据库错误: {}", newName, e.getMessage(), e);
                } catch (Exception e) {
                    failureCount++;
                    log.error("插入新部门 [{}] 时发生未知错误: {}", newName, e.getMessage(), e);
                }
            }

            String message = String.format("部门同步完成，成功新增 %d 条记录。", successCount);
            if (failureCount > 0) {
                message += String.format(" 失败 %d 条。", failureCount);
            }
            log.info(message);
            return new SyncResult(successCount, failureCount, message);

        } catch (Exception e) {
            log.error("部门同步过程中发生严重错误: {}", e.getMessage(), e);
            return new SyncResult(successCount, failureCount, "部门同步过程中发生严重错误: " + e.getMessage());
        }
    }

    /**
     * 执行员工数据同步。
     * 从 t_wkt 同步新员工，并根据员工在 t_wkt 的最新记录查找部门 ID。
     */
    @Override
    @Transactional // 考虑是否需要整体事务
    public SyncResult syncEmployees() {
        // 实现员工同步逻辑
        // 1. 获取 t_wkt 不重复员工列表: timesheetWorkMapper.findDistinctEmployees()
        // 2. 获取 t_employee 现有员工列表: employeeMapper.findAll(...) 或 findEmployeeKeys()
        // 3. 比较差异，找出新员工
        // 4. 对于新员工：
        //    a. 解析 employee 字符串获取 employeeId 和 employeeName
        //    b. 调用 timesheetWorkMapper.findLatestDepByEmployee(employee) 获取最新部门名称
        //    c. 如果部门名称非空，调用 departmentMapper.findIdByName(depName) 获取 dep_id
        //    d. 如果 dep_id 找到，创建 Employee 对象并调用 employeeMapper.insert()
        //    e. 如果部门名称为空或 dep_id 未找到，记录失败或跳过
        //    f. 记录成功/失败数量
        // 5. 返回 SyncResult

        log.info("开始执行员工数据同步...");
        long successCount = 0;
        long failureCount = 0;

        try {
            // 1. 获取源数据 (t_wkt 不重复员工 "工号-姓名")
            List<String> sourceEmployees = timesheetWorkMapper.findDistinctEmployees();
            if (sourceEmployees.isEmpty()) {
                return new SyncResult(0, 0, "源数据中无员工信息，无需同步。");
            }
            log.debug("从 t_wkt 获取到 {} 个不重复员工信息。", sourceEmployees.size());

            // 2. 获取目标数据 (t_employee 现有员工主键 "工号-姓名")
            List<Employee> targetEmployeeList = employeeMapper.findAll(Collections.emptyMap());
            Set<String> targetEmployeeKeys = targetEmployeeList.stream()
                    .map(Employee::getEmployee)
                    .collect(Collectors.toSet());
            log.debug("从 t_employee 获取到 {} 个现有员工。", targetEmployeeKeys.size());

            // 3. 找出新员工
            List<String> newEmployeeKeys = sourceEmployees.stream()
                    .filter(emp -> !targetEmployeeKeys.contains(emp))
                    .toList();

            if (newEmployeeKeys.isEmpty()) {
                log.info("没有新的员工需要同步。");
                return new SyncResult(0, 0, "数据已是最新，无需同步员工。");
            }
            log.info("发现 {} 个新员工需要同步。", newEmployeeKeys.size());

            // 4. 逐条处理新员工
            for (String newEmpKey : newEmployeeKeys) {
                log.debug("处理新员工: {}", newEmpKey);
                try {
                    // a. 解析工号和姓名
                    String[] parts = StringUtils.splitAndGet(newEmpKey, "-", 0) != null ? newEmpKey.split("-", 2) : null;
                    if (parts == null || parts.length != 2 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
                        log.warn("无法解析员工信息字符串: '{}'，跳过此员工。", newEmpKey);
                        failureCount++;
                        continue;
                    }
                    String employeeId = parts[0].trim();
                    String employeeName = parts[1].trim();

                    // b. 查找最新部门名称
                    String latestDepName = timesheetWorkMapper.findLatestDepByEmployee(newEmpKey);
                    if (StringUtils.isBlank(latestDepName)) {
                        log.warn("员工 '{}' 在 t_wkt 中没有找到有效的部门信息，将使用 null 部门 ID。", newEmpKey);
                        // failureCount++; // 或者允许插入 null 部门 ID
                        // continue;
                    }

                    // c. 查找部门 ID
                    Integer depId = null;
                    if (StringUtils.isNotBlank(latestDepName)) {
                        depId = departmentMapper.findIdByName(latestDepName);
                        if (depId == null) {
                            log.warn("员工 '{}' 的最新部门 '{}' 在 t_department 表中未找到，将使用 null 部门 ID。", newEmpKey, latestDepName);
                            // failureCount++; // 或者允许插入 null 部门 ID
                            // continue;
                        }
                    }

                    // d. 创建并插入 Employee 对象
                    Employee newEmployee = new Employee();
                    newEmployee.setEmployee(newEmpKey);
                    newEmployee.setEmployeeId(employeeId);
                    newEmployee.setEmployeeName(employeeName);
                    newEmployee.setDepId(depId); // 可能为 null
                    newEmployee.setActive(true); // 默认值
                    newEmployee.setStatistics(true); // 默认值

                    employeeMapper.insert(newEmployee);
                    successCount++;
                    log.debug("成功插入新员工: {}", newEmpKey);

                } catch (DataAccessException e) {
                    failureCount++;
                    log.error("插入新员工 [{}] 时发生数据库错误: {}", newEmpKey, e.getMessage(), e);
                } catch (Exception e) {
                    failureCount++;
                    log.error("处理新员工 [{}] 时发生未知错误: {}", newEmpKey, e.getMessage(), e);
                }
            }

            String message = String.format("员工同步完成，成功新增 %d 条记录。", successCount);
            if (failureCount > 0) {
                message += String.format(" 失败 %d 条（详情请查看日志）。", failureCount);
            }
            log.info(message);
            return new SyncResult(successCount, failureCount, message);

        } catch (Exception e) {
            log.error("员工同步过程中发生严重错误: {}", e.getMessage(), e);
            return new SyncResult(successCount, failureCount, "员工同步过程中发生严重错误: " + e.getMessage());
        }
    }

    /**
     * 执行工时编码数据同步。
     */
    @Override
    // @Transactional
    public SyncResult syncTimesheetCodes() {
        // 实现工时编码同步逻辑
        // 1. 获取 t_wkt 不重复工时编码 Map 列表: timesheetWorkMapper.findDistinctTimesheetCodes()
        // 2. 获取 t_timesheet_code 现有 ts_bm 列表: timesheetCodeMapper.findAll(...) 或 findTsBms()
        // 3. 比较差异，找出新工时编码
        // 4. 对于新工时编码 Map：
        //    a. 创建 TimesheetCode 对象 (tsBm, tsName, sTsBm 来自 Map, 其他使用默认值)
        //    b. 调用 timesheetCodeMapper.insert()
        //    c. 记录成功/失败数量
        // 5. 返回 SyncResult

        log.info("开始执行工时编码数据同步...");
        long successCount = 0;
        long failureCount = 0;

        try {
            // 1. 获取源数据 (Map 列表: ts_bm, ts_name, s_ts_bm)
            List<Map<String, String>> sourceCodes = timesheetWorkMapper.findDistinctTimesheetCodes();
            if (sourceCodes.isEmpty()) {
                return new SyncResult(0, 0, "源数据中无工时编码信息，无需同步。");
            }
            log.debug("从 t_wkt 获取到 {} 个不重复工时编码信息。", sourceCodes.size());

            // 2. 获取目标数据 (现有 ts_bm 集合)
            List<TimesheetCode> targetCodeList = timesheetCodeMapper.findAll(Collections.emptyMap());
            Set<String> targetTsBms = targetCodeList.stream()
                    .map(TimesheetCode::getTsBm)
                    .collect(Collectors.toSet());
            log.debug("从 t_timesheet_code 获取到 {} 个现有工时编码。", targetTsBms.size());

            // 3. 找出新工时编码
            List<Map<String, String>> newCodes = sourceCodes.stream()
                    .filter(codeMap -> !targetTsBms.contains(codeMap.get("ts_bm")))
                    .toList();

            if (newCodes.isEmpty()) {
                log.info("没有新的工时编码需要同步。");
                return new SyncResult(0, 0, "数据已是最新，无需同步工时编码。");
            }
            log.info("发现 {} 个新工时编码需要同步。", newCodes.size());

            // 4. 逐条插入新工时编码
            for (Map<String, String> codeMap : newCodes) {
                String newTsBm = codeMap.get("ts_bm");
                if (StringUtils.isBlank(newTsBm)) {
                    log.warn("发现空的工时编码，跳过。");
                    failureCount++;
                    continue;
                }
                try {
                    TimesheetCode newCode = new TimesheetCode();
                    newCode.setTsBm(newTsBm);
                    newCode.setTsName(codeMap.get("ts_name")); // 可能为 null
                    newCode.setSTsBm(codeMap.get("s_ts_bm")); // 可能为 null
                    newCode.setCustomProjectName(null); // 默认值
                    newCode.setProjectTimesheet(true); // 默认值
                    newCode.setEnabled(true); // 默认值
                    newCode.setProjectBusinessType(null); // 默认值

                    timesheetCodeMapper.insert(newCode);
                    successCount++;
                    log.debug("成功插入新工时编码: {}", newTsBm);
                } catch (DataAccessException e) {
                    failureCount++;
                    log.error("插入新工时编码 [{}] 时发生数据库错误: {}", newTsBm, e.getMessage(), e);
                } catch (Exception e) {
                    failureCount++;
                    log.error("处理新工时编码 [{}] 时发生未知错误: {}", newTsBm, e.getMessage(), e);
                }
            }

            String message = String.format("工时编码同步完成，成功新增 %d 条记录。", successCount);
            if (failureCount > 0) {
                message += String.format(" 失败 %d 条。", failureCount);
            }
            log.info(message);
            return new SyncResult(successCount, failureCount, message);

        } catch (Exception e) {
            log.error("工时编码同步过程中发生严重错误: {}", e.getMessage(), e);
            return new SyncResult(successCount, failureCount, "工时编码同步过程中发生严重错误: " + e.getMessage());
        }
    }

    /**
     * 执行利润中心数据同步。
     */
    @Override
    // @Transactional
    public SyncResult syncProfitCenters() {
        // 项目利润中心同步逻辑
        // 1. 获取 t_wkt 不重复 zone 列表: timesheetWorkMapper.findDistinctProfitCenters()
        // 2. 获取 t_profit_center 现有 zone 列表: profitCenterMapper.findAll(...) 或 findZones()
        // 3. 比较差异，找出新 zone
        // 4. 对于新 zone：
        //    a. 按 "-" 分割字符串，最多取 6 部分
        //    b. 创建 ProfitCenter 对象 (zone = newZone, 其他字段根据分割结果填充，不足则为 null，多余丢弃，负责人等字段为 null，isEnabled 为 true)
        //    c. 调用 profitCenterMapper.insert()
        //    d. 记录成功/失败数量
        // 5. 返回 SyncResult

        log.info("开始执行利润中心数据同步...");
        long successCount = 0;
        long failureCount = 0;

        try {
            // 1. 获取源数据 (t_wkt 不重复 zone)
            List<String> sourceZones = timesheetWorkMapper.findDistinctProfitCenters();
            if (sourceZones.isEmpty()) {
                return new SyncResult(0, 0, "源数据中无利润中心信息，无需同步。");
            }
            log.debug("从 t_wkt 获取到 {} 个不重复利润中心。", sourceZones.size());

            // 2. 获取目标数据 (现有 zone 集合)
            List<ProfitCenter> targetPcList = profitCenterMapper.findAll(Collections.emptyMap());
            Set<String> targetZones = targetPcList.stream()
                    .map(ProfitCenter::getZone)
                    .collect(Collectors.toSet());
            log.debug("从 t_profit_center 获取到 {} 个现有利润中心。", targetZones.size());


            // 3. 找出新 zone
            List<String> newZones = sourceZones.stream()
                    .filter(zone -> !targetZones.contains(zone))
                    .toList();

            if (newZones.isEmpty()) {
                log.info("没有新的利润中心需要同步。");
                return new SyncResult(0, 0, "数据已是最新，无需同步利润中心。");
            }
            log.info("发现 {} 个新利润中心需要同步。", newZones.size());

            // 4. 逐条插入新利润中心
            for (String newZone : newZones) {
                if (StringUtils.isBlank(newZone)) {
                    log.warn("发现空的利润中心名称，跳过。");
                    failureCount++;
                    continue;
                }
                try {
                    ProfitCenter newPc = new ProfitCenter();
                    newPc.setZone(newZone);

                    // 解析 zone 字符串
                    String[] parts = newZone.split("-");
                    newPc.setBusinessType(parts.length > 0 ? parts[0].trim() : null);
                    newPc.setRegionCategory(parts.length > 1 ? parts[1].trim() : null);
                    newPc.setRegionName(parts.length > 2 ? parts[2].trim() : null);
                    newPc.setCenterName(parts.length > 3 ? parts[3].trim() : null);
                    newPc.setBusinessSubcategory(parts.length > 4 ? parts[4].trim() : null);
                    newPc.setDepartmentName(parts.length > 5 ? parts[5].trim() : null); // 只取前6部分

                    // 设置默认值
                    newPc.setResponsiblePerson(null);
                    newPc.setWorkLocation(null);
                    newPc.setCustomZoneRemark(null);
                    newPc.setEnabled(true);

                    profitCenterMapper.insert(newPc);
                    successCount++;
                    log.debug("成功插入新利润中心: {}", newZone);
                } catch (DataAccessException e) {
                    failureCount++;
                    log.error("插入新利润中心 [{}] 时发生数据库错误: {}", newZone, e.getMessage(), e);
                } catch (Exception e) {
                    failureCount++;
                    log.error("处理新利润中心 [{}] 时发生未知错误: {}", newZone, e.getMessage(), e);
                }
            }

            String message = String.format("利润中心同步完成，成功新增 %d 条记录。", successCount);
            if (failureCount > 0) {
                message += String.format(" 失败 %d 条。", failureCount);
            }
            log.info(message);
            return new SyncResult(successCount, failureCount, message);

        } catch (Exception e) {
            log.error("利润中心同步过程中发生严重错误: {}", e.getMessage(), e);
            return new SyncResult(successCount, failureCount, "利润中心同步过程中发生严重错误: " + e.getMessage());
        }
    }
}




