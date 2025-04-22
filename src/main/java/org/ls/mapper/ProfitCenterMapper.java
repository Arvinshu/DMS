package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ls.entity.ProfitCenter;

import java.util.List;
import java.util.Map;

/**
 * Mapper Interface for t_profit_center (利润中心表)
 * 文件路径: src/main/java/org/ls/mapper/ProfitCenterMapper.java
 */
@Mapper
public interface ProfitCenterMapper {

    /**
     * 根据利润中心全名 (主键) 查询利润中心信息
     *
     * @param zone 利润中心全名 (主键)
     * @return ProfitCenter 实体 或 null
     */
    ProfitCenter findByZone(String zone);

    /**
     * 查询利润中心列表 (支持过滤和分页)
     *
     * @param params 包含过滤条件和分页参数的 Map
     * - 可选过滤条件: zone (模糊), businessType (模糊), regionCategory (模糊), regionName (模糊), centerName (模糊), businessSubcategory (模糊), departmentName (模糊), responsiblePerson (模糊), workLocation (模糊), customZoneRemark (模糊), enabled
     * - 分页参数: offset, limit
     * @return ProfitCenter 列表
     */
    List<ProfitCenter> findAll(Map<String, Object> params);

    /**
     * 统计符合条件的利润中心总数 (用于分页)
     *
     * @param params 包含过滤条件的 Map (同 findAll)
     * @return 记录总数
     */
    long countAll(Map<String, Object> params);

    /**
     * 插入新的利润中心信息
     *
     * @param profitCenter ProfitCenter 实体 (zone 必须提供)
     * @return 影响的行数
     */
    int insert(ProfitCenter profitCenter);

    /**
     * 更新利润中心信息
     *
     * @param profitCenter ProfitCenter 实体 (必须包含主键 zone)
     * @return 影响的行数
     */
    int update(ProfitCenter profitCenter);

    /**
     * 根据利润中心全名 (主键) 删除利润中心信息
     *
     * @param zone 利润中心全名 (主键)
     * @return 影响的行数
     */
    int delete(String zone);

    /**
     * 查询所有不同的利润中心备注 (custom_zone_remark)，并按区域类别、大区名称、备注排序。
     * 用于生成利润中心交叉表的行头。
     * @return 包含 custom_zone_remark, region_category, region_name 的 Map 列表。
     */
    List<Map<String, Object>> findDistinctRemarksSorted();

}
