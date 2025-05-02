/**
 * 目录: src/main/java/org/ls/service/impl/FileManagementServiceImpl.java
 * 文件名: FileManagementServiceImpl.java
 * 开发时间: 2025-04-30 13:55:00 EDT (Final Version with Encrypted/Decrypted methods)
 * 作者: Gemini
 * 用途: 文件管理服务实现类，负责处理解密目录和加密源目录的文件搜索和下载。
 */
package org.ls.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.ls.dto.DecryptedFileDto; // 复用 DTO
import org.ls.dto.PageDto;
import org.ls.service.FileManagementService;
import org.ls.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Spring StringUtils

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j // 使用 Lombok 添加日志记录 (SLF4J)
public class FileManagementServiceImpl implements FileManagementService {

    private final Path sourceDirectory; // 加密源文件目录
    private final Path targetDirectory; // 解密目标文件目录

    /**
     * 构造函数，注入 Environment 以获取配置属性。
     * @param env Spring Environment 对象
     */
    @Autowired
    public FileManagementServiceImpl(Environment env) {
        // 获取并验证加密源目录路径
        String sourceDirPath = env.getProperty("file.sync.source-dir");
        if (!StringUtils.hasText(sourceDirPath)) {
            log.error("配置属性 'file.sync.source-dir' 未设置。");
            throw new IllegalStateException("必需的目录路径 'file.sync.source-dir' 未配置。");
        }
        this.sourceDirectory = Paths.get(sourceDirPath).toAbsolutePath();
        validateDirectory(this.sourceDirectory, "file.sync.source-dir", true, false); // 源目录必须存在，需要可读

        // 获取并验证解密目标目录路径
        String targetDirPath = env.getProperty("file.sync.target-dir");
        if (!StringUtils.hasText(targetDirPath)) {
            log.error("配置属性 'file.sync.target-dir' 未设置。");
            throw new IllegalStateException("必需的目录路径 'file.sync.target-dir' 未配置。");
        }
        this.targetDirectory = Paths.get(targetDirPath).toAbsolutePath();
        validateDirectory(this.targetDirectory, "file.sync.target-dir", false, true); // 目标目录不存在时创建，需要可写

        log.info("FileManagementService 初始化完成。");
        log.info("加密源目录 (Source Directory): {}", this.sourceDirectory);
        log.info("解密目标目录 (Target Directory): {}", this.targetDirectory);
    }

    /**
     * 辅助方法，用于验证目录路径的有效性。
     * @param path 要验证的路径
     * @param propertyKey 配置属性键名 (用于日志)
     * @param mustExist 是否必须存在
     * @param checkWritable 是否需要检查写权限
     */
    private void validateDirectory(Path path, String propertyKey, boolean mustExist, boolean checkWritable) {
        try {
            if (mustExist) {
                // 检查目录是否存在且是目录
                if (!Files.exists(path) || !Files.isDirectory(path)) {
                    log.error("目录 '{}' 不存在或不是一个有效的目录: {}", propertyKey, path);
                    throw new IllegalStateException("目录 '" + propertyKey + "' 必须存在且是一个有效的目录。");
                }
                // 检查读权限
                if (!Files.isReadable(path)) {
                    log.error("目录 '{}' 不可读: {}", propertyKey, path);
                    throw new IllegalStateException("目录 '" + propertyKey + "' 必须可读。");
                }
            } else {
                // 如果允许创建，检查是否存在，不存在则创建
                if (!Files.exists(path)) {
                    Files.createDirectories(path); // 创建目录及其父目录
                    log.info("为 '{}' 创建了目录: {}", propertyKey, path);
                } else if (!Files.isDirectory(path)) {
                    // 如果路径存在但不是目录，则报错
                    log.error("为 '{}' 配置的路径不是一个目录: {}", propertyKey, path);
                    throw new IllegalStateException("为 '" + propertyKey + "' 配置的路径不是一个目录。");
                }
            }
            // 检查写权限
            if (checkWritable && !Files.isWritable(path)) {
                log.error("目录 '{}' 不可写: {}", propertyKey, path);
                throw new IllegalStateException("目录 '" + propertyKey + "' 必须可写。");
            }
        } catch (IOException e) {
            log.error("访问或创建目录 '{}' 时失败: {}", propertyKey, path, e);
            throw new IllegalStateException("初始化目录路径 '" + propertyKey + "' 失败。", e);
        } catch (SecurityException se) {
            log.error("访问目录 '{}' 时出现安全权限问题: {}", propertyKey, path, se);
            throw new IllegalStateException("访问目录 '" + propertyKey + "' 时权限不足。", se);
        }
    }

    // --- 用于解密文件 (Target Directory) 的方法 (保留) ---

