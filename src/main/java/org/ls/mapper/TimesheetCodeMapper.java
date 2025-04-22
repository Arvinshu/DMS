package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ls.entity.TimesheetCode;

import java.util.List;
import java.util.Map;

/**
 * Mapper Interface for t_timesheet_code (工时编码表)
 * 文件路径: src/main/java/org/ls/mapper/TimesheetCodeMapper.java
 */
@Mapper
public interface TimesheetCodeMapper {

    /**
     * 根据工时编码 (主键) 查询工时信息
     *
     * @param tsBm 工时编码 (主键)
     * @return TimesheetCode 实体 或 null
     */
    TimesheetCode findByTsBm(String tsBm);

    /**
     * 查询工时编码列表 (支持过滤和分页)
     *
     * @param params 包含过滤条件和分页参数的 Map
     * - 可选过滤条件: tsBm (模糊), tsName (模糊), sTsBm (模糊), customProjectName (模糊), projectTimesheet, enabled, projectBusinessType
     * - 分页参数: offset, limit
     * @return TimesheetCode 列表
     */
    List<TimesheetCode> findAll(Map<String, Object> params);

    /**
     * 统计符合条件的工时编码总数 (用于分页)
     *
     * @param params 包含过滤条件的 Map (同 findAll)
     * @return 记录总数
     */
    long countAll(Map<String, Object> params);

    /**
     * 插入新的工时编码信息
     *
     * @param timesheetCode TimesheetCode 实体 (tsBm 必须提供)
     * @return 影响的行数
     */
    int insert(TimesheetCode timesheetCode);

    /**
     * 更新工时编码信息
     *
     * @param timesheetCode TimesheetCode 实体 (必须包含主键 tsBm)
     * @return 影响的行数
     */
    int update(TimesheetCode timesheetCode);

    /**
     * 根据工时编码 (主键) 删除工时信息
     *
     * @param tsBm 工时编码 (主键)
     * @return 影响的行数
     */
    int delete(String tsBm);
}
