/**
 * 目录: src/main/java/org/ls/entity/FileSyncMap.java
 * 文件名: FileSyncMap.java
 * 开发时间: 2025-04-29 10:00:00 EDT
 * 作者: Gemini
 * 用途: 文件同步映射表 (file_sync_map) 的实体类，用于表示文件同步过程中的记录。
 */
package org.ls.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件同步映射实体类
 */
@Data // Lombok annotation to generate getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok annotation for no-args constructor
@AllArgsConstructor // Lombok annotation for all-args constructor
public class FileSyncMap {

    /**
     * 唯一主键ID (对应数据库 SERIAL PRIMARY KEY)
     */
    private Long id;

    /**
     * 源文件相对于加密目录根目录的相对路径
     * 例如: "projects/alpha/docs/" 或 "" (表示根目录)
     */
    private String relativeDirPath;

    /**
     * 源文件在加密目录中的原始文件名
     * 例如: "report-final.enc"
     */
    private String originalFilename;

    /**
     * 文件在临时目录中的唯一名称 (可能带后缀)
     * 例如: "report-final.enc" 或 "report-final_1.enc"
     */
    private String tempFilename;

    /**
     * 文件同步状态
     * 可能的值: "pending_sync", "synced", "error_copying", "error_syncing", "syncing"
     */
    private String status;

    /**
     * 记录最后更新时间戳
     */
    private LocalDateTime lastUpdated;

}