    /**
     * 在解密目录中搜索文件。
     * @param keyword 文件名关键字 (模糊匹配)
     * @param page 页码 (从 1 开始)
     * @param size 每页大小
     * @return 分页的文件信息 DTO
     */
    @Override
    public PageDto<DecryptedFileDto> searchDecryptedFiles(String keyword, int page, int size) {
        log.debug("搜索解密文件 (Target Dir)。关键字: '{}', 页码: {}, 大小: {}", keyword, page, size);
        // 调用内部辅助方法，传入目标目录
        return searchFilesInternal(this.targetDirectory, keyword, page, size);
    }

    /**
     * 获取用于下载的解密文件资源。
     * @param relativePath 文件相对于解密目录的相对路径
     * @param filename 文件名
     * @return Spring Resource 对象
     * @throws FileNotFoundException 如果文件未找到或无效
     */
    @Override
    public Resource getDecryptedFileResource(String relativePath, String filename) throws FileNotFoundException {
        log.debug("请求解密文件资源 (Target Dir)。相对路径: '{}', 文件名: '{}'", relativePath, filename);
        // 调用内部辅助方法，传入目标目录
        return getFileResourceInternal(this.targetDirectory, relativePath, filename);
    }

    // --- 用于加密文件 (Source Directory) 的新方法 ---

    /**
     * 在加密源目录中搜索文件。
     * @param keyword 文件名关键字 (模糊匹配)
     * @param page 页码 (从 1 开始)
     * @param size 每页大小
     * @return 分页的文件信息 DTO (复用 DecryptedFileDto 结构)
     */
    @Override
    public PageDto<DecryptedFileDto> searchEncryptedFiles(String keyword, int page, int size) {
        log.debug("搜索加密文件 (Source Dir)。关键字: '{}', 页码: {}, 大小: {}", keyword, page, size);
        // 调用内部辅助方法，传入源目录
        return searchFilesInternal(this.sourceDirectory, keyword, page, size);
    }

    /**
     * 获取用于下载的加密源文件资源。
     * @param relativePath 文件相对于加密目录的相对路径
     * @param filename 文件名
     * @return Spring Resource 对象
     * @throws FileNotFoundException 如果文件未找到或无效
     */
    @Override
    public Resource getEncryptedFileResource(String relativePath, String filename) throws FileNotFoundException {
        log.debug("请求加密文件资源 (Source Dir)。相对路径: '{}', 文件名: '{}'", relativePath, filename);
        // 调用内部辅助方法，传入源目录
        return getFileResourceInternal(this.sourceDirectory, relativePath, filename);
    }


    // --- 内部文件操作辅助方法 ---

