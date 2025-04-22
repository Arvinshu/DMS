package org.ls.service;

import org.ls.entity.TimesheetCode;

import java.util.List;
import java.util.Map;

/**
 * 工时编码服务接口
 * 定义工时编码表 (t_timesheet_code) 相关的业务逻辑操作。
 * 文件路径: src/main/java/org/ls/service/TimesheetCodeService.java
 */
public interface TimesheetCodeService {

    /**
     * 根据工时编码 (主键) 查询工时信息。
     *
     * @param tsBm 工时编码 (主键)。
     * @return TimesheetCode 实体，如果未找到则返回 null。
     */
    TimesheetCode findTimesheetCodeByTsBm(String tsBm);

    /**
     * 查询工时编码列表（支持过滤和分页）。
     * 实现类中应处理分页和过滤逻辑。
     *
     * @param params 包含过滤条件和分页参数 (offset, limit) 的 Map。
     * @return TimesheetCode 实体列表。
     */
    List<TimesheetCode> findTimesheetCodes(Map<String, Object> params);

    /**
     * 统计符合条件的工时编码总数。
     * 用于分页计算。
     *
     * @param params 包含过滤条件的 Map (同 findTimesheetCodes)。
     * @return 记录总数。
     */
    long countTimesheetCodes(Map<String, Object> params);

    /**
     * 添加一个新的工时编码。
     * 实现类中应进行必要的验证（如主键唯一性，业务类型存在性，输入清理）。
     *
     * @param timesheetCode 要添加的工时编码实体 (tsBm 必须提供)。
     * @return 返回操作影响的行数，通常为 1 表示成功。
     * @throws // 可能抛出业务异常，例如主键已存在、业务类型不存在、验证失败
     */
    int addTimesheetCode(TimesheetCode timesheetCode);

    /**
     * 更新现有的工时编码信息。
     * 实现类中应进行必要的验证（如业务类型存在性，输入清理）。
     *
     * @param timesheetCode 包含更新信息和主键 tsBm 的工时编码实体。
     * @return 返回操作影响的行数。
     * @throws // 可能抛出业务异常，例如记录不存在、业务类型不存在、验证失败
     */
    int updateTimesheetCode(TimesheetCode timesheetCode);

    /**
     * 根据工时编码 (主键) 删除工时信息。
     *
     * @param tsBm 工时编码 (主键)。
     * @return 返回操作影响的行数。
     */
    int deleteTimesheetCode(String tsBm);
}
