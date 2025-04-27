/**
 * 文件路径: src/main/java/org/ls/entity/ProjectTag.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目标签实体类，映射数据库表 t_project_tag
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
public class ProjectTag {

    /**
     * 标签唯一标识符 (对应数据库 tag_id, BIGSERIAL PRIMARY KEY)
     */
    private Long tagId;

    /**
     * 标签名称 (对应数据库 tag_name, VARCHAR(100) NOT NULL UNIQUE)
     * 用于项目分类和搜索
     */
    private String tagName;

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
