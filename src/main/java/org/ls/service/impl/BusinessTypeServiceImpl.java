package org.ls.service.impl;

import org.ls.entity.BusinessType;
import org.ls.mapper.BusinessTypeMapper;
import org.ls.mapper.TimesheetCodeMapper; // 引入 TimesheetCodeMapper
import org.ls.service.BusinessTypeService;
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
 * 业务类型服务实现类
 * 文件路径: src/main/java/org/ls/service/impl/BusinessTypeServiceImpl.java
 */
@Service
public class BusinessTypeServiceImpl implements BusinessTypeService {

    private static final Logger log = LoggerFactory.getLogger(BusinessTypeServiceImpl.class);

    private final BusinessTypeMapper businessTypeMapper;
    private final TimesheetCodeMapper timesheetCodeMapper; // 注入 TimesheetCodeMapper

    @Autowired
    public BusinessTypeServiceImpl(BusinessTypeMapper businessTypeMapper, TimesheetCodeMapper timesheetCodeMapper) {
        this.businessTypeMapper = businessTypeMapper;
        this.timesheetCodeMapper = timesheetCodeMapper;
    }

    @Override
    public BusinessType findBusinessTypeById(Integer id) {
        if (id == null) {
            log.warn("查询业务类型失败：ID 为空。");
            return null;
        }
        return businessTypeMapper.findById(id);
    }

    @Override
    public BusinessType findBusinessTypeByName(String businessName) {
        if (StringUtils.isBlank(businessName)) {
            log.warn("查询业务类型失败：业务名称为空。");
            return null;
        }
        String sanitizedName = StringUtils.simpleSanitize(businessName);
        return businessTypeMapper.findByName(sanitizedName);
    }

