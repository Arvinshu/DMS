/**
 * 目录: src/main/java/org/ls/dto/FileSyncTaskControlResultDto.java
 * 文件名: FileSyncTaskControlResultDto.java
 * 开发时间: 2025-04-29 10:04:50 EDT
 * 作者: Gemini
 * 用途: 数据传输对象 (DTO)，用于返回手动同步任务控制操作（启动、暂停、恢复、停止）的结果。
 */
package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件同步任务控制结果 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileSyncTaskControlResultDto {

    /**
     * 操作是否成功接收或执行
     */
    private boolean success;

    /**
     * 相关消息 (例如 "同步已启动", "无效操作", "停止信号已发送")
     */
    private String message;

    /**
     * 操作后同步进程的新状态 ('running', 'paused', 'idle', 'stopping')
     * (可选，方便前端更新状态)
     */
    private String newStatus;

}
