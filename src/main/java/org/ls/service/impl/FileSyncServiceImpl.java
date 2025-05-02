/**
 * 目录: src/main/java/org/ls/service/impl/FileSyncServiceImpl.java
 * 文件名: FileSyncServiceImpl.java
 * 开发时间: 2025-04-30 15:00:00 EDT (Final Annotated Version)
 * 作者: Gemini
 * 用途: 文件同步服务实现类，负责后台实时监控源目录、定时全量扫描、管理数据库同步记录、
 * 提供同步状态查询、待处理文件查询以及手动同步控制接口。
 */
package org.ls.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.ls.dto.FileSyncStatusDto;
import org.ls.dto.FileSyncTaskControlResultDto;
import org.ls.dto.PageDto;
import org.ls.dto.PendingFileSyncDto;
import org.ls.entity.FileSyncMap;
import org.ls.mapper.FileSyncMapMapper;
import org.ls.service.FileSyncService;
import org.ls.utils.DateUtils; // 假设 DateUtils 工具类存在
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor; // 引入 Spring 的 TaskExecutor
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled; // 引入 Scheduled 注解
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant; // 引入 Instant
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // 使用线程安全的 Map
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function; // 引入 Function
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j // 使用 Lombok 添加日志
public class FileSyncServiceImpl implements FileSyncService {

    // --- 依赖注入 ---
    private final FileSyncMapMapper fileSyncMapMapper; // MyBatis Mapper 用于数据库操作
    private final Environment env; // 用于读取 application.properties 配置
    private final TaskExecutor taskExecutor; // Spring 任务执行器，用于异步处理
    private final PlatformTransactionManager transactionManager; // 平台事务管理器，用于编程式事务
    private final ApplicationContext applicationContext; // 应用上下文，用于获取自身代理以调用 @Async/@Transactional 方法

    // --- 目录路径配置 ---
    private final Path sourceDirectory; // 加密源文件目录
    private final Path tempDirectory;   // 临时解密目录
    private final Path targetDirectory; // 解密目标文件目录

    // --- 功能开关配置 ---
    private final boolean monitoringEnabled; // 是否启用实时文件监控
    private final boolean scanEnabled;       // 是否启用定时全量扫描
    private final String targetFilenameRemoveSuffix; // 目标文件名需要移除的后缀 (可选配置)

    // --- 实时监控状态 ---
    private WatchService watchService; // Java NIO WatchService 实例
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>(); // 存储 WatchKey 与目录 Path 的映射 (线程安全)
    private final AtomicBoolean monitoringActive = new AtomicBoolean(false); // 监控活动状态标志
    private Thread monitoringThread; // 监控线程引用

