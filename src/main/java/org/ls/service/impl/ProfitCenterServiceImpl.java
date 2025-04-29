package org.ls.service.impl;

import org.ls.entity.ProfitCenter;
import org.ls.mapper.ProfitCenterMapper;
// import org.ls.mapper.TimesheetWorkMapper; // 可选，用于检查关联
import org.ls.service.ProfitCenterService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 利润中心服务实现类
 * 文件路径: src/main/java/org/ls/service/impl/ProfitCenterServiceImpl.java
 */
@Service
public class ProfitCenterServiceImpl implements ProfitCenterService {

    private static final Logger log = LoggerFactory.getLogger(ProfitCenterServiceImpl.class);

    private final ProfitCenterMapper profitCenterMapper;
    // private final TimesheetWorkMapper timesheetWorkMapper; // 可选注入

    @Autowired
    public ProfitCenterServiceImpl(ProfitCenterMapper profitCenterMapper) { //, TimesheetWorkMapper timesheetWorkMapper) {
        this.profitCenterMapper = profitCenterMapper;
        // this.timesheetWorkMapper = timesheetWorkMapper;
    }

    @Override
    public ProfitCenter findProfitCenterByZone(String zone) {
        if (StringUtils.isBlank(zone)) {
            log.warn("查询利润中心失败：zone (主键) 为空。");
            return null;
        }
        return profitCenterMapper.findByZone(zone);
    }

