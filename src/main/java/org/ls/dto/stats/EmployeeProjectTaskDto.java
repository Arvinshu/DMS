/**
 * 文件路径: src/main/java/org/ls/dto/stats/EmployeeProjectTaskDto.java
 * 开发时间: 2025-05-10 19:05:45 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 存储按项目分组的员工任务列表。
 */
package org.ls.dto.stats;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProjectTaskDto {

    /**
     * 项目的唯一标识符
     */
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 该员工在此项目下负责的任务列表
     */
    private List<EmployeeTaskSummaryDto> tasks;

    // 可以添加项目相关的其他摘要信息，如果前端需要
    // private String projectStatus;
    // private String projectOverallProgress;
}
