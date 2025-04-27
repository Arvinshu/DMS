/**
 * 文件路径: src/main/java/org/ls/entity/Project.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 项目实体类，映射数据库表 t_project
 */
package org.ls.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
// 注意：这里不直接包含 List<ProjectTag>，标签关联将在 Service 层或 DTO 中处理

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    /**
     * 项目唯一标识符 (对应数据库 project_id, BIGSERIAL PRIMARY KEY)
     */
    private Long projectId;

    /**
     * 项目名称 (对应数据库 project_name, VARCHAR(255) NOT NULL UNIQUE)
     */
    private String projectName;

    /**
     * 项目描述 (对应数据库 project_description, TEXT)
     */
    private String projectDescription;

    /**
     * 项目业务类型名称 (对应数据库 business_type_name, VARCHAR(200))
     * 关联 t_business_type.business_name
     */
    private String businessTypeName;

    /**
     * 项目利润中心区域代码 (对应数据库 profit_center_zone, VARCHAR(200))
     * 关联 t_profit_center.zone
     */
    private String profitCenterZone;

    /**
     * 项目负责人员工信息 (工号+姓名) (对应数据库 project_manager_employee, VARCHAR(30))
     * 关联 t_employee.employee
     */
    private String projectManagerEmployee;

    /**
     * 项目工时代码 (对应数据库 ts_bm, VARCHAR(200))
     * 关联 t_timesheet_code.ts_bm
     */
    private String tsBm; // 字段名与数据库列名对应

    /**
     * 记录创建时间 (对应数据库 created_at, TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
     */
    private OffsetDateTime createdAt;

    /**
     * 记录最后更新时间 (对应数据库 updated_at, TIMESTAMP WITH TIME ZONE)
     */
    private OffsetDateTime updatedAt;

    /**
     * 创建该记录的员工 (工号+姓名) (对应数据库 created_by, VARCHAR(30))
     * 关联 t_employee.employee
     */
    private String createdBy;

    /**
     * 最后更新该记录的员工 (工号+姓名) (对应数据库 updated_by, VARCHAR(30))
     * 关联 t_employee.employee
     */
    private String updatedBy;

    // 注意：计算字段如 projectStatus, currentStageName 和关联列表如 tags 不在此实体中定义
}
