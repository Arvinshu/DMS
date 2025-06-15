/**
 * 目录: src/main/java/org/ls/controller/api/FileManageApiController.java
 * 文件名: FileManageApiController.java
 * 开发时间: 2025-06-03 23:00:00 (Asia/Shanghai)
 * 作者: Gemini
 * 用途: 提供文件管理和同步相关的 RESTful API 端点。
 * 本次更新: 添加了全文搜索的API端点 /api/filemanage/fulltext/search。
 */
package org.ls.controller.api;

import jakarta.validation.Valid; // 引入 @Valid 进行请求体验证
import lombok.extern.slf4j.Slf4j;
import org.ls.dto.*;
import org.ls.service.FileManagementService;
import org.ls.service.FileSyncService;
import org.ls.service.FulltextSearchService; // 引入新的服务接口
// import org.ls.utils.StringUtils; // 如果需要，可以取消注释
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/filemanage")
@Slf4j
public class FileManageApiController {

    private final FileManagementService fileManagementService;
    private final FileSyncService fileSyncService;
    private final FulltextSearchService fulltextSearchService; // 新注入的服务

    @Autowired
    public FileManageApiController(FileManagementService fileManagementService,
                                   FileSyncService fileSyncService,
                                   FulltextSearchService fulltextSearchService) { // 构造函数注入
        this.fileManagementService = fileManagementService;
        this.fileSyncService = fileSyncService;
        this.fulltextSearchService = fulltextSearchService;
    }

    // --- Decrypted File Search and Download Endpoints (保持不变) ---

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
        log.debug("API 请求: 搜索解密文件。关键字: '{}', 页码: {}, 大小: {}", keyword, page, size);
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // 限制页面大小

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
            String decodedFilepath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            log.debug("解码后的文件目录: '{}', 解码后的文件名: '{}'", decodedFilepath, decodedFilename);

