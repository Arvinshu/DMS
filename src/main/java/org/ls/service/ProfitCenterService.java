package org.ls.service;

import org.ls.entity.ProfitCenter;

import java.util.List;
import java.util.Map;

/**
 * 利润中心服务接口
 * 定义利润中心表 (t_profit_center) 相关的业务逻辑操作。
 * 文件路径: src/main/java/org/ls/service/ProfitCenterService.java
 */
public interface ProfitCenterService {

    /**
     * 根据利润中心全名 (主键) 查询利润中心信息。
     *
     * @param zone 利润中心全名 (主键)。
     * @return ProfitCenter 实体，如果未找到则返回 null。
     */
    ProfitCenter findProfitCenterByZone(String zone);

    /**
     * 查询利润中心列表（支持过滤和分页）。
     * 实现类中应处理分页和过滤逻辑。
     *
     * @param params 包含过滤条件和分页参数 (offset, limit) 的 Map。
     * @return ProfitCenter 实体列表。
     */
    List<ProfitCenter> findProfitCenters(Map<String, Object> params);

    /**
     * 统计符合条件的利润中心总数。
     * 用于分页计算。
     *
     * @param params 包含过滤条件的 Map (同 findProfitCenters)。
     * @return 记录总数。
     */
    long countProfitCenters(Map<String, Object> params);

    /**
     * 添加一个新的利润中心。
     * 实现类中应进行必要的验证（如主键唯一性，输入清理）。
     *
     * @param profitCenter 要添加的利润中心实体 (zone 必须提供)。
     * @return 返回操作影响的行数，通常为 1 表示成功。
     * @throws // 可能抛出业务异常，例如主键已存在、验证失败
     */
    int addProfitCenter(ProfitCenter profitCenter);

    /**
     * 更新现有的利润中心信息。
     * 实现类中应进行必要的验证（输入清理）。
     *
     * @param profitCenter 包含更新信息和主键 zone 的利润中心实体。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如记录不存在、验证失败
     */
    int updateProfitCenter(ProfitCenter profitCenter);

    /**
     * 根据利润中心全名 (主键) 删除利润中心信息。
     *
     * @param zone 利润中心全名 (主键)。
     * @return 返回操作影响的行数。
     */
    int deleteProfitCenter(String zone);
}
