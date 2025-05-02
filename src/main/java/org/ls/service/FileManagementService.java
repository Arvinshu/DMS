/**
 * 目录: src/main/java/org/ls/service/FileManagementService.java
 * 文件名: FileManagementService.java
 * 开发时间: 2025-04-30 13:45:10 EDT (Update: Added methods for encrypted files)
 * 作者: Gemini
 * 用途: 文件管理服务接口，定义了查询和下载解密后文件及加密源文件的操作。
 */
package org.ls.service;

import org.ls.dto.DecryptedFileDto; // Reuse DTO for simplicity, represents source file info here
import org.ls.dto.PageDto;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;

public interface FileManagementService {

    /**
     * 在解密目录中搜索文件。 (保留供未来使用)
     *
     * @param keyword    文件名关键字 (模糊匹配)
     * @param page       页码 (从 1 开始)
     * @param size       每页大小
     * @return 分页的文件信息 DTO
     */
    PageDto<DecryptedFileDto> searchDecryptedFiles(String keyword, int page, int size);

    /**
     * 获取用于下载的解密文件资源。 (保留供未来使用)
     *
     * @param relativePath 文件相对于解密目录的相对路径
     * @param filename     文件名
     * @return Spring Resource 对象，代表文件内容
     * @throws FileNotFoundException 如果文件未找到或无法访问，或者路径无效
     */
    Resource getDecryptedFileResource(String relativePath, String filename) throws FileNotFoundException;

    // --- NEW Methods for Encrypted Source Files ---

    /**
     * 在加密源目录中搜索文件。
     *
     * @param keyword    文件名关键字 (模糊匹配)
     * @param page       页码 (从 1 开始)
     * @param size       每页大小
     * @return 分页的文件信息 DTO (复用 DecryptedFileDto 结构)
     */
    PageDto<DecryptedFileDto> searchEncryptedFiles(String keyword, int page, int size);

    /**
     * 获取用于下载的加密源文件资源。
     *
     * @param relativePath 文件相对于加密目录的相对路径
     * @param filename     文件名
     * @return Spring Resource 对象，代表文件内容
     * @throws FileNotFoundException 如果文件未找到或无法访问，或者路径无效
     */
    Resource getEncryptedFileResource(String relativePath, String filename) throws FileNotFoundException;

}