            Resource resource = fileManagementService.getDecryptedFileResource(decodedFilepath, decodedFilename);
            String encodedFilenameDisposition = URLEncoder.encode(decodedFilename, StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilenameDisposition)
                    .body(resource);
        } catch (FileNotFoundException e) {
            log.warn("请求下载的解密文件未找到: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("准备解密文件下载时出错: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    // --- Encrypted File Search and Download Endpoints (保持不变) ---

    /**
     * 搜索加密源目录中的文件（分页）
     * @param keyword 文件名关键字 (可选)
     * @param page 页码 (默认为 1)
     * @param size 每页大小 (默认为 100)
     * @return 分页结果 DTO
     */
    @GetMapping("/encrypted/search")
    public ResponseEntity<PageDto<DecryptedFileDto>> searchEncryptedFiles(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        log.debug("API 请求: 搜索加密文件。关键字: '{}', 页码: {}, 大小: {}", keyword, page, size);
        if (page < 1) page = 1; if (size < 1) size = 10; if (size > 100) size = 100;
        PageDto<DecryptedFileDto> result = fileManagementService.searchEncryptedFiles(keyword, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 下载指定的加密源文件
     * @param relativePath 文件相对路径 (URL-encoded)
     * @param filename 文件名 (URL-encoded)
     * @return 文件资源或 404 错误
     */
    @GetMapping("/encrypted/download")
    public ResponseEntity<Resource> downloadEncryptedFile(
            @RequestParam String relativePath,
            @RequestParam String filename) {
        log.debug("API 请求: 下载加密文件。相对路径: '{}', 原始文件名参数: '{}'", relativePath, filename);
        try {
            String decodedFilepath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            log.debug("解码后的加密文件路径: '{}', 解码后的加密文件名: '{}'", decodedFilepath, decodedFilename);

            Resource resource = fileManagementService.getEncryptedFileResource(decodedFilepath, decodedFilename);
            String encodedFilenameDisposition = URLEncoder.encode(decodedFilename, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilenameDisposition)
                    .body(resource);
        } catch (FileNotFoundException e) {
            log.warn("请求下载的加密文件未找到: Path='{}', Original Filename Param='{}', Decoded Attempt='{}'",
                    relativePath, filename, URLDecoder.decode(filename, StandardCharsets.UTF_8), e);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.error("下载请求中的文件名参数解码失败: '{}'", filename, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("准备加密文件下载时出错: Path='{}', Filename='{}'", relativePath, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- File Sync Management Endpoints (保持不变) ---
    /**
     * 确认并删除标记为 'pending_deletion' 的文件记录。
     * @param idsToDelete 包含要确认删除的记录 ID 的列表 (来自请求体)
     * @return 成功或失败的响应
     */
    @PostMapping("/sync/confirm-delete")
    public ResponseEntity<?> confirmAndDeleteSyncRecords(@RequestBody List<Long> idsToDelete) {
        log.info("API 请求: 确认删除 {} 条同步记录。", idsToDelete != null ? idsToDelete.size() : 0);
        if (CollectionUtils.isEmpty(idsToDelete)) {
            log.warn("确认删除请求收到的 ID 列表为空。");
            return ResponseEntity.badRequest().body(Map.of("message", "需要提供要删除的记录 ID。"));
        }
        try {
            fileSyncService.confirmAndDeleteFiles(idsToDelete);
            log.info("已成功处理 ID 列表的删除确认请求: {}", idsToDelete);
            return ResponseEntity.ok(Map.of("message", "删除确认请求已处理。"));
        } catch (Exception e) {
            log.error("处理删除确认请求时发生错误: IDs={}", idsToDelete, e);
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
        log.debug("API 请求: 获取待处理文件列表。页码: {}, 大小: {}", page, size);
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
        log.trace("API 请求: 获取同步状态。");
        FileSyncStatusDto status = fileSyncService.getSyncStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 启动手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/start")
    public ResponseEntity<FileSyncTaskControlResultDto> startSync() {
        log.info("API 请求: 启动手动同步。");
        FileSyncTaskControlResultDto result = fileSyncService.startManualSync();
        return ResponseEntity.ok(result);
    }

    /**
     * 暂停当前的手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/pause")
    public ResponseEntity<FileSyncTaskControlResultDto> pauseSync() {
        log.info("API 请求: 暂停手动同步。");
        FileSyncTaskControlResultDto result = fileSyncService.pauseManualSync();
        return ResponseEntity.ok(result);
    }

    /**
     * 恢复已暂停的手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/resume")
    public ResponseEntity<FileSyncTaskControlResultDto> resumeSync() {
        log.info("API 请求: 恢复手动同步。");
        FileSyncTaskControlResultDto result = fileSyncService.resumeManualSync();
        return ResponseEntity.ok(result);
    }

    /**
     * 停止当前的手动文件同步流程
     * @return 操作结果 DTO
     */
    @PostMapping("/sync/stop")
    public ResponseEntity<FileSyncTaskControlResultDto> stopSync() {
        log.info("API 请求: 停止手动同步。");
        FileSyncTaskControlResultDto result = fileSyncService.stopManualSync();
        return ResponseEntity.ok(result);
    }

    // --- 新增：全文搜索 API 端点 ---
    /**
     * 执行全文搜索。
     * @param requestDto 包含搜索条件、分页、筛选和排序信息的请求体。
     * @return 分页的全文搜索结果。
     */
    @PostMapping("/fulltext/search")
    public ResponseEntity<PageDto<FulltextSearchResultDto>> fulltextSearch(
            @Valid @RequestBody FulltextSearchRequestDto requestDto) { // 使用 @Valid 进行基本校验 (如果DTO中有校验注解)
        log.info("API 请求: 执行全文搜索。请求参数: {}", requestDto);
        try {
            // 参数校验 (虽然 @Valid 会做一些，但可以补充业务校验)
            if (requestDto.getPage() < 1) {
                requestDto.setPage(1);
            }
            if (requestDto.getSize() < 1) {
                requestDto.setSize(10); // 或者使用设计文档中定义的默认值 50
            }
            if (requestDto.getSize() > 200) { // 设置一个合理的上限
                requestDto.setSize(200);
            }

            PageDto<FulltextSearchResultDto> results = fulltextSearchService.search(requestDto);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // 更具体的异常可以在 FulltextSearchServiceImpl 中捕获并包装，或者在这里统一处理
            log.error("全文搜索 API 处理时发生错误: {}", e.getMessage(), e);
            // 返回一个表示错误的 PageDto，或者根据 GlobalExceptionHandler 的行为来决定
            // 这里我们假设 Service 层会处理大部分错误并返回合适的 PageDto，
            // 或者 GlobalExceptionHandler 会捕获并转换特定异常。
            // 如果 Service 层可能抛出未被 GlobalExceptionHandler 特定处理的运行时异常，
            // 则需要一个更通用的错误响应。
            // 为简单起见，如果 FulltextSearchService 抛出未捕获的异常，让 GlobalExceptionHandler 处理。
            // 如果 FulltextSearchService 设计为总是返回 PageDto（即使是空的或带错误信息的），则这里不需要额外的 catch。
            // 假设 service 层会处理好，或者 GlobalExceptionHandler 会捕获
            // 此处可以返回一个通用的错误响应，如果不想依赖 GlobalExceptionHandler 对所有 Exception 的处理
            return ResponseEntity.internalServerError()
                    .body(new PageDto<>(null, requestDto.getPage(), requestDto.getSize(), 0L)); // 返回空的错误分页
        }
    }
}