    @Override
    public List<BusinessType> findBusinessTypes(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeBusinessTypeFilterParams(params);
        try {
            return businessTypeMapper.findAll(params);
        } catch (Exception e) {
            log.error("查询业务类型列表时出错: params={}, error={}", params, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countBusinessTypes(Map<String, Object> params) {
        // 清理 params 中的字符串参数
        sanitizeBusinessTypeFilterParams(params);
        try {
            return businessTypeMapper.countAll(params);
        } catch (Exception e) {
            log.error("统计业务类型数量时出错: params={}, error={}", params, e.getMessage(), e);
            return 0L;
        }
    }

    // 辅助方法：清理业务类型查询参数中的字符串
    private void sanitizeBusinessTypeFilterParams(Map<String, Object> params) {
        if (params != null) {
            if (params.containsKey("businessCategory") && params.get("businessCategory") instanceof String) {
                params.put("businessCategory", StringUtils.simpleSanitize((String) params.get("businessCategory")));
            }
            if (params.containsKey("businessName") && params.get("businessName") instanceof String) {
                params.put("businessName", StringUtils.simpleSanitize((String) params.get("businessName")));
            }
            if (params.containsKey("businessDescription") && params.get("businessDescription") instanceof String) {
                params.put("businessDescription", StringUtils.simpleSanitize((String) params.get("businessDescription")));
            }
        }
    }


    @Override
    @Transactional
    public int addBusinessType(BusinessType businessType) {
        if (businessType == null || StringUtils.isBlank(businessType.getBusinessName())
                || StringUtils.isBlank(businessType.getBusinessCategory())) {
            log.error("添加业务类型失败：关键参数为空或无效。 BusinessType: {}", businessType);
            throw new IllegalArgumentException("添加业务类型失败：业务名称和业务类别不能为空。");
        }

        // 检查名称是否已存在
        if (businessTypeMapper.findByName(businessType.getBusinessName()) != null) {
            log.warn("添加业务类型失败：业务名称 [{}] 已存在。", businessType.getBusinessName());
            throw new IllegalStateException("业务名称已存在，无法添加。");
        }

        // 清理用户输入的字段
        businessType.setBusinessCategory(StringUtils.simpleSanitize(businessType.getBusinessCategory()));
        businessType.setBusinessName(StringUtils.simpleSanitize(businessType.getBusinessName()));
        if (StringUtils.isNotBlank(businessType.getBusinessDescription())) {
            businessType.setBusinessDescription(StringUtils.simpleSanitize(businessType.getBusinessDescription()));
        }

        businessType.setUpdatedAt(null); // 确保插入时 updated_at 为 null

        try {
            log.info("准备添加业务类型: {}", businessType);
            // insert 方法会回填自增 ID 到 businessType 对象中
            int result = businessTypeMapper.insert(businessType);
            log.info("添加业务类型成功，生成 ID: {}", businessType.getId());
            return result;
        } catch (Exception e) {
            log.error("添加业务类型时数据库操作出错: {}, error={}", businessType, e.getMessage(), e);
            throw new RuntimeException("添加业务类型时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int updateBusinessType(BusinessType businessType) {
        if (businessType == null || businessType.getId() == null) {
            log.error("更新业务类型失败：ID 为空或实体为 null。 BusinessType: {}", businessType);
            throw new IllegalArgumentException("更新业务类型失败：业务类型 ID 不能为空。");
        }

        // 检查业务类型是否存在
        BusinessType existing = businessTypeMapper.findById(businessType.getId());
        if (existing == null) {
            log.warn("更新业务类型失败：业务类型 ID [{}] 不存在。", businessType.getId());
            return 0; // 或抛出 ResourceNotFoundException
        }

        // 检查更新后的名称是否与其他记录冲突 (如果名称被修改)
        if (StringUtils.isNotBlank(businessType.getBusinessName())
                && !Objects.equals(existing.getBusinessName(), businessType.getBusinessName())) {
            if (businessTypeMapper.findByName(businessType.getBusinessName()) != null) {
                log.warn("更新业务类型失败：业务名称 [{}] 已被其他记录使用。", businessType.getBusinessName());
                throw new IllegalStateException("业务名称已被其他记录使用，无法更新。");
            }
            // 清理新名称
            businessType.setBusinessName(StringUtils.simpleSanitize(businessType.getBusinessName()));
        } else {
            businessType.setBusinessName(null); // 避免更新名称字段
        }


        // 清理其他用户输入的字段
        if (StringUtils.isNotBlank(businessType.getBusinessCategory())) {
            businessType.setBusinessCategory(StringUtils.simpleSanitize(businessType.getBusinessCategory()));
        } else {
            businessType.setBusinessCategory(null); // 避免更新
        }
        if (StringUtils.isNotBlank(businessType.getBusinessDescription())) {
            businessType.setBusinessDescription(StringUtils.simpleSanitize(businessType.getBusinessDescription()));
        } else {
            businessType.setBusinessDescription(null); // 避免更新
        }


        // 设置更新时间
        // businessType.setUpdatedAt(LocalDateTime.now()); // Mapper XML 中已使用 CURRENT_TIMESTAMP

        try {
            log.info("准备更新业务类型: {}", businessType);
            return businessTypeMapper.update(businessType);
        } catch (Exception e) {
            log.error("更新业务类型时数据库操作出错: {}, error={}", businessType, e.getMessage(), e);
            throw new RuntimeException("更新业务类型时发生数据库错误。", e);
        }
    }

    @Override
    @Transactional
    public int deleteBusinessType(Integer id) {
        if (id == null) {
            log.warn("删除业务类型失败：ID 为空。");
            return 0;
        }

        // 检查业务类型是否存在
        BusinessType existing = businessTypeMapper.findById(id);
        if (existing == null) {
            log.warn("删除业务类型失败：业务类型 ID [{}] 不存在。", id);
            return 0;
        }

        // 检查是否有工时编码关联到此业务类型
        long usageCount = timesheetCodeMapper.countAll(Map.of("projectBusinessType", existing.getBusinessName()));
        if (usageCount > 0) {
            log.warn("删除业务类型失败：业务类型 [{}] (ID:{}) 仍被 {} 个工时编码引用。", existing.getBusinessName(), id, usageCount);
            throw new IllegalStateException("无法删除业务类型：该类型仍被工时编码引用。");
        }

        try {
            log.info("准备删除业务类型: ID={}", id);
            return businessTypeMapper.delete(id);
        } catch (Exception e) {
            log.error("删除业务类型时数据库操作出错: ID={}, error={}", id, e.getMessage(), e);
            throw new RuntimeException("删除业务类型时发生数据库错误。", e);
        }
    }
}
