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
import java.net.URLDecoder; // 引入 URLDecoder
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets; // 引入 StandardCharsets
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
        log.debug("API 请求: 下载解密文件。相对路径: '{}', 文件名: '{}'", relativePath, filename);
        try {
            // --- 显式解码文件路径 ---
            String decodedFilepath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
            log.debug("解码后的文件目录: '{}'", decodedFilepath);

            // --- 显式解码文件名 ---
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            log.debug("解码后的文件名: '{}'", decodedFilename);

            Resource resource = fileManagementService.getDecryptedFileResource(decodedFilepath, decodedFilename); // 传递解码后的文件名
            String encodedFilenameDisposition = URLEncoder.encode(decodedFilename, StandardCharsets.UTF_8).replace("+", "%20"); // 对原始（解码后的）文件名进行编码用于 Content-Disposition

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilenameDisposition)
                    .body(resource);
        } catch (FileNotFoundException e) {
            log.warn("请求下载的解密文件未找到: Path='{}', Filename='{}' (Decoded='{}')", relativePath, filename, filename, e); // 日志中也记录解码尝试
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("准备解密文件下载时出错: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    // --- NEW Encrypted File Search and Download Endpoints ---

    /**
     * 搜索加密源目录中的文件（分页）
     * @param keyword 文件名关键字 (可选)
     * @param page 页码 (默认为 1)
     * @param size 每页大小 (默认为 100)
     * @return 分页结果 DTO
     */
    @GetMapping("/encrypted/search") // New path
    public ResponseEntity<PageDto<DecryptedFileDto>> searchEncryptedFiles( // New method name
                                                                           @RequestParam(required = false, defaultValue = "") String keyword,
                                                                           @RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "100") int size) {
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
    public ResponseEntity<Resource> downloadEncryptedFile(
            @RequestParam String relativePath,
            @RequestParam String filename) {
        log.debug("API 请求: 下载加密文件。相对路径: '{}', 原始文件名参数: '{}'", relativePath, filename);
        try {
            // --- 显式解码文件目录和文件名 ---
            // Spring 通常会自动解码 @RequestParam，但为了确保，我们再次解码。
            // 如果 Spring 已经解码，重复解码通常不会出错（对非 %xx 字符无影响）。
            String decodedFilepath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
            log.debug("解码后的加密文件路径: '{}'", decodedFilepath);

            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            log.debug("解码后的加密文件名: '{}'", decodedFilename);

            // 调用 Service 层，传递解码后的文件名
            Resource resource = fileManagementService.getEncryptedFileResource(decodedFilepath, decodedFilename);

            // 为 Content-Disposition 准备文件名，需要对原始（解码后的）文件名进行 URL 编码
            String encodedFilenameDisposition = URLEncoder.encode(decodedFilename, StandardCharsets.UTF_8)
                    // RFC 5987 建议将空格编码为 %20 而不是 +
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // 加密文件通常作为通用二进制流
                    // 设置 Content-Disposition 头，让浏览器知道这是附件并使用正确的文件名
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilenameDisposition)
                    .body(resource);
        } catch (FileNotFoundException e) {
            // 在日志中记录原始参数和尝试解码后的文件名
            log.warn("请求下载的加密文件未找到: Path='{}', Original Filename Param='{}', Decoded Attempt='{}'",
                    relativePath, filename, URLDecoder.decode(filename, StandardCharsets.UTF_8), e);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            // 如果 URLDecoder.decode 失败 (例如无效的编码)
            log.error("下载请求中的文件名参数解码失败: '{}'", filename, e);
//            return ResponseEntity.badRequest().body(Map.of("message", "无效的文件名参数编码。"));
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("准备加密文件下载时出错: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- File Sync Management Endpoints ---
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
     * @param size 每页大小 (默认为 100)
     * @return 分页结果 DTO
     */
    @GetMapping("/pending")
    public ResponseEntity<PageDto<PendingFileSyncDto>> getPendingFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
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
