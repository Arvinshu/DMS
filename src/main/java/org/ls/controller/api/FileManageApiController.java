/**
 * 目录: src/main/java/org/ls/controller/api/FileManageApiController.java
 * 文件名: FileManageApiController.java
 * 开发时间: 2025-04-30 14:35:10 EDT (Update: Added confirm-delete endpoint)
 * 作者: Gemini
 * 用途: 提供文件管理和同步相关的 RESTful API 端点。
 */
package org.ls.controller.api;

import lombok.extern.slf4j.Slf4j;
import org.ls.dto.*;
import org.ls.service.FileManagementService;
import org.ls.service.FileSyncService;
// import org.ls.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils; // Import CollectionUtils
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List; // Import List
import java.util.Map; // Import Map for simple response

@RestController
@RequestMapping("/api/filemanage")
@Slf4j
public class FileManageApiController {

    private final FileManagementService fileManagementService;
    private final FileSyncService fileSyncService;

    @Autowired
    public FileManageApiController(FileManagementService fileManagementService, FileSyncService fileSyncService) {
        this.fileManagementService = fileManagementService;
        this.fileSyncService = fileSyncService;
    }

    // --- Decrypted File Search and Download Endpoints ---

    /**
     * 搜索解密目录中的文件（分页）
     * @param keyword 文件名关键字 (可选)
     * @param page 页码 (默认为 1)
     * @param size 每页大小 (默认为 10)
     * @return 分页结果 DTO
     */
    @GetMapping("/decrypted/search")
    public ResponseEntity<PageDto<DecryptedFileDto>> searchDecryptedFiles(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("API Request: Search decrypted files. Keyword: '{}', Page: {}, Size: {}", keyword, page, size);
        // Basic validation for page and size
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // Limit page size

        PageDto<DecryptedFileDto> result = fileManagementService.searchDecryptedFiles(keyword, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 下载指定的解密文件
     * @param relativePath 文件相对路径 (URL-encoded)
     * @param filename 文件名 (URL-encoded)
     * @return 文件资源或 404 错误
     */
    @GetMapping("/decrypted/download")
    public ResponseEntity<Resource> downloadDecryptedFile(
            @RequestParam String relativePath,
            @RequestParam String filename) {
        log.debug("API Request: Download decrypted file. RelativePath: '{}', Filename: '{}'", relativePath, filename);
        try {
            // URL Decoding is usually handled by Spring automatically, but double-check if issues arise.
            // String decodedPath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
            // String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

            Resource resource = fileManagementService.getDecryptedFileResource(relativePath, filename);

            // Encode filename for Content-Disposition header to handle special characters
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Set appropriate content type if known, otherwise use generic stream
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename) // RFC 5987 compliant
                    .body(resource);
        } catch (FileNotFoundException e) {
            log.warn("File not found for download request: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error preparing file download: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.internalServerError().build(); // Or handle specific exceptions
        }
    }


    // --- NEW Encrypted File Search and Download Endpoints ---

    /**
     * 搜索加密源目录中的文件（分页）
     * @param keyword 文件名关键字 (可选)
     * @param page 页码 (默认为 1)
     * @param size 每页大小 (默认为 10)
     * @return 分页结果 DTO
     */
    @GetMapping("/encrypted/search") // New path
    public ResponseEntity<PageDto<DecryptedFileDto>> searchEncryptedFiles( // New method name
                                                                           @RequestParam(required = false, defaultValue = "") String keyword,
                                                                           @RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "10") int size) {
        log.debug("API Request: Search ENCRYPTED files. Keyword: '{}', Page: {}, Size: {}", keyword, page, size);
        if (page < 1) page = 1; if (size < 1) size = 10; if (size > 100) size = 100;
        PageDto<DecryptedFileDto> result = fileManagementService.searchEncryptedFiles(keyword, page, size); // Call new service method
        return ResponseEntity.ok(result);
    }

    /**
     * 下载指定的加密源文件
     * @param relativePath 文件相对路径 (URL-encoded)
     * @param filename 文件名 (URL-encoded)
     * @return 文件资源或 404 错误
     */
    @GetMapping("/encrypted/download") // New path
    public ResponseEntity<Resource> downloadEncryptedFile( // New method name
                                                           @RequestParam String relativePath,
                                                           @RequestParam String filename) {
        log.debug("API Request: Download ENCRYPTED file. RelativePath: '{}', Filename: '{}'", relativePath, filename);
        try {
            Resource resource = fileManagementService.getEncryptedFileResource(relativePath, filename); // Call new service method
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Assume encrypted files are generic stream
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(resource);
        } catch (FileNotFoundException e) {
            log.warn("Encrypted file not found for download request: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error preparing encrypted file download: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    // --- File Sync Management Endpoints ---

    // --- NEW Endpoint for Confirming Deletions ---

    /**
     * 确认并删除标记为 'pending_deletion' 的文件记录。
     * @param idsToDelete 包含要确认删除的记录 ID 的列表 (来自请求体)
     * @return 成功或失败的响应
     */
    @PostMapping("/sync/confirm-delete")
    public ResponseEntity<?> confirmAndDeleteSyncRecords(@RequestBody List<Long> idsToDelete) {
        log.info("API 请求: 确认删除 {} 条同步记录。", idsToDelete != null ? idsToDelete.size() : 0);

        // 基本验证
        if (CollectionUtils.isEmpty(idsToDelete)) {
            log.warn("确认删除请求收到的 ID 列表为空。");
            // 返回 Bad Request 或 OK 但带消息
            return ResponseEntity.badRequest().body(Map.of("message", "需要提供要删除的记录 ID。"));
        }

        try {
            // 调用 Service 层处理删除逻辑
            fileSyncService.confirmAndDeleteFiles(idsToDelete);
            log.info("已成功处理 ID 列表的删除确认请求: {}", idsToDelete);
            // 返回成功响应 (可以包含更详细的结果，如果 Service 返回的话)
            return ResponseEntity.ok(Map.of("message", "删除确认请求已处理。"));
        } catch (Exception e) {
            // 捕获 Service 层可能抛出的异常
            log.error("处理删除确认请求时发生错误: IDs={}", idsToDelete, e);
            // 返回服务器内部错误
            return ResponseEntity.internalServerError().body(Map.of("message", "处理删除请求时发生内部错误。"));
        }
    }

    /**
     * 获取待同步的文件列表（分页）
     * @param page 页码 (默认为 1)
     * @param size 每页大小 (默认为 10)
     * @return 分页结果 DTO
     */
    @GetMapping("/pending")
    public ResponseEntity<PageDto<PendingFileSyncDto>> getPendingFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("API Request: Get pending sync files. Page: {}, Size: {}", page, size);
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        PageDto<PendingFileSyncDto> result = fileSyncService.getPendingSyncFiles(page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取文件同步服务的当前状态
     * @return 同步状态 DTO
     */
    @GetMapping("/sync/status")
    public ResponseEntity<FileSyncStatusDto> getSyncStatus() {
        log.trace("API Request: Get sync status."); // Use trace for frequent calls
        FileSyncStatusDto status = fileSyncService.getSyncStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 启动手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/start")
    public ResponseEntity<FileSyncTaskControlResultDto> startSync() {
        log.info("API Request: Start manual sync.");
        FileSyncTaskControlResultDto result = fileSyncService.startManualSync();
        return ResponseEntity.ok(result);
    }

    /**
     * 暂停当前的手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/pause")
    public ResponseEntity<FileSyncTaskControlResultDto> pauseSync() {
        log.info("API Request: Pause manual sync.");
        FileSyncTaskControlResultDto result = fileSyncService.pauseManualSync();
        return ResponseEntity.ok(result);
    }

    /**
     * 恢复已暂停的手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/resume")
    public ResponseEntity<FileSyncTaskControlResultDto> resumeSync() {
        log.info("API Request: Resume manual sync.");
        FileSyncTaskControlResultDto result = fileSyncService.resumeManualSync();
        return ResponseEntity.ok(result);
    }

    /**
     * 停止当前的手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/stop")
    public ResponseEntity<FileSyncTaskControlResultDto> stopSync() {
        log.info("API Request: Stop manual sync.");
        FileSyncTaskControlResultDto result = fileSyncService.stopManualSync();
        return ResponseEntity.ok(result);
    }
}
