package org.ls.service;

import org.ls.entity.BusinessType;

import java.util.List;
import java.util.Map;

/**
 * 业务类型服务接口
 * 定义业务类型表 (t_business_type) 相关的业务逻辑操作。
 * 文件路径: src/main/java/org/ls/service/BusinessTypeService.java
 */
public interface BusinessTypeService {

    /**
     * 根据 ID 查询业务类型。
     *
     * @param id 业务类型 ID (主键)。
     * @return BusinessType 实体，如果未找到则返回 null。
     */
    BusinessType findBusinessTypeById(Integer id);

    /**
     * 根据业务名称查询业务类型。
     *
     * @param businessName 业务名称 (唯一)。
     * @return BusinessType 实体，如果未找到则返回 null。
     */
    BusinessType findBusinessTypeByName(String businessName);

    /**
     * 查询业务类型列表（支持过滤和分页）。
     * 实现类中应处理分页和过滤逻辑。
     *
     * @param params 包含过滤条件和分页参数 (offset, limit) 的 Map。
     * @return BusinessType 实体列表。
     */
    List<BusinessType> findBusinessTypes(Map<String, Object> params);

    /**
     * 统计符合条件的业务类型总数。
     * 用于分页计算。
     *
     * @param params 包含过滤条件的 Map (同 findBusinessTypes)。
     * @return 记录总数。
     */
    long countBusinessTypes(Map<String, Object> params);

    /**
     * 添加一个新的业务类型。
     * 实现类中应进行必要的验证（如名称唯一性，输入清理）。
     *
     * @param businessType 要添加的业务类型实体。
     * @return 返回操作影响的行数，通常为 1 表示成功。
     * @throws // 可能抛出业务异常，例如名称已存在、验证失败
     */
    int addBusinessType(BusinessType businessType);

    /**
     * 更新现有的业务类型信息。
     * 实现类中应进行必要的验证（如名称唯一性，输入清理）。
     *
     * @param businessType 包含更新信息和主键 ID 的业务类型实体。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如记录不存在、名称已存在、验证失败
     */
    int updateBusinessType(BusinessType businessType);

    /**
     * 根据 ID 删除业务类型。
     * 实现类中可能需要检查是否存在关联的工时编码。
     *
     * @param id 业务类型 ID。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如存在关联工时编码不允许删除
     */
    int deleteBusinessType(Integer id);
}
