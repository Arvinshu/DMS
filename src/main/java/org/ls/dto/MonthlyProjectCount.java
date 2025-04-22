package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO 用于封装按月统计的项目数量。
 * 文件路径: src/main/java/org/ls/dto/MonthlyProjectCount.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyProjectCount {

    /**
     * 月份 (1 到 12)。
     */
    private int month;

    /**
     * 该月份对应的去重项目数量。
     */
    private long projectCount;

}
