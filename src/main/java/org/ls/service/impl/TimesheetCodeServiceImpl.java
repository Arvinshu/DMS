package org.ls.service.impl;

import org.ls.entity.BusinessType;
import org.ls.entity.TimesheetCode;
import org.ls.mapper.BusinessTypeMapper; // 引入 BusinessTypeMapper
import org.ls.mapper.TimesheetCodeMapper;
import org.ls.service.TimesheetCodeService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工时编码服务实现类
 * 文件路径: src/main/java/org/ls/service/impl/TimesheetCodeServiceImpl.java
 */
@Service
public class TimesheetCodeServiceImpl implements TimesheetCodeService {

    private static final Logger log = LoggerFactory.getLogger(TimesheetCodeServiceImpl.class);

    private final TimesheetCodeMapper timesheetCodeMapper;
    private final BusinessTypeMapper businessTypeMapper; // 注入 BusinessTypeMapper

    @Autowired
    public TimesheetCodeServiceImpl(TimesheetCodeMapper timesheetCodeMapper, BusinessTypeMapper businessTypeMapper) {
        this.timesheetCodeMapper = timesheetCodeMapper;
        this.businessTypeMapper = businessTypeMapper;
    }

    @Override
    public TimesheetCode findTimesheetCodeByTsBm(String tsBm) {
        if (StringUtils.isBlank(tsBm)) {
            log.warn("查询工时编码失败：tsBm 为空。");
            return null;
        }
        return timesheetCodeMapper.findByTsBm(tsBm);
    }