    // 所有使用到利润中心的地方，均需要调用此方法。因为此方法是对利润中心进行去重。
    @Override
    public List<ProfitCenter> findProfitCenterDistinctCZRAll(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeProfitCenterFilterParams(params);
        try {
            return profitCenterMapper.findDistinctCZRAll(params);
        } catch (Exception e) {
            log.error("查询利润中心列表时出错: params={}, error={}", params, e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    @Override
    public List<ProfitCenter> findProfitCenters(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeProfitCenterFilterParams(params);
        try {
            return profitCenterMapper.findAll(params);
        } catch (Exception e) {
            log.error("查询利润中心列表时出错: params={}, error={}", params, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countProfitCenters(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeProfitCenterFilterParams(params);
        try {
            return profitCenterMapper.countAll(params);
        } catch (Exception e) {
            log.error("统计利润中心数量时出错: params={}, error={}", params, e.getMessage(), e);
            return 0L;
        }
    }

    // 辅助方法：清理利润中心查询参数中的字符串
    private void sanitizeProfitCenterFilterParams(Map<String, Object> params) {
        if (params != null) {
            String[] keysToSanitize = {"zone", "businessType", "regionCategory", "regionName", "centerName",
                    "businessSubcategory", "departmentName", "responsiblePerson",
                    "workLocation", "customZoneRemark"};
            for (String key : keysToSanitize) {
                if (params.containsKey(key) && params.get(key) instanceof String) {
                    params.put(key, StringUtils.simpleSanitize((String) params.get(key)));
                }
            }
        }
    }


    @Override
    @Transactional
    public int addProfitCenter(ProfitCenter profitCenter) {
        if (profitCenter == null || StringUtils.isBlank(profitCenter.getZone())) {
            log.error("添加利润中心失败：zone (主键) 为空或实体为 null。 ProfitCenter: {}", profitCenter);
            throw new IllegalArgumentException("添加利润中心失败：利润中心全名（zone）不能为空。");
        }

        // 检查主键是否已存在
        if (profitCenterMapper.findByZone(profitCenter.getZone()) != null) {
            log.warn("添加利润中心失败：利润中心 [{}] 已存在。", profitCenter.getZone());
            throw new IllegalStateException("利润中心已存在，无法添加。");
        }

        // 清理用户输入的字段
        if (StringUtils.isNotBlank(profitCenter.getResponsiblePerson())) {
            profitCenter.setResponsiblePerson(StringUtils.simpleSanitize(profitCenter.getResponsiblePerson()));
        }
        if (StringUtils.isNotBlank(profitCenter.getWorkLocation())) {
            profitCenter.setWorkLocation(StringUtils.simpleSanitize(profitCenter.getWorkLocation()));
        }
        if (StringUtils.isNotBlank(profitCenter.getCustomZoneRemark())) {
            profitCenter.setCustomZoneRemark(StringUtils.simpleSanitize(profitCenter.getCustomZoneRemark()));
        }
        // zone, businessType 等字段来自同步，理论不需清理

        profitCenter.setUpdatedAt(null); // 确保插入时 updated_at 为 null

        try {
            log.info("准备添加利润中心: {}", profitCenter);
            return profitCenterMapper.insert(profitCenter);
        } catch (Exception e) {
            log.error("添加利润中心时数据库操作出错: {}, error={}", profitCenter, e.getMessage(), e);
            throw new RuntimeException("添加利润中心时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int updateProfitCenter(ProfitCenter profitCenter) {
        if (profitCenter == null || StringUtils.isBlank(profitCenter.getZone())) {
            log.error("更新利润中心失败：zone (主键) 为空或实体为 null。 ProfitCenter: {}", profitCenter);
            throw new IllegalArgumentException("更新利润中心失败：利润中心全名（zone）不能为空。");
        }

        // 检查利润中心是否存在
        ProfitCenter existing = profitCenterMapper.findByZone(profitCenter.getZone());
        if (existing == null) {
            log.warn("更新利润中心失败：利润中心 [{}] 不存在。", profitCenter.getZone());
            return 0; // 或抛出 ResourceNotFoundException
        }

        // 清理用户输入的字段
        if (StringUtils.isNotBlank(profitCenter.getResponsiblePerson())) {
            profitCenter.setResponsiblePerson(StringUtils.simpleSanitize(profitCenter.getResponsiblePerson()));
        } else {
            profitCenter.setResponsiblePerson(null); // 允许清空
        }
        if (StringUtils.isNotBlank(profitCenter.getWorkLocation())) {
            profitCenter.setWorkLocation(StringUtils.simpleSanitize(profitCenter.getWorkLocation()));
        } else {
            profitCenter.setWorkLocation(null); // 允许清空
        }
        if (StringUtils.isNotBlank(profitCenter.getCustomZoneRemark())) {
            profitCenter.setCustomZoneRemark(StringUtils.simpleSanitize(profitCenter.getCustomZoneRemark()));
        } else {
            profitCenter.setCustomZoneRemark(null); // 允许清空
        }

        // businessType 等同步字段不应在此更新
        profitCenter.setBusinessType(null);
        profitCenter.setRegionCategory(null);
        profitCenter.setRegionName(null);
        profitCenter.setCenterName(null);
        profitCenter.setBusinessSubcategory(null);
        profitCenter.setDepartmentName(null);


        // 设置更新时间
        // profitCenter.setUpdatedAt(LocalDateTime.now()); // Mapper XML 中已使用 CURRENT_TIMESTAMP

        try {
            log.info("准备更新利润中心: {}", profitCenter);
            return profitCenterMapper.update(profitCenter);
        } catch (Exception e) {
            log.error("更新利润中心时数据库操作出错: {}, error={}", profitCenter, e.getMessage(), e);
            throw new RuntimeException("更新利润中心时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int deleteProfitCenter(String zone) {
        if (StringUtils.isBlank(zone)) {
            log.warn("删除利润中心失败：zone (主键) 为空。");
            return 0;
        }

        // 检查利润中心是否存在
        ProfitCenter existing = profitCenterMapper.findByZone(zone);
        if (existing == null) {
            log.warn("删除利润中心失败：利润中心 [{}] 不存在。", zone);
            return 0;
        }

        // 检查是否被 t_wkt 引用 (可选)
        // long usageCount = timesheetWorkMapper.countAll(Map.of("zone", zone));
        // if (usageCount > 0) { ... }

        try {
            log.info("准备删除利润中心: {}", zone);
            return profitCenterMapper.delete(zone);
        } catch (Exception e) {
            log.error("删除利润中心时数据库操作出错: {}, error={}", zone, e.getMessage(), e);
            throw new RuntimeException("删除利润中心时发生数据库错误。", e);
        }
    }
}
