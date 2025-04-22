package org.ls.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类：利润中心维护表 (t_profit_center)
 * 文件路径: src/main/java/org/ls/entity/ProfitCenter.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitCenter implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 利润中心全名 (zone)
     * 主键
     * 对 t_wkt.zone 去重后的数据写入该列,此列数据是从其他表同步过来，在页面只展示不维护。
     */
    private String zone;

    /**
     * 业务类型 (business_type)
     * 自动将 t_wkt.zone 去重后的数据按 "-" 号拆分后填入第 1 个数据，在页面只展示不维护。
     * 示例："基础业务"
     */
    private String businessType;

    /**
     * 区域分类 (region_category)
     * 自动将 t_wkt.zone 去重后的数据按 "-" 号拆分后填入第 2 个数据，在页面只展示不维护。
     * 示例："大区与网省中心"
     */
    private String regionCategory;

    /**
     * 大区名称 (region_name)
     * 自动将 t_wkt.zone 去重后的数据按 "-" 号拆分后填入第 3 个数据，在页面只展示不维护。
     * 示例："大区四"
     */
    private String regionName;

    /**
     * 中心名称 (center_name)
     * 自动将 t_wkt.zone 去重后的数据按 "-" 号拆分后填入第 4 个数据，在页面只展示不维护。
     * 示例："冀北中心"
     */
    private String centerName;

    /**
     * 业务子类 (business_subcategory)
     * 自动将 t_wkt.zone 去重后的数据按 "-" 号拆分后填入第 5 个数据，在页面只展示不维护。
     * 示例："业务"
     */
    private String businessSubcategory;

    /**
     * 部门名称 (department_name)
     * 自动将 t_wkt.zone 去重后的数据按 "-" 号拆分后填入第 6 个数据，在页面只展示不维护。
     * 示例："大营销咨询业务部"
     */
    private String departmentName; // 注意：数据库注释中提到的是第六个部分，需要确认拆分逻辑

    /**
     * 区域负责人 (responsible_person)
     * 部门维护字段
     */
    private String responsiblePerson;

    /**
     * 主要工作地点 (work_location)
     * 部门维护字段
     */
    private String workLocation;

    /**
     * 利润中心备注 (custom_zone_remark)
     * 利润中心全名长度不一，需要手工将利润中心名称截取一部分，部门自己维护的利润中心名称。
     */
    private String customZoneRemark;

    /**
     * 利润中心启用状态 (is_enabled)
     * true-启用, false-停用
     * 默认为 true
     */
    private boolean enabled;

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

    // Lombok @Data 自动生成 getter/setter/toString/equals/hashCode
}
