/**
 * 文件路径: src/main/java/org/ls/dto/stats/AtRiskProjectDto.java
 * 开发时间: 2025-05-10 19:05:25 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 存储风险项目的信息。
 */
package org.ls.dto.stats;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtRiskProjectDto {

    /**
     * 项目的唯一标识符
     */
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目经理 (通常是 "工号-姓名" 格式)
     */
    private String projectManager;

    /**
     * 风险等级 (例如："高", "中", "低")
     */
    private String riskLevel;

    /**
     * 该项目下逾期任务的数量
     */
    private Integer overdueTaskCount;

    /**
     * 主要风险点的描述 (由后端根据规则自动生成)
     * 例如："存在3个逾期任务; 未来3天内有1个高优任务'X任务'即将到期且未开始。"
     */
    private String riskDescription;

    // 可以添加其他与风险评估相关的字段
    // 例如：
    // private Integer upcomingHighPriorityTasks; // 即将到期的高优先级任务数
    // private String lastActivityDate; // 项目最后活动日期，用于判断是否停滞
}
