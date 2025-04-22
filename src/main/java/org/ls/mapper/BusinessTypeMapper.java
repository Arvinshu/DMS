package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ls.entity.BusinessType;

import java.util.List;
import java.util.Map;

/**
 * Mapper Interface for t_business_type (业务类型表)
 * 文件路径: src/main/java/org/ls/mapper/BusinessTypeMapper.java
 */
@Mapper
public interface BusinessTypeMapper {

    /**
     * 根据 ID 查询业务类型
     *
     * @param id 业务类型 ID (主键)
     * @return BusinessType 实体 或 null
     */
    BusinessType findById(Integer id);

    /**
     * 根据业务名称查询业务类型
     *
     * @param businessName 业务名称 (唯一)
     * @return BusinessType 实体 或 null
     */
    BusinessType findByName(String businessName);

    /**
     * 查询业务类型列表 (支持过滤和分页)
     *
     * @param params 包含过滤条件和分页参数的 Map
     * - 可选过滤条件: businessCategory (模糊), businessName (模糊), businessDescription (模糊), enabled
     * - 分页参数: offset, limit
     * @return BusinessType 列表
     */
    List<BusinessType> findAll(Map<String, Object> params);

    /**
     * 统计符合条件的业务类型总数 (用于分页)
     *
     * @param params 包含过滤条件的 Map (同 findAll)
     * @return 记录总数
     */
    long countAll(Map<String, Object> params);

    /**
     * 插入新的业务类型信息
     *
     * @param businessType BusinessType 实体 (ID 会自动生成并回填)
     * @return 影响的行数
     */
    int insert(BusinessType businessType);

    /**
     * 更新业务类型信息
     *
     * @param businessType BusinessType 实体 (必须包含 ID)
     * @return 影响的行数
     */
    int update(BusinessType businessType);

    /**
     * 根据 ID 删除业务类型
     *
     * @param id 业务类型 ID
     * @return 影响的行数
     */
    int delete(Integer id);
}
