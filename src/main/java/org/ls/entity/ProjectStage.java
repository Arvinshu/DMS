/**
 * 文件路径: src/main/java/org/ls/entity/ProjectStage.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目阶段实体类，映射数据库表 t_project_stage
 */
package org.ls.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime; // 使用 OffsetDateTime 对应 TIMESTAMP WITH TIME ZONE

@Data // Lombok 注解：自动生成 getter, setter, toString, equals, hashCode
@Builder // Lombok 注解：构造者模式
@NoArgsConstructor // Lombok 注解：无参构造函数
@AllArgsConstructor // Lombok 注解：全参构造函数
public class ProjectStage {

    /**
     * 阶段唯一标识符 (对应数据库 stage_id, SERIAL PRIMARY KEY)
     */
    private Integer stageId;

    /**
     * 阶段排序号 (对应数据库 stage_order, INTEGER NOT NULL)
     * 用于前端显示的排序顺序
     */
    private Integer stageOrder;

    /**
     * 阶段名称 (对应数据库 stage_name, VARCHAR(100) NOT NULL UNIQUE)
     */
    private String stageName;

    /**
     * 阶段描述 (对应数据库 stage_description, TEXT)
     */
    private String stageDescription;

    /**
     * 是否可用 (对应数据库 is_enabled, BOOLEAN NOT NULL DEFAULT TRUE)
     * 标识该阶段是否在系统中启用
     */
    private Boolean isEnabled;

    /**
     * 记录创建时间 (对应数据库 created_at, TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
     * 由数据库自动生成
     */
    private OffsetDateTime createdAt;

    /**
     * 记录最后更新时间 (对应数据库 updated_at, TIMESTAMP WITH TIME ZONE)
     * 由应用程序在更新时维护
     */
    private OffsetDateTime updatedAt;
}
