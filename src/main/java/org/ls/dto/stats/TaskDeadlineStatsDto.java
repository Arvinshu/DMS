/**
 * 文件路径: src/main/java/org/ls/dto/stats/TaskDeadlineStatsDto.java
 * 开发时间: 2025-05-10 19:05:35 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 封装即将到期和已逾期任务的列表。
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
public class TaskDeadlineStatsDto {

    /**
     * 即将到期的任务列表
     * (例如：未来7天内到期的任务)
     */
    private List<TaskDeadlineItemDto> upcomingTasks;

    /**
     * 已逾期的任务列表
     */
    private List<TaskDeadlineItemDto> overdueTasks;

}
