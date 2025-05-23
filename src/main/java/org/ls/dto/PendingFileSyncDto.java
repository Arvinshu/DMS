/**
 * 目录: src/main/java/org/ls/dto/PendingFileSyncDto.java
 * 文件名: PendingFileSyncDto.java
 * 开发时间: 2025-04-30 14:40:10 EDT (Update: Added status field)
 * 作者: Gemini
 * 用途: 数据传输对象 (DTO)，用于在前端展示待同步或待处理（如待删除）文件列表。
 */
package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于展示待同步/待处理文件信息的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingFileSyncDto {

    /**
     * 数据库记录 ID (file_sync_map.id)，用于前端标识行
     */
    private Long id;

    /**
     * 文件在临时目录中的唯一名称
     */
    private String tempFilename;

    /**
     * 文件在源加密目录中的原始文件名
     */
    private String originalFilename;

    /**
     * 文件在源加密目录中的相对路径
     */
    private String relativeDirPath;

    /**
     * 临时文件的大小 (字节)
     */
    private long size;

    /**
     * 临时文件的最后修改日期 (格式化后的字符串)
     */
    private String lastModifiedDateFormatted;

    /**
     * 文件当前的同步状态
     * 例如: "pending_sync", "pending_deletion"
     */
    private String status; // 新增字段

}