    // --- 手动同步控制状态 ---
    private final AtomicReference<String> syncProcessStatus = new AtomicReference<>("idle"); // 手动同步进程状态: idle, running, paused, stopping
    private final AtomicBoolean pauseFlag = new AtomicBoolean(false); // 暂停标志
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false); // 取消/停止标志
    private final AtomicReference<Future<?>> currentSyncTaskFuture = new AtomicReference<>(null); // 当前异步同步任务的 Future 对象
    private final AtomicInteger processedInCurrentRun = new AtomicInteger(0); // 当前运行轮次成功处理计数
    private final AtomicInteger failedInCurrentRun = new AtomicInteger(0); // 当前运行轮次失败处理计数
    private static final int SYNC_BATCH_SIZE = 10; // 手动同步时每批处理的文件数量

    // --- 状态常量定义 ---
    private static final String STATUS_PENDING = "pending_sync";         // 待同步
    private static final String STATUS_SYNCED = "synced";               // 已同步
    private static final String STATUS_ERROR_COPYING = "error_copying";   // 复制到临时目录出错
    private static final String STATUS_ERROR_SYNCING = "error_syncing";   // 从临时目录同步到目标目录出错
    private static final String STATUS_SYNCING = "syncing";             // 正在被手动同步任务处理中
    private static final String STATUS_PENDING_DELETION = "pending_deletion"; // 源文件已删除，等待用户确认删除目标文件


    /**
     * 构造函数，注入所有依赖项并初始化配置。
     */
    @Autowired
    public FileSyncServiceImpl(FileSyncMapMapper fileSyncMapMapper, Environment env,
                               @Qualifier("taskExecutor") TaskExecutor taskExecutor, // 指定 TaskExecutor Bean 名称
                               PlatformTransactionManager transactionManager,
                               ApplicationContext applicationContext) {
        this.fileSyncMapMapper = fileSyncMapMapper;
        this.env = env;
        this.taskExecutor = taskExecutor;
        this.transactionManager = transactionManager;
        this.applicationContext = applicationContext;

        // 读取并验证目录配置
        this.sourceDirectory = getRequiredDirectoryPath("file.sync.source-dir");
        this.tempDirectory = getRequiredDirectoryPath("file.sync.temp-dir");
        this.targetDirectory = getRequiredDirectoryPath("file.sync.target-dir");

        // 读取功能开关配置
        this.monitoringEnabled = Boolean.parseBoolean(env.getProperty("file.sync.enabled", "false"));
        this.scanEnabled = Boolean.parseBoolean(env.getProperty("file.sync.scan.enabled", "false"));
        this.targetFilenameRemoveSuffix = env.getProperty("file.sync.target-filename.remove-suffix");

        // 打印初始化信息
        log.info("FileSyncService 初始化完成。");
        log.info("加密源目录 (Source Directory): {}", this.sourceDirectory);
        log.info("临时解密目录 (Temp Directory): {}", this.tempDirectory);
        log.info("解密目标目录 (Target Directory): {}", this.targetDirectory);
        log.info("实时监控启用状态 (Monitoring Enabled): {}", this.monitoringEnabled);
        log.info("定时扫描启用状态 (Scheduled Scan Enabled): {}", this.scanEnabled);
        log.info("目标文件名移除后缀 (Target Filename Remove Suffix): '{}'", this.targetFilenameRemoveSuffix);
    }

    /**
     * 从配置中获取必需的目录路径并进行验证。
     *
     * @param propertyKey 配置属性的键名
     * @return 验证通过的绝对路径 Path 对象
     * @throws IllegalStateException 如果配置缺失或路径无效/权限不足
     */
    private Path getRequiredDirectoryPath(String propertyKey) {
        String pathStr = env.getProperty(propertyKey);
        if (!StringUtils.hasText(pathStr)) {
            log.error("配置属性 '{}' 未设置。", propertyKey);
            throw new IllegalStateException("必需的目录路径 '" + propertyKey + "' 未配置。");
        }
        Path path = Paths.get(pathStr).toAbsolutePath();
        // 根据目录类型调用验证方法
        validateDirectory(path, propertyKey,
                propertyKey.equals("file.sync.source-dir"), // 源目录必须存在
                !propertyKey.equals("file.sync.source-dir") // 临时和目标目录需要写权限
        );
        return path;
    }

    /**
     * 验证目录路径的有效性（存在性、类型、权限）。
     *
     * @param path          待验证的路径
     * @param propertyKey   配置属性键名 (用于日志)
     * @param mustExist     是否必须存在
     * @param checkWritable 是否需要检查写权限
     * @throws IllegalStateException 如果验证失败
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


    // --- Bean 生命周期管理：启动和停止后台监控 ---

    /**
     * Bean 初始化后执行，用于启动后台文件监控线程（如果配置启用）。
     */
    @PostConstruct
    public void initializeMonitoring() {
        if (!monitoringEnabled) {
            log.info("后台文件监控已在配置中禁用。");
            return;
        }
        // 使用原子操作确保只初始化一次
        if (monitoringActive.compareAndSet(false, true)) {
            log.info("正在初始化后台文件监控...");
            try {
                watchService = FileSystems.getDefault().newWatchService(); // 创建 WatchService
                registerDirectoryTree(sourceDirectory); // 递归注册源目录及其子目录

                // 创建并启动监控线程
                monitoringThread = new Thread(this::processWatchEvents, "FileSyncWatcher");
                monitoringThread.setDaemon(true); // 设置为守护线程，允许 JVM 在主线程结束后退出
                monitoringThread.start();
                log.info("后台文件监控已成功启动。");

            } catch (IOException e) {
                log.error("初始化 WatchService 或注册目录失败。监控未启动。", e);
                monitoringActive.set(false); // 重置活动标志
                closeWatchService(); // 清理可能已部分初始化的资源
            } catch (Exception e) {
                log.error("启动后台监控时发生意外错误。", e);
                monitoringActive.set(false);
                closeWatchService();
            }
        } else {
            log.warn("监控初始化被调用，但监控已处于活动状态。");
        }
    }

    /**
     * Bean 销毁前执行，用于停止后台文件监控线程并释放资源。
     */
    @PreDestroy
    public void shutdownMonitoring() {
        // 停止实时监控
        if (monitoringActive.compareAndSet(true, false)) {
            log.info("正在关闭后台文件监控...");
            closeWatchService(); // 关闭 WatchService 会导致 take() 方法抛出异常，从而退出循环
            if (monitoringThread != null && monitoringThread.isAlive()) {
                log.debug("尝试中断监控线程...");
                monitoringThread.interrupt(); // 中断线程，以防卡在 take()
                try {
                    monitoringThread.join(5000); // 等待线程结束，设置超时时间
                    if (monitoringThread.isAlive()) {
                        log.warn("监控线程在超时后仍未结束。");
                    }
                } catch (InterruptedException e) {
                    log.warn("等待监控线程关闭时被中断。");
                    Thread.currentThread().interrupt();
                }
            }
            log.info("后台文件监控已关闭。");
        }
        // 尝试停止可能正在运行的手动同步任务
        if (syncProcessStatus.get().equals("running") || syncProcessStatus.get().equals("paused")) {
            log.info("尝试在应用关闭时停止手动同步任务...");
            stopManualSync(); // 发送停止信号
            Future<?> future = currentSyncTaskFuture.get();
            if (future != null && !future.isDone()) {
                future.cancel(true); // 尝试中断异步任务线程
                log.info("已尝试取消正在运行的手动同步任务。");
            }
        }
    }

    /**
     * 安全地关闭 WatchService 并清理 watchKeys 映射。
     */
    private void closeWatchService() {
        if (watchService != null) {
            try {
                watchService.close(); // 关闭服务，这将使 take() 方法抛出 ClosedWatchServiceException
                log.info("WatchService 已关闭。");
            } catch (IOException e) {
                log.error("关闭 WatchService 时出错。", e);
            } finally {
                watchService = null;
                watchKeys.clear(); // 清空 key 映射
            }
        }
    }

    /**
     * 递归地注册指定目录及其所有子目录到 WatchService。
     *
     * @param start 起始目录路径
     * @throws IOException 如果注册过程中发生 IO 错误
     */
    private void registerDirectoryTree(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir); // 注册当前目录
                return FileVisitResult.CONTINUE; // 继续访问子目录
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.warn("访问文件/目录 {} 失败，跳过注册: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE; // 继续访问其他目录
            }
        });
    }

    /**
     * 注册单个目录到 WatchService。
     *
     * @param dir 要注册的目录路径
     * @throws IOException 如果注册失败
     */
    private void registerDirectory(Path dir) throws IOException {
        // 检查目录是否可读，不可读则跳过
        if (!Files.isReadable(dir)) {
            log.warn("目录不可读，跳过监控注册: {}", dir);
            return;
        }
        try {
            // 注册需要监听的事件类型：创建、修改、删除
            WatchKey key = dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            // 将 WatchKey 与对应的目录路径存入映射，以便后续查找
            watchKeys.put(key, dir);
            log.debug("已注册监控目录: {}", dir);
        } catch (IOException e) {
            log.error("注册监控目录 {} 时失败。", dir, e);
            throw e; // 向上抛出异常，可能导致初始化失败
        }
    }

    /**
     * 后台监控线程的主循环，处理文件系统事件。
     */
    private void processWatchEvents() {
        log.info("文件系统监控事件处理线程已启动。");
        while (monitoringActive.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                log.info("监控线程被中断。正在退出...");
                Thread.currentThread().interrupt();
                monitoringActive.set(false);
                break;
            } catch (ClosedWatchServiceException e) {
                log.info("WatchService 已关闭。正在退出监控线程...");
                monitoringActive.set(false);
                break;
            }

            Path watchedDir = watchKeys.get(key);
            if (watchedDir == null) {
                log.warn("WatchKey 不再被识别，可能对应的目录已被删除。");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    log.warn("WatchService OVERFLOW 事件发生在目录: {}. 可能有事件丢失。", watchedDir);
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path relativeName = ev.context();
                Path fullPath = watchedDir.resolve(relativeName);

                log.debug("检测到事件: {}，路径: {}", kind.name(), fullPath);

                try {
                    FileSyncService self = applicationContext.getBean(FileSyncService.class);

                    // --- 修改点：移除特定的 NoSuchFileException catch ---
                    boolean isDirectory = Files.isDirectory(fullPath, LinkOption.NOFOLLOW_LINKS);
                    // 注意：如果文件在检查时刚好被删除，isDirectory 会返回 false，
                    // 这将使其被当作文件删除事件处理，这是可接受的行为。

                    if (isDirectory) {
                        // 处理目录事件
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            log.info("检测到新目录: {}. 正在注册监控。", fullPath);
                            registerDirectoryTree(fullPath);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            log.info("检测到目录删除事件（或 key 失效前兆）: {}", fullPath);
                        }
                    } else {
                        // 处理文件事件
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            ((FileSyncServiceImpl) self).handleFileCreateOrModify(fullPath);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            ((FileSyncServiceImpl) self).handleFileDelete(fullPath);
                        }
                    }
                } catch (IOException ioEx) {
                    // 捕获检查 isDirectory 或其他潜在的 IO 错误
                    log.error("处理监控事件 {} 时发生 IO 错误，路径: {}", kind.name(), fullPath, ioEx);
                } catch (Exception e) {
                    // 捕获其他所有异常
                    log.error("处理监控事件 {} 时发生意外错误，路径: {}", kind.name(), fullPath, e);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                log.warn("目录 {} 的 WatchKey 不再有效。正在从监控列表中移除。", watchedDir);
                watchKeys.remove(key);
            }
        }
        log.info("文件系统监控事件处理线程已结束。");
    }


    // --- 事件处理逻辑 (由监控线程或扫描任务调用, 带事务) ---

    /**
     * 处理文件创建或修改事件。复制文件到临时目录，并更新数据库记录。
     * 使用 @Transactional 注解确保数据库操作的原子性。
     *
     * @param sourceFilePath 源文件的完整路径
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFileCreateOrModify(Path sourceFilePath) {
        // 检查是否是普通文件，如果不是则跳过
        if (!Files.isRegularFile(sourceFilePath, LinkOption.NOFOLLOW_LINKS)) {
            log.debug("跳过非普通文件事件: {}", sourceFilePath);
            return;
        }
        log.info("处理创建/修改事件: {}", sourceFilePath);
        LocalDateTime sourceLastModifiedTime = null; // 用于存储源文件时间戳

        try {
            // 1. 获取源文件的最后修改时间
            sourceLastModifiedTime = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(sourceFilePath).toInstant(), ZoneId.systemDefault()
            );

            // 2. 计算相对路径和文件名
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath); // 格式化为 "dir/" 或 ""
            String originalFilename = sourceFilePath.getFileName().toString();

            // 3. 查询数据库中是否已存在该文件的记录
            FileSyncMap existingRecord = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);
            String tempFilename;
            boolean isNewEntry = false;

            if (existingRecord == null) {
                // 数据库中不存在记录 -> 新增文件
                isNewEntry = true;
                tempFilename = generateUniqueTempFilename(originalFilename); // 生成唯一的临时文件名
                log.debug("准备为新文件 {} 生成记录，临时文件名: {}", sourceFilePath, tempFilename);
            } else {
                // 数据库中已存在记录 -> 修改文件 或 状态重置
                tempFilename = existingRecord.getTempFilename(); // 使用已有的临时文件名
                // 检查文件时间戳是否真的改变了，或者状态是否需要重置
                if (sourceLastModifiedTime.equals(existingRecord.getSourceLastModified()) &&
                        (STATUS_PENDING.equals(existingRecord.getStatus()) || STATUS_SYNCED.equals(existingRecord.getStatus()))) {
                    // 如果时间戳相同，且状态已经是待同步或已同步，则无需操作
                    log.debug("源文件 {} 未更改或状态无需更新，跳过处理。", sourceFilePath);
                    return; // 提前返回，不执行后续操作
                }
                log.info("检测到源文件 {} 更改或状态需要重置 (DB状态: {}, DB时间: {}, 文件时间: {})。",
                        sourceFilePath, existingRecord.getStatus(), existingRecord.getSourceLastModified(), sourceLastModifiedTime);
            }

            // 4. 复制文件到临时目录 (覆盖同名文件)
            Path tempFilePath = tempDirectory.resolve(tempFilename);
            Files.copy(sourceFilePath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("已复制源文件 {} 到临时文件 {}", sourceFilePath, tempFilePath);

            // 5. 更新数据库记录
            if (isNewEntry) {
                // 插入新记录
                FileSyncMap newRecord = new FileSyncMap(null, relativeDirPath, originalFilename, tempFilename, STATUS_PENDING, null, sourceLastModifiedTime);
                fileSyncMapMapper.insert(newRecord);
                log.debug("已插入新记录到数据库，temp 文件名: {}", tempFilename);
            } else {
                // 更新现有记录的状态为 'pending_sync' 并记录最新的源文件修改时间
                fileSyncMapMapper.updateStatusAndTimestampsById(existingRecord.getId(), STATUS_PENDING, sourceLastModifiedTime);
                log.debug("已更新记录状态为 pending，temp 文件名: {}", tempFilename);
            }
            // 事务将在方法成功结束时提交

        } catch (NoSuchFileException e) {
            // 文件在处理过程中被删除，记录警告，事务会自动回滚（如果已开始）或不执行
            log.warn("处理创建/修改事件时文件 {} 已消失。", sourceFilePath, e);
        } catch (IOException e) {
            // 文件复制失败，记录错误，尝试更新状态为 error_copying，然后抛出异常以回滚事务
            log.error("复制文件 {} 到临时目录时出错。正在回滚事务...", sourceFilePath, e);
            // 尝试在独立事务中更新错误状态
            updateStatusOnError(sourceFilePath, STATUS_ERROR_COPYING, sourceLastModifiedTime);
            // 抛出运行时异常，确保当前事务回滚
            throw new RuntimeException("复制文件失败: " + sourceFilePath, e);
        } catch (Exception e) {
            // 其他意外错误（如数据库操作失败）
            log.error("处理文件 {} 时发生意外错误。正在回滚事务...", sourceFilePath, e);
            // 尝试在独立事务中更新错误状态
            updateStatusOnError(sourceFilePath, STATUS_ERROR_COPYING, sourceLastModifiedTime);
            // 抛出运行时异常，确保当前事务回滚
            throw new RuntimeException("处理文件时发生意外错误: " + sourceFilePath, e);
        }
    }

    /**
     * 处理文件删除事件。将数据库中对应记录的状态标记为 'pending_deletion'。
     *
     * @param sourceFilePath 被删除的源文件的原始路径
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFileDelete(Path sourceFilePath) {
        log.info("处理删除事件，可能的文件路径: {}", sourceFilePath);
        try {
            // 计算相对路径和文件名（即使文件已不存在）
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();

            // 查询数据库中对应的记录
            FileSyncMap recordToDelete = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);

            if (recordToDelete != null) {
                // 如果记录存在且状态不是 'pending_deletion'，则更新状态
                if (!STATUS_PENDING_DELETION.equals(recordToDelete.getStatus())) {
                    fileSyncMapMapper.updateStatusById(recordToDelete.getId(), STATUS_PENDING_DELETION);
                    log.info("已将源文件 {} 对应的记录 ID {} 标记为待删除。", sourceFilePath, recordToDelete.getId());
                    // 注意：不在此处删除临时文件或目标文件，等待用户确认
                } else {
                    // 如果已经是待删除状态，则忽略重复事件
                    log.debug("记录 ID {} 已标记为待删除，忽略重复的删除事件。", recordToDelete.getId());
                }
            } else {
                // 如果数据库中找不到记录，可能已被手动删除或从未同步过
                log.warn("未找到已删除源文件 {} 对应的数据库记录。", sourceFilePath);
            }
            // 事务将在方法成功结束时提交
        } catch (Exception e) {
            // 捕获数据库操作等异常
            log.error("处理文件删除事件 {} 时发生意外错误。正在回滚事务...", sourceFilePath, e);
            // 抛出运行时异常，确保当前事务回滚
            throw new RuntimeException("处理文件删除时发生意外错误: " + sourceFilePath, e);
        }
    }

    /**
     * 在独立的事务中尝试更新记录的错误状态。
     * 用于在主事务回滚时仍能记录错误信息。
     *
     * @param sourceFilePath         源文件路径 (用于查找记录)
     * @param errorStatus            要设置的错误状态
     * @param sourceLastModifiedTime 源文件的最后修改时间 (可能为 null)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 确保在新事务中执行
    public void updateStatusOnError(Path sourceFilePath, String errorStatus, LocalDateTime sourceLastModifiedTime) {
        try {
            // 再次计算路径和文件名以查找记录
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();
            FileSyncMap record = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);

            if (record != null) {
                // 根据是否有时间戳选择不同的更新方法
                if (sourceLastModifiedTime != null) {
                    fileSyncMapMapper.updateStatusAndTimestampsById(record.getId(), errorStatus, sourceLastModifiedTime);
                } else {
                    fileSyncMapMapper.updateStatusById(record.getId(), errorStatus);
                }
                log.warn("已在新事务中更新记录 ID {} 的状态为 {}", record.getId(), errorStatus);
            } else {
                // 如果记录不存在，无法更新状态
                log.warn("无法找到记录来更新错误状态: {}", sourceFilePath);
                // 考虑是否应该在这种情况下插入一条错误记录？（通常不建议，可能导致数据不一致）
            }
            // 新事务成功提交
        } catch (Exception dbEx) {
            // 记录更新错误状态本身也失败了
            log.error("在新事务中为文件 {} 更新错误状态 {} 时失败。", sourceFilePath, errorStatus, dbEx);
            // 此处不再向上抛出异常，避免影响主流程的回滚
        }
    }

    /**
     * 生成在临时目录中唯一的文件名，处理潜在的命名冲突。
     *
     * @param originalFilename 原始文件名
     * @return 唯一的临时文件名
     * @throws IllegalStateException 如果无法生成唯一名称（例如尝试次数过多）
     */
    private String generateUniqueTempFilename(String originalFilename) {
        String baseName;
        String extension;
        // 分离基础名和扩展名
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            baseName = originalFilename.substring(0, dotIndex);
            extension = originalFilename.substring(dotIndex); // 包含点 "."
        } else {
            baseName = originalFilename;
            extension = ""; // 没有扩展名
        }

        String tempFilename = originalFilename;
        int counter = 1;
        // 循环检查数据库中是否存在该临时文件名，直到找到唯一的
        while (fileSyncMapMapper.existsByTempFilename(tempFilename)) {
            // 添加计数后缀，格式如 "basename_1.ext"
            tempFilename = String.format("%s_%d%s", baseName, counter++, extension);
            // 设置一个尝试上限，防止无限循环
            if (counter > 1000) {
                log.error("尝试 1000 次后仍无法为 {} 生成唯一的临时文件名。", originalFilename);
                throw new IllegalStateException("无法生成唯一的临时文件名。");
            }
        }
        // 如果添加了后缀，记录警告
        if (counter > 1) {
            log.warn("为原始文件 '{}' 生成了唯一的临时文件名 '{}'", originalFilename, tempFilename);
        }
        return tempFilename;
    }

    /**
     * 将相对路径 Path 对象格式化为数据库存储的字符串格式。
     * 使用 '/' 作为分隔符，并在非根目录路径末尾添加 '/'。
     *
     * @param relativePath 相对路径 Path 对象
     * @return 格式化后的字符串路径
     */
    private String formatRelativePath(Path relativePath) {
        String pathStr = relativePath.toString();
        if (pathStr.isEmpty()) {
            return ""; // 根目录表示为空字符串
        }
        // 将系统分隔符替换为 '/'
        pathStr = pathStr.replace(FileSystems.getDefault().getSeparator(), "/");
        // 确保末尾有 '/'
        if (!pathStr.endsWith("/")) {
            pathStr += "/";
        }
        return pathStr;
    }


    // --- 定时全量扫描任务 ---

    /**
     * 定时执行全量扫描任务，对比源目录和数据库状态。
     * 使用 @Scheduled 注解，cron 表达式从配置文件读取。
     * 使用 @Async 注解使其在单独的线程中异步执行。
     */
    @Async // 异步执行，需要 TaskExecutor Bean 和 @EnableAsync
    @Scheduled(cron = "${file.sync.scan.cron:0 0 1 * * ?}") // 从配置读取 cron，默认每天凌晨 1 点
    @Transactional(propagation = Propagation.NEVER) // 扫描方法本身不开启事务，内部按需管理
    @Override
    public void performFullScan() {
        // 检查定时扫描功能是否启用
        if (!scanEnabled) {
            log.debug("定时扫描已在配置中禁用，跳过执行。");
            return;
        }
        // TODO: 添加分布式锁或原子标志，防止多个实例并发执行扫描（如果应用可能部署多实例）
        log.info("开始执行定时全量扫描...");
        long startTime = System.currentTimeMillis(); // 记录开始时间

        // 1. 获取文件系统的当前状态 (路径 -> 文件信息)
        Map<String, FileSystemInfo> fileSystemState = scanSourceDirectory();
        if (fileSystemState == null) {
            log.error("全量扫描失败：无法扫描源目录。");
            return; // 扫描失败，提前退出
        }
        log.info("全量扫描：扫描到源目录中 {} 个文件。", fileSystemState.size());

        // 2. 获取数据库中相关记录的当前状态 (路径 -> 数据库记录)
        Map<String, FileSyncMap> dbState = getDatabaseState();
        log.info("全量扫描：从数据库获取到 {} 条相关记录。", dbState.size());

        // 3. 对比文件系统和数据库状态，找出差异
        List<Path> filesToProcess = new ArrayList<>(); // 需要处理的新增或修改的文件路径
        List<Long> idsToMarkForDeletion = new ArrayList<>();   // 需要标记为待删除的数据库记录 ID

        // 3.1 遍历文件系统中的文件
        for (Map.Entry<String, FileSystemInfo> entry : fileSystemState.entrySet()) {
            String fileKey = entry.getKey(); // "relativeDirPath||originalFilename"
            FileSystemInfo fsInfo = entry.getValue();
            FileSyncMap dbRecord = dbState.get(fileKey); // 在数据库状态中查找对应记录

            if (dbRecord == null) {
                // 文件存在于文件系统，但不存在于数据库 -> 新增文件
                log.debug("全量扫描发现新增文件: {}", fsInfo.fullPath);
                filesToProcess.add(fsInfo.fullPath);
            } else {
                // 文件在两边都存在，检查最后修改时间
                if (fsInfo.lastModifiedTime != null && // 确保文件系统时间有效
                        (dbRecord.getSourceLastModified() == null || // 如果数据库没有记录时间
                                fsInfo.lastModifiedTime.isAfter(dbRecord.getSourceLastModified()))) { // 或文件系统时间更新
                    // 文件已被修改
                    log.debug("全量扫描发现修改文件: {}", fsInfo.fullPath);
                    filesToProcess.add(fsInfo.fullPath);
                }
                // 从数据库状态 Map 中移除已匹配的记录，方便后续找出仅存在于数据库的记录
                dbState.remove(fileKey);
            }
        }

        // 3.2 遍历数据库状态 Map 中剩余的记录
        // 这些记录存在于数据库，但对应的文件在文件系统中未找到
        for (FileSyncMap dbRecord : dbState.values()) {
            // 只处理状态不是 'pending_deletion' 的记录（避免重复标记）
            if (!STATUS_PENDING_DELETION.equals(dbRecord.getStatus())) {
                log.debug("全量扫描发现数据库记录对应的源文件已删除: ID={}, Path={}{}",
                        dbRecord.getId(), dbRecord.getRelativeDirPath(), dbRecord.getOriginalFilename());
                idsToMarkForDeletion.add(dbRecord.getId()); // 加入待标记删除列表
            }
        }

        log.info("全量扫描对比完成。发现 {} 个新增/修改的文件，{} 个待标记为删除的文件。", filesToProcess.size(), idsToMarkForDeletion.size());

        // 4. 处理找出的差异
        FileSyncService self = applicationContext.getBean(FileSyncService.class); // 获取自身代理对象

        // 4.1 处理新增/修改的文件 (调用带事务的 handle 方法)
        // 每个文件处理都在其自己的新事务中进行
        for (Path filePath : filesToProcess) {
            try {
                // 通过代理调用，确保 @Transactional 生效
                ((FileSyncServiceImpl) self).handleFileCreateOrModify(filePath);
            } catch (Exception e) {
                // handle 方法内部会记录错误状态，这里只记录扫描任务中的错误
                log.error("全量扫描处理文件 {} 时出错。", filePath, e);
            }
        }

        // 4.2 处理待标记删除的文件 (批量更新状态)
        if (!idsToMarkForDeletion.isEmpty()) {
            try {
                // 在新事务中批量更新状态
                updateDeletionStatusInNewTransaction(idsToMarkForDeletion);
            } catch (Exception e) {
                log.error("全量扫描批量标记待删除状态时出错。", e);
            }
        }

        long endTime = System.currentTimeMillis(); // 记录结束时间
        log.info("定时全量扫描执行完毕，耗时: {} 毫秒", (endTime - startTime));
    }

    /**
     * 辅助方法：扫描源目录，返回包含文件路径和最后修改时间信息的 Map。
     *
     * @return Map<String, FileSystemInfo>，键为 "relativeDirPath||originalFilename"，值为 FileSystemInfo 对象。
     * 如果扫描失败则返回 null。
     */
    private Map<String, FileSystemInfo> scanSourceDirectory() {
        Map<String, FileSystemInfo> fileSystemState = new HashMap<>();
        try (Stream<Path> stream = Files.walk(sourceDirectory)) {
            stream.filter(Files::isRegularFile).forEach(path -> { // 只处理普通文件
                try {
                    // 计算相对路径和文件名
                    Path relativePath = sourceDirectory.relativize(path.getParent());
                    String relativeDirPath = formatRelativePath(relativePath);
                    String originalFilename = path.getFileName().toString();
                    // 创建组合键
                    String key = relativeDirPath + "||" + originalFilename;

                    // 获取最后修改时间
                    LocalDateTime lastModified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()
                    );
                    // 存入 Map
                    fileSystemState.put(key, new FileSystemInfo(path, lastModified));
                } catch (IOException | SecurityException | InvalidPathException e) {
                    // 捕获可能的异常，记录错误并跳过该文件
                    log.error("扫描文件 {} 时读取属性或计算路径出错，跳过该文件。", path, e);
                }
            });
            return fileSystemState; // 返回扫描结果
        } catch (IOException | SecurityException e) {
            // 捕获遍历目录时发生的错误
            log.error("扫描源目录 {} 时出错。", sourceDirectory, e);
            return null; // 返回 null 表示扫描失败
        }
    }

    /**
     * 辅助方法：从数据库获取用于扫描对比的相关记录状态。
     *
     * @return Map<String, FileSyncMap>，键为 "relativeDirPath||originalFilename"，值为 FileSyncMap 对象。
     */
    @Transactional(readOnly = true) // 在只读事务中执行
    protected Map<String, FileSyncMap> getDatabaseState() {
        // 调用 Mapper 获取所有需要参与对比的记录
        List<FileSyncMap> dbRecords = fileSyncMapMapper.selectAllRelevantForScan();
        // 将列表转换为 Map，方便按路径+文件名快速查找
        // 使用 Function.identity() 作为 valueMapper，直接使用 FileSyncMap 对象
        // 如果出现键冲突（理论上不应发生），保留已存在的记录
        return dbRecords.stream()
                .collect(Collectors.toMap(
                        record -> record.getRelativeDirPath() + "||" + record.getOriginalFilename(), // 组合键
                        Function.identity(), // 值就是记录本身
                        (existing, replacement) -> { // 合并函数（处理重复键）
                            log.warn("数据库中发现重复的路径和文件名组合: {}{}", existing.getRelativeDirPath(), existing.getOriginalFilename());
                            return existing; // 保留已存在的
                        }
                ));
    }

    /**
     * 辅助方法：在新事务中批量将记录状态更新为 'pending_deletion'。
     *
     * @param ids 要更新状态的记录 ID 列表
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 确保在新事务中执行
    protected void updateDeletionStatusInNewTransaction(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            int updatedRows = fileSyncMapMapper.batchUpdateStatus(ids, STATUS_PENDING_DELETION);
            log.info("已将 {} 条记录的状态标记为 '{}'。", updatedRows, STATUS_PENDING_DELETION);
        }
    }

    /**
     * 内部静态类，用于存储文件系统扫描时获取的文件信息。
     */
    private static class FileSystemInfo {
        final Path fullPath; // 文件的完整路径
        final LocalDateTime lastModifiedTime; // 文件的最后修改时间

        FileSystemInfo(Path fullPath, LocalDateTime lastModifiedTime) {
            this.fullPath = fullPath;
            this.lastModifiedTime = lastModifiedTime;
        }
    }


    // --- 公共服务方法 (查询和手动同步控制) ---

    /**
     * 获取待处理（待同步或待删除）的文件列表（分页）。
     */
    @Override
    @Transactional(readOnly = true)
    public PageDto<PendingFileSyncDto> getPendingSyncFiles(int page, int size) {
        log.debug("获取待处理文件列表 (pending_sync & pending_deletion), 页码: {}, 大小: {}", page, size);
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        // !!! 注意：以下实现效率较低，建议优化 Mapper 直接查询所需状态并分页 !!!
        // 临时方案：分别查询再合并排序
        List<FileSyncMap> pendingSyncMaps = fileSyncMapMapper.selectByStatus(STATUS_PENDING);
        List<FileSyncMap> pendingDeletionMaps = fileSyncMapMapper.selectByStatus(STATUS_PENDING_DELETION);

        List<FileSyncMap> allPendingMaps = new ArrayList<>();
        allPendingMaps.addAll(pendingSyncMaps);
        allPendingMaps.addAll(pendingDeletionMaps);

        // 按最后更新时间降序排序（使最新的变更排在前面）
        allPendingMaps.sort(Comparator.comparing(FileSyncMap::getLastUpdated, Comparator.nullsLast(Comparator.reverseOrder())));

        // 手动进行分页
        long totalElements = allPendingMaps.size();
        int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
        int currentPage = Math.max(1, Math.min(page, totalPages == 0 ? 1 : totalPages));
        int offset = (currentPage - 1) * size;
        int endIndex = Math.min(offset + size, (int) totalElements);
        List<FileSyncMap> mapsOnPage = (offset < endIndex) ? allPendingMaps.subList(offset, endIndex) : Collections.emptyList();

        // 将实体列表转换为 DTO 列表
        List<PendingFileSyncDto> dtoList = mapsOnPage.stream()
                .map(this::mapToPendingDto) // 调用包含 status 的映射方法
                .collect(Collectors.toList());

        log.debug("返回 {} 条待处理记录，用于页码 {}", dtoList.size(), currentPage);
        return new PageDto<>(dtoList, currentPage, size, totalElements);
    }

    /**
     * 辅助方法：将 FileSyncMap 实体映射到包含状态的 PendingFileSyncDto。
     */
    private PendingFileSyncDto mapToPendingDto(FileSyncMap entity) {
        long fileSize = -1;
        LocalDateTime lastModified = null;
        try {
            Path tempFilePath = tempDirectory.resolve(entity.getTempFilename());
            if (Files.exists(tempFilePath)) {
                BasicFileAttributes attrs = Files.readAttributes(tempFilePath, BasicFileAttributes.class);
                fileSize = attrs.size();
                lastModified = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
            } else {
                if (!STATUS_PENDING_DELETION.equals(entity.getStatus())) {
                    log.warn("临时文件 {} 未找到 (状态: {})", tempFilePath, entity.getStatus());
                }
            }
        } catch (IOException e) {
            log.error("读取临时文件 {} 属性时出错", entity.getTempFilename(), e);
        }
        String formattedDate = (lastModified != null) ? DateUtils.formatDateTime(DateUtils.convertlocalDateTimeToDate(lastModified)) : "N/A";

        // 返回包含 status 的 DTO
        return new PendingFileSyncDto(
                entity.getId(),
                entity.getTempFilename(),
                entity.getOriginalFilename(),
                entity.getRelativeDirPath(),
                fileSize,
                formattedDate,
                entity.getStatus() // 包含状态
        );
    }


    /**
     * 获取当前文件同步服务的整体状态。
     */
    @Override
    @Transactional(readOnly = true)
    public FileSyncStatusDto getSyncStatus() {
        // 分别统计各种状态的数量
        long pendingCount = fileSyncMapMapper.countByStatus(STATUS_PENDING);
        long syncedCount = fileSyncMapMapper.countByStatus(STATUS_SYNCED);
        long errorCopyingCount = fileSyncMapMapper.countByStatus(STATUS_ERROR_COPYING);
        long errorSyncingCount = fileSyncMapMapper.countByStatus(STATUS_ERROR_SYNCING);
        long syncingCount = fileSyncMapMapper.countByStatus(STATUS_SYNCING);
        long pendingDeletionCount = fileSyncMapMapper.countByStatus(STATUS_PENDING_DELETION);

        // 组装状态 DTO
        return new FileSyncStatusDto(
                monitoringActive.get(), // 实时监控是否活动
                pendingCount,           // 待同步数量
                syncedCount,            // 已同步数量
                // 将错误、进行中、待删除状态合并为需要关注的总数
                errorCopyingCount + errorSyncingCount + syncingCount + pendingDeletionCount,
                syncProcessStatus.get(), // 手动同步进程状态
                processedInCurrentRun.get(), // 本轮手动同步成功数
                failedInCurrentRun.get()     // 本轮手动同步失败数
        );
    }

    // --- 手动同步控制方法 (实现细节保持不变) ---
    @Override
    public FileSyncTaskControlResultDto startManualSync() {
        log.info("Attempting to start manual sync process...");
        // Ensure only one sync process runs at a time using compareAndSet
        if (syncProcessStatus.compareAndSet("idle", "running")) {
            log.info("Starting new manual sync task.");
            // Reset flags and counters for the new run
            pauseFlag.set(false);
            cancelFlag.set(false);
            processedInCurrentRun.set(0);
            failedInCurrentRun.set(0);

            // Get self-proxy to call the @Async method
            FileSyncService self = applicationContext.getBean(FileSyncService.class);
            Future<?> future = ((FileSyncServiceImpl) self).runSyncCycle(); // Call @Async method via proxy
            currentSyncTaskFuture.set(future);

            return new FileSyncTaskControlResultDto(true, "同步已启动。", "running");
        } else {
            String currentStatus = syncProcessStatus.get();
            log.warn("Cannot start manual sync. Current status: {}", currentStatus);
            String message = "无法启动同步，当前状态: " + currentStatus + ".";
            if ("paused".equals(currentStatus)) {
                message += " 请先恢复同步。";
            } else if ("running".equals(currentStatus)) {
                message += " 同步已在运行中。";
            } else if ("stopping".equals(currentStatus)) {
                message += " 同步正在停止中。";
            }
            return new FileSyncTaskControlResultDto(false, message, currentStatus);
        }
    }

    @Override
    public FileSyncTaskControlResultDto pauseManualSync() {
        log.info("Attempting to pause manual sync process...");
        if (syncProcessStatus.compareAndSet("running", "paused")) {
            pauseFlag.set(true);
            log.info("Manual sync process paused.");
            return new FileSyncTaskControlResultDto(true, "同步已暂停。", "paused");
        } else {
            String currentStatus = syncProcessStatus.get();
            log.warn("Cannot pause manual sync. Current status: {}", currentStatus);
            return new FileSyncTaskControlResultDto(false, "无法暂停，当前状态: " + currentStatus + ".", currentStatus);
        }
    }

    @Override
    public FileSyncTaskControlResultDto resumeManualSync() {
        log.info("Attempting to resume manual sync process...");
        if (syncProcessStatus.compareAndSet("paused", "running")) {
            pauseFlag.set(false);
            log.info("Manual sync process resumed.");
            return new FileSyncTaskControlResultDto(true, "同步已恢复。", "running");
        } else {
            String currentStatus = syncProcessStatus.get();
            log.warn("Cannot resume manual sync. Current status: {}", currentStatus);
            return new FileSyncTaskControlResultDto(false, "无法恢复，当前状态: " + currentStatus + ".", currentStatus);
        }
    }

    @Override
    public FileSyncTaskControlResultDto stopManualSync() {
        log.info("Attempting to stop manual sync process...");
        String currentStatus = syncProcessStatus.get();
        if ("running".equals(currentStatus) || "paused".equals(currentStatus)) {
            // Attempt to set status to 'stopping' first to prevent new start requests
            if (syncProcessStatus.compareAndSet(currentStatus, "stopping")) {
                cancelFlag.set(true);
                pauseFlag.set(false); // Ensure it doesn't stay paused
                log.info("Stop signal sent to manual sync process. Status set to stopping.");

                // Optionally try to cancel the future task (may interrupt IO)
                Future<?> future = currentSyncTaskFuture.get();
                if (future != null && !future.isDone()) {
                    future.cancel(true); // Attempt to interrupt the task thread
                    log.info("Attempted to cancel the running sync task future.");
                }

                return new FileSyncTaskControlResultDto(true, "停止信号已发送。", "stopping");
            } else {
                // Status changed between check and set, likely finished or stopped already
                log.warn("Could not set status to stopping, current status is now: {}", syncProcessStatus.get());
                return new FileSyncTaskControlResultDto(false, "无法停止，当前状态已改变: " + syncProcessStatus.get() + ".", syncProcessStatus.get());
            }
        } else {
            log.warn("Cannot stop manual sync. Current status: {}", currentStatus);
            return new FileSyncTaskControlResultDto(false, "无法停止，当前状态: " + currentStatus + ".", currentStatus);
        }
    }


    // --- Asynchronous Sync Cycle ---

    /**
     * Asynchronous method containing the main loop for processing pending files.
     * This method runs in a separate thread managed by the TaskExecutor.
     * It fetches files in batches and processes them.
     */
    @Async // Make this method run asynchronously
    public Future<?> runSyncCycle() { // Return Future<?> or CompletableFuture<?>
        log.info("Starting asynchronous sync cycle...");
        try {
            while (!cancelFlag.get()) {
                // Handle pause state
                while (pauseFlag.get() && !cancelFlag.get()) {
                    try {
                        Thread.sleep(1000); // Sleep while paused
                    } catch (InterruptedException e) {
                        log.warn("Sync cycle interrupted during pause. Checking cancel flag.");
                        Thread.currentThread().interrupt(); // Re-interrupt
                        if (cancelFlag.get()) break; // Exit if cancelled during sleep
                    }
                }
                if (cancelFlag.get()) break; // Exit if cancelled after pause

                // --- Process one batch ---
                List<FileSyncMap> batchToProcess = selectAndProcessBatch();

                // If no files were found in pending state, the sync is complete for now
                if (CollectionUtils.isEmpty(batchToProcess)) {
                    log.info("No more pending files found in this cycle.");
                    break; // Exit the main while loop
                }

                log.info("Processing batch of {} files...", batchToProcess.size());
                for (FileSyncMap record : batchToProcess) {
                    if (cancelFlag.get()) {
                        log.info("Cancel flag detected during batch processing. Aborting current batch.");
                        // Update status back to pending for unprocessed items in this locked batch? Or leave as 'syncing'?
                        // Leaving as 'syncing' might be okay, next run will pick them up if needed.
                        break; // Exit the inner for loop
                    }
                    // Handle pause within the batch processing as well
                    while (pauseFlag.get() && !cancelFlag.get()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            log.warn("Sync cycle interrupted during pause within batch. Checking cancel flag.");
                            Thread.currentThread().interrupt();
                            if (cancelFlag.get()) break;
                        }
                    }
                    if (cancelFlag.get()) break;

                    // Process individual file (move and update status)
                    processSingleFileSyncRecord(record);
                }
                // --- End of batch processing ---

                // Optional: Add a small delay between batches to avoid hammering the DB/FS
                // try { Thread.sleep(100); } catch (InterruptedException e) { /* handle */ }

            } // End of main while loop

        } catch (Exception e) {
            log.error("Unhandled exception in asynchronous sync cycle!", e);
            // Ensure status is reset even if an unexpected error occurs
        } finally {
            log.info("Asynchronous sync cycle finished. Processed: {}, Failed: {}. Cancelled: {}",
                    processedInCurrentRun.get(), failedInCurrentRun.get(), cancelFlag.get());
            // Reset status to idle only if it was running or stopping
            syncProcessStatus.compareAndSet("running", "idle");
            syncProcessStatus.compareAndSet("paused", "idle"); // If paused and then cancelled/finished
            syncProcessStatus.compareAndSet("stopping", "idle");
            currentSyncTaskFuture.set(null); // Clear the future reference
            // Do NOT reset cancel/pause flags here, let startManualSync do it
        }
        return null; // Or return a CompletableFuture result if needed
    }

    /**
     * Selects a batch of pending files, locks them by updating status to 'syncing'.
     * Uses programmatic transaction management.
     *
     * @return List of FileSyncMap records to process, or empty list if none found.
     */
    private List<FileSyncMap> selectAndProcessBatch() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);
        List<FileSyncMap> batch = Collections.emptyList();
        try {
            batch = fileSyncMapMapper.selectAndLockPending(SYNC_BATCH_SIZE);
            if (!CollectionUtils.isEmpty(batch)) {
                List<Long> idsToUpdate = batch.stream().map(FileSyncMap::getId).collect(Collectors.toList());
                int updatedRows = fileSyncMapMapper.batchUpdateStatus(idsToUpdate, STATUS_SYNCING);
                if (updatedRows != batch.size()) {
                    // This case should ideally not happen with SKIP LOCKED if locking works correctly
                    log.error("Mismatch between locked rows ({}) and updated rows ({}). Rolling back batch selection.", batch.size(), updatedRows);
                    transactionManager.rollback(status); // Rollback the status update
                    return Collections.emptyList(); // Return empty to avoid processing potentially unlocked rows
                }
                log.debug("Locked and updated status to 'syncing' for {} records.", updatedRows);
            }
            transactionManager.commit(status);
            return batch;
        } catch (Exception e) {
            log.error("Error selecting and locking batch. Rolling back.", e);
            try {
                transactionManager.rollback(status);
            } catch (Exception rbEx) {
                log.error("Error during rollback of batch selection.", rbEx);
            }
            return Collections.emptyList(); // Return empty on error
        }
    }


    /**
     * Processes a single FileSyncMap record: moves the file and updates its status.
     * Uses programmatic transaction for the final status update.
     */
    private void processSingleFileSyncRecord(FileSyncMap record) {
        Path tempFilePath = tempDirectory.resolve(record.getTempFilename());
        String finalStatus = STATUS_ERROR_SYNCING; // Default to error

        try {
            if (!Files.exists(tempFilePath)) {
                log.error("Temporary file {} for record ID {} not found. Setting status to error.", tempFilePath, record.getId());
                // Status already defaults to error, just log and proceed to update DB
            } else {
                // Determine final target path and filename
                String targetFilename = generateTargetFilename(record.getOriginalFilename());
                Path targetPath = targetDirectory.resolve(record.getRelativeDirPath()).resolve(targetFilename).normalize();

                // Security check: Ensure target is within the target directory
                if (!targetPath.startsWith(targetDirectory)) {
                    log.error("Target path traversal attempt for record ID {}: {}", record.getId(), targetPath);
                    // Status remains error_syncing
                } else {
                    // Ensure parent directories exist
                    Files.createDirectories(targetPath.getParent());

                    // Move the file (atomic operation on most systems)
                    Files.move(tempFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING); // Overwrite if exists
                    log.info("Successfully moved temp file {} to target {}", tempFilePath, targetPath);
                    finalStatus = STATUS_SYNCED; // Set status to success
                    processedInCurrentRun.incrementAndGet(); // Increment success counter
                }
            }
        } catch (IOException e) {
            log.error("IOException during file move for record ID {}. Temp: {}, Target attempt: {}. Setting status to error.",
                    record.getId(), tempFilePath, record.getRelativeDirPath() + generateTargetFilename(record.getOriginalFilename()), e);
            // Status remains error_syncing
        } catch (Exception e) {
            log.error("Unexpected exception during file processing for record ID {}. Setting status to error.", record.getId(), e);
            // Status remains error_syncing
        }

        // Update status in a new transaction
        if (STATUS_ERROR_SYNCING.equals(finalStatus)) {
            failedInCurrentRun.incrementAndGet(); // Increment failure counter only if error occurred here
        }
        updateFileSyncStatusInNewTransaction(record.getId(), finalStatus);
    }

    /**
     * Updates the status of a single record in a new transaction.
     */
    private void updateFileSyncStatusInNewTransaction(Long id, String status) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus txStatus = transactionManager.getTransaction(def);
        try {
            int updated = fileSyncMapMapper.updateStatusById(id, status);
            transactionManager.commit(txStatus);
            if (updated > 0) {
                log.debug("Updated status to '{}' for record ID {}", status, id);
            } else {
                log.warn("Could not update status to '{}' for record ID {} (record might have been deleted concurrently?)", status, id);
            }
        } catch (Exception e) {
            log.error("Failed to update status to '{}' for record ID {}. Rolling back status update.", status, id, e);
            try {
                transactionManager.rollback(txStatus);
            } catch (Exception rbEx) {
                log.error("Error during rollback of status update for record ID {}.", id, rbEx);
            }
        }
    }

    /**
     * Generates the final filename for the target directory based on configuration.
     */
    private String generateTargetFilename(String originalFilename) {
        if (StringUtils.hasText(targetFilenameRemoveSuffix) && originalFilename.endsWith(targetFilenameRemoveSuffix)) {
            return originalFilename.substring(0, originalFilename.length() - targetFilenameRemoveSuffix.length());
        }
        return originalFilename; // Return original if no suffix rule matches
    }


    // --- 用户确认删除的处理方法 (实现细节保持不变) ---
    @Override
    @Transactional(propagation = Propagation.NEVER) // 方法内部自己管理事务
    public void confirmAndDeleteFiles(List<Long> idsToConfirm) {
        log.info("开始处理用户确认删除的 {} 条记录...", idsToConfirm != null ? idsToConfirm.size() : 0);
        if (CollectionUtils.isEmpty(idsToConfirm)) {
            log.warn("确认删除列表为空，无需操作。");
            return;
        }
        int successCount = 0;
        int failCount = 0;

        for (Long id : idsToConfirm) {
            if (id == null) {
                log.warn("确认删除列表中包含 null ID，跳过。");
                failCount++;
                continue;
            }
            // 调用处理单个删除的方法
            boolean deleted = processSingleDeletionConfirmation(id);
            if (deleted) {
                successCount++;
            } else {
                failCount++;
            }
        }
        log.info("用户确认删除处理完成。成功: {}, 失败: {}", successCount, failCount);
        // 可以考虑返回更详细的结果
    }

    /**
     * 处理单个文件的删除确认（包含文件删除和DB记录删除）。
     * 使用编程式事务管理。
     *
     * @param id 记录 ID
     * @return true 如果成功删除，否则 false
     */
    private boolean processSingleDeletionConfirmation(Long id) {
        // 定义新事务
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(def);
        FileSyncMap record = null;

        try {
            // 1. 查询并验证记录状态 (在事务内执行)
            // 注意：需要确保 Mapper 有 selectById 方法，或者修改这里的查询逻辑
            // record = fileSyncMapMapper.selectById(id); // 假设存在此方法
            // 临时替代方案 (效率低):
            record = fileSyncMapMapper.selectByStatus(STATUS_PENDING_DELETION)
                    .stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (record == null) {
                log.warn("确认删除失败：未找到 ID 为 {} 的记录，或其状态不是 '{}'。", id, STATUS_PENDING_DELETION);
                transactionManager.rollback(txStatus); // 回滚事务
                return false;
            }

            // 2. 删除解密目录中的对应文件
            String targetFilename = generateTargetFilename(record.getOriginalFilename());
            Path targetFilePath = targetDirectory.resolve(record.getRelativeDirPath()).resolve(targetFilename).normalize();
            // 安全性检查
            if (!targetFilePath.startsWith(targetDirectory)) {
                log.error("确认删除失败：目标路径 {} 超出范围。", targetFilePath);
                transactionManager.rollback(txStatus);
                return false;
            }
            try {
                boolean targetDeleted = Files.deleteIfExists(targetFilePath);
                log.info("删除解密文件 {} (成功: {})", targetFilePath, targetDeleted);
            } catch (IOException | SecurityException e) {
                log.error("确认删除失败：删除解密文件 {} 时出错。", targetFilePath, e);
                transactionManager.rollback(txStatus);
                return false;
            }

            // 3. 删除临时目录中的对应文件
            Path tempFilePath = tempDirectory.resolve(record.getTempFilename());
            try {
                boolean tempDeleted = Files.deleteIfExists(tempFilePath);
                log.info("删除临时文件 {} (成功: {})", tempFilePath, tempDeleted);
            } catch (IOException | SecurityException e) {
                log.error("确认删除失败：删除临时文件 {} 时出错。", tempFilePath, e);
                transactionManager.rollback(txStatus);
                return false;
            }

            // 4. 从数据库中删除该记录
            int deletedRows = fileSyncMapMapper.deleteById(id);
            if (deletedRows == 0) {
                // 理论上不应发生，因为前面已查到记录
                log.warn("确认删除失败：删除数据库记录 ID {} 时未找到或已被删除。", id);
                transactionManager.rollback(txStatus);
                return false;
            }

            // 5. 提交事务
            transactionManager.commit(txStatus);
            log.info("已成功确认并删除与记录 ID {} 相关的文件和数据。", id);
            return true;

        } catch (Exception e) {
            // 捕获任何意外错误
            log.error("处理删除确认 ID {} 时发生意外错误。", id, e);
            try {
                // 尝试回滚事务
                transactionManager.rollback(txStatus);
            } catch (Exception rbEx) {
                log.error("回滚删除确认事务 ID {} 时出错。", id, rbEx);
            }
            return false; // 返回失败
        }
    }

}
