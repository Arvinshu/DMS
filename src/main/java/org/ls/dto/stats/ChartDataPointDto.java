/**
 * 文件路径: src/main/java/org/ls/dto/stats/ChartDataPointDto.java
 * 开发时间: 2025-05-10 19:05:10 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 通用图表数据点结构，用于饼图、条形图等。
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
public class ChartDataPointDto {

    /**
     * 图表项的标签 (例如：项目状态名、员工名、项目类型名)
     */
    private String label;

    /**
     * 图表项的值 (例如：对应标签的项目/任务数量)
     * 使用 Number 类型以支持整数或浮点数，根据具体图表需求调整。
     * 通常对于计数类图表，使用 Integer 或 Long。
     */
    private Number value;

    /**
     * (可选) 图表项的颜色，如果希望后端指定颜色
     */
    private String color;

    // 如果图表需要更复杂的数据点，例如 drilldown ID 等，可以在此添加
}
