/**
 * 文件路径: src/main/java/org/ls/entity/Task.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 任务实体类，映射数据库表 t_task
 */
package org.ls.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;      // 用于 DATE 类型
import java.time.OffsetDateTime; // 用于 TIMESTAMP WITH TIME ZONE 类型

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    /**
     * 任务唯一标识符 (对应数据库 task_id, BIGSERIAL PRIMARY KEY)
     */
    private Long taskId;

    /**
     * 该任务所属的项目ID (对应数据库 project_id, BIGINT NOT NULL)
     * 关联 t_project.project_id
     */
    private Long projectId;

    /**
     * 任务名称 (对应数据库 task_name, VARCHAR(255) NOT NULL)
     */
    private String taskName;

    /**
     * 任务描述 (对应数据库 task_description, TEXT)
     */
    private String taskDescription;

    /**
     * 任务优先级 (对应数据库 priority, VARCHAR(20) DEFAULT 'Medium')
     * 例如: 'High', 'Medium', 'Low'
     */
    private String priority;

    /**
     * 任务分配给的员工 (工号+姓名) (对应数据库 assignee_employee, VARCHAR(30))
     * 关联 t_employee.employee
     */
    private String assigneeEmployee;

    /**
     * 任务计划开始日期 (对应数据库 start_date, DATE)
     */
    private LocalDate startDate;

    /**
     * 任务计划截止日期 (对应数据库 due_date, DATE)
     */
    private LocalDate dueDate;

    /**
     * 任务当前所属的项目阶段ID (对应数据库 stage_id, INTEGER NOT NULL)
     * 关联 t_project_stage.stage_id
     */
    private Integer stageId;

    /**
     * 任务的当前状态 (对应数据库 task_status, VARCHAR(20) DEFAULT '待办' NOT NULL)
     * 例如: '待办', '进行中', '已完成', '已暂停', '已取消'
     */
    private String taskStatus;

    /**
     * 与任务相关的附件信息 (对应数据库 attachments, TEXT)
     * 简单存储，如JSON或逗号分隔的文件名列表
     */
    private String attachments;

    /**
     * 记录创建时间 (对应数据库 created_at, TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
     */
    private OffsetDateTime createdAt;

    /**
     * 记录最后更新时间 (对应数据库 updated_at, TIMESTAMP WITH TIME ZONE)
     * 用于计算项目状态/阶段
     */
    private OffsetDateTime updatedAt;
}