    /**
     * 在指定的根目录下搜索文件（内部实现）。
     * @param baseDirectory 要搜索的根目录 (源目录或目标目录)
     * @param keyword 文件名关键字
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    private PageDto<DecryptedFileDto> searchFilesInternal(Path baseDirectory, String keyword, int page, int size) {
        // 验证分页参数
        if (page < 1) page = 1;
        if (size < 1) size = 10; // 默认大小
        if (size > 100) size = 100; // 防止过大分页

        List<Path> allMatchingFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(baseDirectory)) {
            // 收集所有匹配的文件路径
            allMatchingFiles = stream
                    .filter(Files::isRegularFile) // 只查找文件
                    .filter(path -> { // 根据关键字过滤
                        if (!StringUtils.hasText(keyword)) return true; // 关键字为空则匹配所有
                        // 文件名包含关键字（忽略大小写）
                        return path.getFileName().toString().toLowerCase().contains(keyword.toLowerCase());
                    })
                    // 按最后修改时间降序排序
                    .sorted(Comparator.comparing((Path path) -> { // 显式指定类型
                        try { return Files.getLastModifiedTime(path); }
                        catch (IOException | SecurityException e) { // 捕获可能的异常
                            log.warn("无法获取文件 {} 的最后修改时间，使用纪元时间代替。", path, e);
                            return FileTime.fromMillis(0); // 返回默认值以保证排序继续
                        }
                    }).reversed())
                    .collect(Collectors.toList());
        } catch (IOException | SecurityException e) { // 捕获遍历目录时可能发生的异常
            log.error("遍历目录 {} 时出错。", baseDirectory, e);
            // 出错时返回空的分页结果
            return new PageDto<>(new ArrayList<>(), page, size, 0);
        }

        // 计算分页信息
        long totalElements = allMatchingFiles.size();
        int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
        // 校正当前页码
        int currentPage = Math.max(1, Math.min(page, totalPages == 0 ? 1 : totalPages)); // 确保 currentPage 至少为 1
        if (page > totalPages && totalPages > 0) log.warn("请求的页码 {} 超出范围 (总页数 {})。返回最后一页。", page, totalPages);
        else if (page < 1) log.warn("请求的页码 {} 无效。返回第一页。", page);

        // 计算当前页的索引范围
        int startIndex = (currentPage - 1) * size;
        int endIndex = Math.min(startIndex + size, (int) totalElements);

        // 获取当前页的数据并转换为 DTO
        List<DecryptedFileDto> pageContent = new ArrayList<>();
        if (startIndex < endIndex) {
            List<Path> filesOnPage = allMatchingFiles.subList(startIndex, endIndex);
            for (Path filePath : filesOnPage) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                    // 计算相对于指定根目录的相对路径
                    String relativePathStr = baseDirectory.relativize(filePath.getParent()).toString();
                    // 格式化相对路径 (使用 / 并确保末尾有 /，除非是根目录)
                    if (StringUtils.hasText(relativePathStr) && !relativePathStr.endsWith(FileSystems.getDefault().getSeparator())) {
                        relativePathStr += FileSystems.getDefault().getSeparator();
                    }
                    relativePathStr = relativePathStr.replace(FileSystems.getDefault().getSeparator(), "/");

                    // 格式化最后修改时间
                    LocalDateTime lastModified = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

                    String formattedDate = DateUtils.formatDateTime(DateUtils.convertlocalDateTimeToDate(lastModified)); // 使用 DateUtils

                    // 创建 DTO 对象
                    pageContent.add(new DecryptedFileDto(
                            filePath.getFileName().toString(),
                            relativePathStr,
                            attrs.size(),
                            formattedDate != null ? formattedDate : "N/A" // 处理可能的 null 返回
                    ));
                } catch (IOException | SecurityException e) { // 捕获读取属性时可能发生的异常
                    log.error("读取文件 {} 属性时出错。", filePath, e);
                    // 可以选择跳过该文件或添加一个表示错误的 DTO
                }
            }
        }

        log.debug("在 {} 中找到 {} 个匹配文件，返回第 {} 页，包含 {} 个文件。", baseDirectory.getFileName(), totalElements, currentPage, pageContent.size());
        // 返回分页结果 DTO
        return new PageDto<>(pageContent, currentPage, size, totalElements);
    }

    /**
     * 从指定的根目录获取文件资源（内部实现）。
     * @param baseDirectory 根目录 (源目录或目标目录)
     * @param relativePath 相对路径
     * @param filename 文件名
     * @return 文件资源
     * @throws FileNotFoundException 如果文件无效或找不到
     */
    private Resource getFileResourceInternal(Path baseDirectory, String relativePath, String filename) throws FileNotFoundException {
        // 基础验证
        if (!StringUtils.hasText(filename)) {
            log.warn("请求下载的文件名为空或 null。");
            throw new FileNotFoundException("文件名不能为空。");
        }

        // 构造目标文件路径
        Path targetFile = baseDirectory;
        if (StringUtils.hasText(relativePath)) {
            // 将 Web 路径分隔符替换为系统分隔符进行解析
            String systemRelativePath = relativePath.replace("/", FileSystems.getDefault().getSeparator());
            // 解析相对路径并规范化
            targetFile = targetFile.resolve(systemRelativePath).normalize();
        }
        // 解析文件名并规范化
        targetFile = targetFile.resolve(filename).normalize();

        // 安全性检查：确保最终路径仍在指定的根目录下，防止路径遍历攻击
        if (!targetFile.startsWith(baseDirectory)) {
            log.error("检测到路径遍历尝试！请求路径 '{}' 解析后超出了根目录 '{}'", targetFile, baseDirectory);
            throw new FileNotFoundException("指定的文件路径无效。");
        }

        // 检查文件是否存在、是否是普通文件、是否可读
        if (!Files.exists(targetFile)) {
            log.warn("文件在路径 {} 未找到。", targetFile);
            throw new FileNotFoundException("文件未找到: " + filename + " (位于: " + baseDirectory.getFileName() + ")");
        }
        if (!Files.isRegularFile(targetFile)) {
            log.warn("请求的路径不是一个普通文件: {}", targetFile);
            throw new FileNotFoundException("指定的文件无效: " + filename);
        }
        if (!Files.isReadable(targetFile)) {
            log.warn("文件不可读: {}", targetFile);
            // 可以抛出 FileNotFoundException 或 AccessDeniedException，前者更通用
            throw new FileNotFoundException("文件无法访问: " + filename);
        }

        log.info("提供文件资源: {}", targetFile);
        // 返回 PathResource，Spring 会处理流的传输
        return new PathResource(targetFile);
    }
}
