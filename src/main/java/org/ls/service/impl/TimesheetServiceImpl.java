package org.ls.service.impl;

import org.ls.dto.BatchInsertResult; // 导入 DTO
import org.ls.entity.TimesheetWork;
import org.ls.mapper.TimesheetWorkMapper;
import org.ls.service.TimesheetService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException; // 用于捕获主键冲突等
import org.springframework.dao.DuplicateKeyException; // 更具体的重复键异常
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 单条记录的事务

import java.time.LocalDate;
import java.util.ArrayList; // 引入 ArrayList
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工时记录服务实现类 (主键修正版)
 * 文件路径: src/main/java/org/ls/service/impl/TimesheetServiceImpl.java
 * 修正：适配新的四列组合主键 (ts_id, employee, ts_date, ts_bm)。
 */
@Service // 标记为 Spring Service 组件
public class TimesheetServiceImpl implements TimesheetService {

    private static final Logger log = LoggerFactory.getLogger(TimesheetServiceImpl.class);

    private final TimesheetWorkMapper timesheetWorkMapper;

    @Autowired
    public TimesheetServiceImpl(TimesheetWorkMapper timesheetWorkMapper) {
        this.timesheetWorkMapper = timesheetWorkMapper;
    }

    /**
     * 根据复合主键查询单个工时记录。
     * @param tsId     工时申请单号
     * @param employee 员工信息
     * @param tsDate   工时日期
     * @param tsBm     工时编码 (新增的主键部分)
     * @return TimesheetWork 实体，如果未找到则返回 null。
     */
    @Override
    public TimesheetWork findTimesheetById(String tsId, String employee, LocalDate tsDate, String tsBm) {
        // 增加对新主键 tsBm 的验证
        if (StringUtils.isBlank(tsId) || StringUtils.isBlank(employee) || tsDate == null || StringUtils.isBlank(tsBm)) {
            log.warn("查询工时记录失败：参数无效 (tsId={}, employee={}, tsDate={}, tsBm={})", tsId, employee, tsDate, tsBm);
            return null;
        }
        // 调用 Mapper 时传递所有四个主键参数
        return timesheetWorkMapper.findById(tsId, employee, tsDate, tsBm);
    }



