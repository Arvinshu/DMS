/**
 * 目录: src/main/java/org/ls/dto/FileSyncStatusDto.java
 * 文件名: FileSyncStatusDto.java
 * 开发时间: 2025-04-29 10:04:40 EDT
 * 作者: Gemini
 * 用途: 数据传输对象 (DTO)，用于向前端传递文件同步功能的整体状态信息。
 */
package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件同步状态 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSyncStatusDto {

    /**
     * 后台目录监控是否活动
     */
    private boolean monitoringActive;

    /**
     * 待同步文件总数 (status='pending_sync')
     */
    private long totalPendingCount;

    /**
     * 已同步文件总数 (status='synced')
     */
    private long totalSyncedCount;

    /**
     * 错误状态文件总数 (status='error_copying' or 'error_syncing')
     */
    private long totalErrorCount;

    /**
     * 手动同步进程的当前状态 ('idle', 'running', 'paused', 'stopping')
     */
    private String syncProcessStatus;

    /**
     * (可选) 当前运行轮次中已成功处理的文件数
     */
    private int processedInCurrentRun;

    /**
     * (可选) 当前运行轮次中处理失败的文件数
     */
    private int failedInCurrentRun;

    // 可以根据需要添加更多状态信息，例如上次同步时间等

}
