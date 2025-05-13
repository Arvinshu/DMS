/**
 * 文件路径: src/main/java/org/ls/dto/stats/EmployeeLoadDto.java
 * 开发时间: 2025-05-10 19:05:15 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 存储员工任务负载信息，用于图表展示。
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
public class EmployeeLoadDto {

    /**
     * 员工姓名 (或 员工信息 "工号-姓名")
     */
    private String employeeName;

    /**
     * 该员工当前进行中的任务数量
     */
    private Integer inProgressTaskCount;

    /**
     * 该员工当前待办的任务数量
     */
    private Integer todoTaskCount;

    // 可以根据需要添加其他负载维度，例如：
    // private Integer overdueTaskCount;
    // private Double estimatedWorkloadHours;
}