    @Override
    public List<Map<String, Object>> findTimesheetStatistics(Map<String, Object> params) {
        try {
            return timesheetWorkMapper.findTimesheetStatistics(params);
        } catch (Exception e) {
            // 错误日志中已包含 params，避免重复打印敏感信息（如果 params 中包含）
            log.error("查询工时统计数据时出错: error={}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countTimesheetStatistics(Map<String, Object> params) {
        try {
            return timesheetWorkMapper.countTimesheetStatistics(params);
        } catch (Exception e) {
            log.error("统计工时统计数据时出错: error={}", e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public List<TimesheetWork> findAllTimesheets(Map<String, Object> params) {
        try {
            return timesheetWorkMapper.findAll(params);
        } catch (Exception e) {
            log.error("查询所有工时记录时出错: params={}, error={}", params, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countAllTimesheets(Map<String, Object> params) {
        try {
            return timesheetWorkMapper.countAll(params);
        } catch (Exception e) {
            log.error("统计所有工时记录时出错: params={}, error={}", params, e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    @Transactional // 添加事务管理
    public int addTimesheet(TimesheetWork timesheetWork) {
        // 验证所有主键字段和必要字段
        if (timesheetWork == null
                || StringUtils.isBlank(timesheetWork.getTsId())
                || StringUtils.isBlank(timesheetWork.getEmployee())
                || timesheetWork.getTsDate() == null
                || StringUtils.isBlank(timesheetWork.getTsBm()) // 验证新增的主键 tsBm
                /* 添加其他必要的非空验证，例如 tr, dep, tsStatus 等 */
                || StringUtils.isBlank(timesheetWork.getTr())
                || StringUtils.isBlank(timesheetWork.getDep())
                || StringUtils.isBlank(timesheetWork.getTsStatus())
                || StringUtils.isBlank(timesheetWork.getTsYm())
                || StringUtils.isBlank(timesheetWork.getNatureYm())
                || timesheetWork.getTsHours() == null
                || timesheetWork.getTsMonth() == null ) {
            log.error("添加工时记录失败：关键参数为空。 TimesheetWork: {}", timesheetWork);
            throw new IllegalArgumentException("添加工时记录失败：所有主键字段和必要信息不能为空。");
        }

        // 清理备注
        if (StringUtils.isNotBlank(timesheetWork.getTsComments())) {
            timesheetWork.setTsComments(StringUtils.simpleSanitize(timesheetWork.getTsComments()));
        }

// try-catch 移到 batchAddTimesheets 中处理
//        try {
//            // 检查记录是否已存在 (根据新的复合主键)
//            TimesheetWork existing = timesheetWorkMapper.findById(
//                    timesheetWork.getTsId(),
//                    timesheetWork.getEmployee(),
//                    timesheetWork.getTsDate(),
//                    timesheetWork.getTsBm() // 使用新的主键检查
//            );
//            if (existing != null) {
//                log.warn("添加工时记录失败：记录已存在。 Key: tsId={}, employee={}, tsDate={}, tsBm={}",
//                        timesheetWork.getTsId(), timesheetWork.getEmployee(), timesheetWork.getTsDate(), timesheetWork.getTsBm());
//                throw new IllegalStateException("工时记录已存在，无法重复添加。");
//            }
//
//            log.info("准备添加工时记录: {}", timesheetWork);
//            return timesheetWorkMapper.insert(timesheetWork);
//        } catch (Exception e) {
//            log.error("添加工时记录时数据库操作出错: {}, error={}", timesheetWork, e.getMessage(), e);
//            throw new RuntimeException("添加工时记录时发生数据库错误。", e);
//        }

        log.info("准备添加工时记录 (单条): {}", timesheetWork);
        // insert 操作本身可能会抛出 DuplicateKeyException 或其他 DataIntegrityViolationException
        return timesheetWorkMapper.insert(timesheetWork);


    }



    @Override
    @Transactional // 添加事务管理
    public int updateTimesheet(TimesheetWork timesheetWork) {
        // 验证传入对象及其所有主键字段非空
        if (timesheetWork == null
                || StringUtils.isBlank(timesheetWork.getTsId())
                || StringUtils.isBlank(timesheetWork.getEmployee())
                || timesheetWork.getTsDate() == null
                || StringUtils.isBlank(timesheetWork.getTsBm())) { // 验证新增的主键 tsBm
            log.error("更新工时记录失败：关键主键参数为空。 TimesheetWork: {}", timesheetWork);
            throw new IllegalArgumentException("更新工时记录失败：所有主键字段不能为空。");
        }

        // 检查记录是否存在 (使用新的复合主键)
        TimesheetWork existing = timesheetWorkMapper.findById(
                timesheetWork.getTsId(),
                timesheetWork.getEmployee(),
                timesheetWork.getTsDate(),
                timesheetWork.getTsBm() // 使用新的主键检查
        );
        if (existing == null) {
            log.warn("更新工时记录失败：记录不存在。 Key: tsId={}, employee={}, tsDate={}, tsBm={}",
                    timesheetWork.getTsId(), timesheetWork.getEmployee(), timesheetWork.getTsDate(), timesheetWork.getTsBm());
            return 0; // 或抛出异常
        }

        // 清理备注
        if (StringUtils.isNotBlank(timesheetWork.getTsComments())) {
            timesheetWork.setTsComments(StringUtils.simpleSanitize(timesheetWork.getTsComments()));
        }
        // 其他字段验证...

        try {
            log.info("准备更新工时记录: {}", timesheetWork);
            // Mapper 的 update 方法依赖对象中的主键字段来定位 WHERE 条件
            return timesheetWorkMapper.update(timesheetWork);
        } catch (Exception e) {
            log.error("更新工时记录时数据库操作出错: {}, error={}", timesheetWork, e.getMessage(), e);
            throw new RuntimeException("更新工时记录时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional // 添加事务管理
    public int deleteTimesheet(String tsId, String employee, LocalDate tsDate, String tsBm) {
        // 验证所有主键参数
        if (StringUtils.isBlank(tsId) || StringUtils.isBlank(employee) || tsDate == null || StringUtils.isBlank(tsBm)) {
            log.warn("删除工时记录失败：参数无效 (tsId={}, employee={}, tsDate={}, tsBm={})", tsId, employee, tsDate, tsBm);
            throw new IllegalArgumentException("删除工时记录失败：所有主键字段不能为空。");
        }

        // 检查记录是否存在 (可选，但建议)
        TimesheetWork existing = timesheetWorkMapper.findById(tsId, employee, tsDate, tsBm);
        if (existing == null) {
            log.warn("删除工时记录失败：记录不存在。 Key: tsId={}, employee={}, tsDate={}, tsBm={}", tsId, employee, tsDate, tsBm);
            return 0;
        }

        try {
            log.info("准备删除工时记录: tsId={}, employee={}, tsDate={}, tsBm={}", tsId, employee, tsDate, tsBm);
            // 调用 Mapper 时传递所有四个主键参数
            return timesheetWorkMapper.delete(tsId, employee, tsDate, tsBm);
        } catch (Exception e) {
            log.error("删除工时记录时数据库操作出错: tsId={}, employee={}, tsDate={}, tsBm={}, error={}", tsId, employee, tsDate, tsBm, e.getMessage(), e);
            throw new RuntimeException("删除工时记录时发生数据库错误。", e);
        }
    }


    /**
     * 批量添加工时记录的实现。
     * 逐条处理，不使用整体事务，以便反馈单条结果。
     */
    @Override
    public List<BatchInsertResult> batchAddTimesheets(List<TimesheetWork> timesheets) {
        if (timesheets == null || timesheets.isEmpty()) {
            return Collections.emptyList();
        }

        List<BatchInsertResult> results = new ArrayList<>();
        log.info("开始批量导入工时记录，共 {} 条", timesheets.size());

        for (int i = 0; i < timesheets.size(); i++) {
            TimesheetWork record = timesheets.get(i);
            BatchInsertResult result;
            String errorMessage = null;
            boolean success = false;

            // 提取关键字段用于结果 DTO
            String tsId = record != null ? record.getTsId() : null;
            String employee = record != null ? record.getEmployee() : null;
            LocalDate tsDate = record != null ? record.getTsDate() : null;
            String tsBm = record != null ? record.getTsBm() : null;

            try {
                // 调用单条添加方法（该方法包含验证逻辑并会抛出异常）
                // addTimesheet 方法本身有 @Transactional
                int affectedRows = addTimesheet(record);
                if (affectedRows > 0) {
                    success = true;
                    errorMessage = "导入成功";
                    log.debug("记录 #{} 导入成功: ({}, {}, {}, {})", i, tsId, employee, tsDate, tsBm);
                } else {
                    // 理论上 addTimesheet 失败会抛异常，较少到这一步
                    errorMessage = "导入失败 (未知原因)";
                    log.warn("记录 #{} 导入返回 0 行受影响: ({}, {}, {}, {})", i, tsId, employee, tsDate, tsBm);
                }
            } catch (DataIntegrityViolationException e) {
                // 捕获主键冲突或唯一约束违反异常
                errorMessage = "导入失败: 主键冲突或违反唯一约束.组合主键为：工时申请单号、申请人、工时日期、工时编码";
                log.warn("记录 #{} 导入失败 (主键冲突): ({}, {}, {}, {}). Error: {}", i, tsId, employee, tsDate, tsBm, e.getMessage());
            } catch (IllegalArgumentException e) {
                // 捕获 addTimesheet 中的验证异常
                errorMessage = "导入失败: " + e.getMessage();
                log.warn("记录 #{} 导入失败 (验证错误): ({}, {}, {}, {}). Error: {}", i, tsId, employee, tsDate, tsBm, e.getMessage());
            } catch (Exception e) {
                // 捕获其他意外错误
                errorMessage = "导入失败: " + e.getMessage();
                log.error("记录 #{} 导入时发生意外错误: ({}, {}, {}, {}). Error: {}", i, tsId, employee, tsDate, tsBm, e.getMessage(), e);
            }

            // 创建并添加结果
            result = BatchInsertResult.create(i, success, errorMessage, tsId, employee, tsDate, tsBm);
            results.add(result);
        }

        log.info("批量导入完成，共处理 {} 条记录。", timesheets.size());
        return results;
    }

    // --- 数据同步准备方法实现 (保持不变) ---

    @Override
    public List<String> getDistinctDepartments() {
        try {
            return timesheetWorkMapper.findDistinctDepartments();
        } catch (Exception e) {
            log.error("获取不重复部门列表时出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getDistinctEmployees() {
        try {
            return timesheetWorkMapper.findDistinctEmployees();
        } catch (Exception e) {
            log.error("获取不重复员工列表时出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, String>> getDistinctTimesheetCodes() {
        try {
            return timesheetWorkMapper.findDistinctTimesheetCodes();
        } catch (Exception e) {
            log.error("获取不重复工时编码列表时出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getDistinctProfitCenters() {
        try {
            return timesheetWorkMapper.findDistinctProfitCenters();
        } catch (Exception e) {
            log.error("获取不重复利润中心列表时出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


}
