/**
 * 目录: src/main/java/org/ls/service/FileSyncService.java
 * 文件名: FileSyncService.java
 * 开发时间: 2025-04-29 10:08:10 EDT
 * 作者: Gemini
 * 用途: 文件同步服务接口，定义后台监控管理、状态查询、待同步文件查询以及手动同步控制操作。
 */
package org.ls.service;

import org.ls.dto.FileSyncStatusDto;
import org.ls.dto.FileSyncTaskControlResultDto;
import org.ls.dto.PageDto;
import org.ls.dto.PendingFileSyncDto;

public interface FileSyncService {

    /**
     * 获取待同步的文件列表（分页）。
     *
     * @param page 页码 (从 1 开始)
     * @param size 每页大小
     * @return 分页的待同步文件信息 DTO
     */
    PageDto<PendingFileSyncDto> getPendingSyncFiles(int page, int size);

    /**
     * 获取当前文件同步服务的整体状态。
     *
     * @return 文件同步状态 DTO
     */
    FileSyncStatusDto getSyncStatus();

    /**
     * 启动手动同步流程（异步执行）。
     * 如果已经在运行或暂停中，则可能返回失败或特定消息。
     *
     * @return 操作结果 DTO
     */
    FileSyncTaskControlResultDto startManualSync();

    /**
     * 暂停当前正在运行的手动同步流程。
     * 如果未在运行，则返回失败或特定消息。
     *
     * @return 操作结果 DTO
     */
    FileSyncTaskControlResultDto pauseManualSync();

    /**
     * 恢复已暂停的手动同步流程。
     * 如果未暂停，则返回失败或特定消息。
     *
     * @return 操作结果 DTO
     */
    FileSyncTaskControlResultDto resumeManualSync();

    /**
     * 请求停止当前正在运行或暂停的手动同步流程。
     * 停止是异步的，此方法仅发送停止信号。
     *
     * @return 操作结果 DTO
     */
    FileSyncTaskControlResultDto stopManualSync();

    // 后台监控的启动和停止通过 Spring Bean 生命周期管理 (PostConstruct/PreDestroy)
    // 因此不需要显式的 start/stop monitoring 方法。

}
