package org.ls.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类：工时编码维护表 (t_timesheet_code)
 * 文件路径: src/main/java/org/ls/entity/TimesheetCode.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetCode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工时编码 (ts_bm)
     * 主键
     * 从 t_wkt.ts_bm 去重后同步过来，此列数据是从其他表同步过来，在页面只展示不维护。
     */
    private String tsBm;

    /**
     * 工时名称 (ts_name)
     * 将 t_wkt.ts_bm 对应的 t_wkt.ts_name 同步过来，此列数据是从其他表同步过来，在页面只展示不维护。
     */
    private String tsName;

    /**
     * 子工时编码 (s_ts_bm)
     * 将 t_wkt.ts_bm 对应的 t_wkt.s_ts_bm 同步过来，此列数据是从其他表同步过来，在页面只展示不维护。
     */
    private String sTsBm;

    /**
     * 工时信息 (custom_project_name)
     * 由于工时名称和实际项目名称不一致，此字段用以部门自己维护的项目名称。
     */
    private String customProjectName;

    /**
     * 是否项目工时 (is_project_timesheet)
     * 用以标识项目工时和非项目工时，此字段还用以后期是否参与工时统计。
     * 默认为 true
     */
    private boolean projectTimesheet;

    /**
     * 工时启用标识 (is_enabled)
     * 该工时是否处于启用状态，true为启用，false为停用。
     * 默认为 true
     */
    private boolean enabled;

    /**
     * 项目业务类型 (project_business_type)
     * 用以对项目按照业务进行分类，标记项目属于哪一类业务。
     * 关联 t_business_type.business_name
     */
    private String projectBusinessType;

    /**
     * 数据创建时间 (created_at)
     * 数据库自动生成默认值
     */
    private LocalDateTime createdAt;

    /**
     * 数据更新时间 (updated_at)
     * 应用程序维护
     */
    private LocalDateTime updatedAt;

    // --- 非数据库字段，用于关联查询或显示 ---
    /**
     * 项目业务类型的详细信息 (非数据库字段)
     * 需要联表查询获得
     */
    private transient BusinessType businessTypeInfo;

    // Lombok @Data 自动生成 getter/setter/toString/equals/hashCode
}