    @Override
    public List<TimesheetCode> findTimesheetCodes(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeTimesheetCodeFilterParams(params);
        try {
            return timesheetCodeMapper.findAll(params);
        } catch (Exception e) {
            log.error("查询工时编码列表时出错: params={}, error={}", params, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countTimesheetCodes(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeTimesheetCodeFilterParams(params);
        try {
            return timesheetCodeMapper.countAll(params);
        } catch (Exception e) {
            log.error("统计工时编码数量时出错: params={}, error={}", params, e.getMessage(), e);
            return 0L;
        }
    }

    // 辅助方法：清理工时编码查询参数中的字符串
    private void sanitizeTimesheetCodeFilterParams(Map<String, Object> params) {
        if (params != null) {
            if (params.containsKey("tsBm") && params.get("tsBm") instanceof String) {
                params.put("tsBm", StringUtils.simpleSanitize((String) params.get("tsBm")));
            }
            if (params.containsKey("tsName") && params.get("tsName") instanceof String) {
                params.put("tsName", StringUtils.simpleSanitize((String) params.get("tsName")));
            }
            if (params.containsKey("sTsBm") && params.get("sTsBm") instanceof String) {
                params.put("sTsBm", StringUtils.simpleSanitize((String) params.get("sTsBm")));
            }
            if (params.containsKey("customProjectName") && params.get("customProjectName") instanceof String) {
                params.put("customProjectName", StringUtils.simpleSanitize((String) params.get("customProjectName")));
            }
            if (params.containsKey("projectBusinessType") && params.get("projectBusinessType") instanceof String) {
                params.put("projectBusinessType", StringUtils.simpleSanitize((String) params.get("projectBusinessType")));
            }
        }
    }

    @Override
    @Transactional
    public int addTimesheetCode(TimesheetCode timesheetCode) {
        if (timesheetCode == null || StringUtils.isBlank(timesheetCode.getTsBm())) {
            log.error("添加工时编码失败：tsBm 为空或实体为 null。 TimesheetCode: {}", timesheetCode);
            throw new IllegalArgumentException("添加工时编码失败：工时编码（tsBm）不能为空。");
        }

        // 检查主键是否已存在
        if (timesheetCodeMapper.findByTsBm(timesheetCode.getTsBm()) != null) {
            log.warn("添加工时编码失败：工时编码 [{}] 已存在。", timesheetCode.getTsBm());
            throw new IllegalStateException("工时编码已存在，无法添加。");
        }
        // 检查业务类型是否存在
        if (StringUtils.isNotBlank(timesheetCode.getProjectBusinessType())
                && businessTypeMapper.findByName(timesheetCode.getProjectBusinessType()) == null) {
            log.warn("添加工时编码失败：业务类型 [{}] 不存在。", timesheetCode.getProjectBusinessType());
            throw new IllegalArgumentException("指定的业务类型不存在。");
        }

        // 清理用户输入的字段
        if (StringUtils.isNotBlank(timesheetCode.getCustomProjectName())) {
            timesheetCode.setCustomProjectName(StringUtils.simpleSanitize(timesheetCode.getCustomProjectName()));
        }
        // tsName, sTsBm 来自同步，理论不需清理

        timesheetCode.setUpdatedAt(null); // 确保插入时 updated_at 为 null

        try {
            log.info("准备添加工时编码: {}", timesheetCode);
            return timesheetCodeMapper.insert(timesheetCode);
        } catch (Exception e) {
            log.error("添加工时编码时数据库操作出错: {}, error={}", timesheetCode, e.getMessage(), e);
            throw new RuntimeException("添加工时编码时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int updateTimesheetCode(TimesheetCode timesheetCode) {
        if (timesheetCode == null || StringUtils.isBlank(timesheetCode.getTsBm())) {
            log.error("更新工时编码失败：tsBm 为空或实体为 null。 TimesheetCode: {}", timesheetCode);
            throw new IllegalArgumentException("更新工时编码失败：工时编码（tsBm）不能为空。");
        }

        // 检查工时编码是否存在
        TimesheetCode existing = timesheetCodeMapper.findByTsBm(timesheetCode.getTsBm());
        if (existing == null) {
            log.warn("更新工时编码失败：工时编码 [{}] 不存在。", timesheetCode.getTsBm());
            return 0; // 或抛出 ResourceNotFoundException
        }
        // 检查业务类型是否存在 (如果传入了)
        if (StringUtils.isNotBlank(timesheetCode.getProjectBusinessType())
                && businessTypeMapper.findByName(timesheetCode.getProjectBusinessType()) == null) {
            log.warn("更新工时编码失败：业务类型 [{}] 不存在。", timesheetCode.getProjectBusinessType());
            throw new IllegalArgumentException("指定的业务类型不存在。");
        }

        // 清理用户输入的字段
        if (StringUtils.isNotBlank(timesheetCode.getCustomProjectName())) {
            timesheetCode.setCustomProjectName(StringUtils.simpleSanitize(timesheetCode.getCustomProjectName()));
        }
        // tsName, sTsBm 不应在此更新
        timesheetCode.setTsName(null);
        timesheetCode.setSTsBm(null);

        // 设置更新时间
        // timesheetCode.setUpdatedAt(LocalDateTime.now()); // Mapper XML 中已使用 CURRENT_TIMESTAMP

        try {
            log.info("准备更新工时编码: {}", timesheetCode);
            return timesheetCodeMapper.update(timesheetCode);
        } catch (Exception e) {
            log.error("更新工时编码时数据库操作出错: {}, error={}", timesheetCode, e.getMessage(), e);
            throw new RuntimeException("更新工时编码时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int deleteTimesheetCode(String tsBm) {
        if (StringUtils.isBlank(tsBm)) {
            log.warn("删除工时编码失败：tsBm 为空。");
            return 0;
        }

        // 检查工时编码是否存在
        TimesheetCode existing = timesheetCodeMapper.findByTsBm(tsBm);
        if (existing == null) {
            log.warn("删除工时编码失败：工时编码 [{}] 不存在。", tsBm);
            return 0;
        }

        // 检查是否被 t_wkt 引用 (这比较复杂，通常不直接检查，依赖数据库外键或允许悬空)
        // long usageCount = timesheetWorkMapper.countAll(Map.of("tsBm", tsBm));
        // if (usageCount > 0) { ... }

        try {
            log.info("准备删除工时编码: {}", tsBm);
            return timesheetCodeMapper.delete(tsBm);
        } catch (Exception e) {
            log.error("删除工时编码时数据库操作出错: {}, error={}", tsBm, e.getMessage(), e);
            throw new RuntimeException("删除工时编码时发生数据库错误。", e);
        }
    }
}
