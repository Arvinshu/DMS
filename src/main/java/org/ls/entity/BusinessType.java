package org.ls.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类：业务类型表 (t_business_type)
 * 文件路径: src/main/java/org/ls/entity/BusinessType.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键 (id)
     */
    private Integer id;

    /**
     * 业务类别 (business_category)
     */
    private String businessCategory;

    /**
     * 业务名称 (business_name)
     * 描述业务名称信息。
     * 具有唯一约束。
     */
    private String businessName;

    /**
     * 业务内容描述 (business_description)
     */
    private String businessDescription;

    /**
     * 业务是否启用 (is_enabled)
     * 业务当前是否处于活动状态: true-启用, false-停用
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
