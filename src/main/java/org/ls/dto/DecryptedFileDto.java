/**
 * 目录: src/main/java/org/ls/dto/DecryptedFileDto.java
 * 文件名: DecryptedFileDto.java
 * 开发时间: 2025-04-29 10:04:10 EDT
 * 作者: Gemini
 * 用途: 数据传输对象 (DTO)，用于在前端展示解密目录中文件的搜索结果。
 */
package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于展示解密后文件信息的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecryptedFileDto {

    /**
     * 文件名 (不含路径)
     */
    private String filename;

    /**
     * 文件相对于解密目录根目录的相对路径
     * 例如: "project_a/reports/" 或 ""
     */
    private String relativePath;

    /**
     * 文件大小 (字节)
     */
    private long size;

    /**
     * 文件最后修改日期 (格式化后的字符串，例如 "yyyy-MM-dd HH:mm:ss")
     */
    private String lastModifiedDateFormatted;

}
