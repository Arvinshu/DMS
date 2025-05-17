/**
 * 目录: src/main/java/org/ls/service/impl/FileSyncServiceImpl.java
 * 文件名: FileSyncServiceImpl.java
 * 开发时间: 2025-04-30 15:00:00 EDT (Final Annotated Version)
 * 作者: Gemini
 * 用途: 文件同步服务实现类，负责后台实时监控源目录、定时全量扫描、管理数据库同步记录、
 * 提供同步状态查询、待处理文件查询以及手动同步控制接口。
 */
package org.ls.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.kafka.core.KafkaTemplate; // +++ 引入 KafkaTemplate +++
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; // 用于生成 eventId
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

    // +++ 注入 KafkaTemplate 和 ObjectMapper +++
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper; // Jackson ObjectMapper for JSON conversion

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
    private static final int SYNC_BATCH_SIZE = 100; // 手动同步时每批处理的文件数量

    // --- 状态常量定义 ---
    private static final String STATUS_PENDING = "pending_sync";         // 待同步
    private static final String STATUS_SYNCED = "synced";               // 已同步
    private static final String STATUS_ERROR_COPYING = "error_copying";   // 复制到临时目录出错
    private static final String STATUS_ERROR_SYNCING = "error_syncing";   // 从临时目录同步到目标目录出错
    private static final String STATUS_SYNCING = "syncing";             // 正在被手动同步任务处理中
    private static final String STATUS_PENDING_DELETION = "pending_deletion"; // 源文件已删除，等待用户确认删除目标文件

    // +++ 定义 Kafka Topic 名称常量 +++
    private static final String TOPIC_FILE_UPSERT_EVENTS = "dms-file-upsert-events";
    private static final String TOPIC_FILE_DELETE_EVENTS = "dms-file-delete-events";

    /**
     * 构造函数，注入所有依赖项并初始化配置。
     */
    @Autowired
    public FileSyncServiceImpl(FileSyncMapMapper fileSyncMapMapper, Environment env,
                               @Qualifier("taskExecutor") TaskExecutor taskExecutor,
                               PlatformTransactionManager transactionManager,
                               ApplicationContext applicationContext,
                               KafkaTemplate<String, String> kafkaTemplate, // +++ 注入 KafkaTemplate +++
                               ObjectMapper objectMapper) { // +++ 注入 ObjectMapper +++
        this.fileSyncMapMapper = fileSyncMapMapper;
        this.env = env;
        this.taskExecutor = taskExecutor;
        this.transactionManager = transactionManager;
        this.applicationContext = applicationContext;
        this.kafkaTemplate = kafkaTemplate; // +++ 赋值 +++
        this.objectMapper = objectMapper;   // +++ 赋值 +++

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

                    boolean isDirectory = Files.isDirectory(fullPath, LinkOption.NOFOLLOW_LINKS);

                    if (isDirectory) {
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            log.info("检测到新目录: {}. 正在注册监控。", fullPath);
                            registerDirectoryTree(fullPath);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            log.info("检测到目录删除事件（或 key 失效前兆）: {}", fullPath);
                            // +++ 目录删除事件也可能触发目标端空目录的清理检查 (如果需要实时性更高)
                            // +++ 但通常由 performFullScan 处理更全面, 这里暂不直接处理目标端删除
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
                    log.error("处理监控事件 {} 时发生 IO 错误，路径: {}", kind.name(), fullPath, ioEx);
                } catch (Exception e) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFileCreateOrModify(Path sourceFilePath) {
        if (!Files.isRegularFile(sourceFilePath, LinkOption.NOFOLLOW_LINKS)) {
            log.debug("跳过非普通文件事件: {}", sourceFilePath);
            return;
        }
        log.info("处理创建/修改事件: {}", sourceFilePath);
        LocalDateTime sourceLastModifiedTimeTruncated = null;

        try {
            Instant sourceInstant = Files.getLastModifiedTime(sourceFilePath).toInstant();
            Instant sourceInstantTruncatedToSecond = sourceInstant.truncatedTo(ChronoUnit.SECONDS);
            sourceLastModifiedTimeTruncated = LocalDateTime.ofInstant(sourceInstantTruncatedToSecond, ZoneId.systemDefault());

            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();

            FileSyncMap existingRecord = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);
            String tempFilename;
            boolean isNewEntry = false;

            if (existingRecord == null) {
                isNewEntry = true;
                tempFilename = generateUniqueTempFilename(originalFilename);
                log.debug("准备为新文件 {} 生成记录，临时文件名: {}", sourceFilePath, tempFilename);
            } else {
                tempFilename = existingRecord.getTempFilename();
                LocalDateTime dbSourceLastModified = existingRecord.getSourceLastModified();
                LocalDateTime dbSourceLastModifiedTruncatedToSecond = null;
                if (dbSourceLastModified != null) {
                    dbSourceLastModifiedTruncatedToSecond = dbSourceLastModified.truncatedTo(ChronoUnit.SECONDS);
                }
                if (sourceLastModifiedTimeTruncated.equals(dbSourceLastModifiedTruncatedToSecond) &&
                        (STATUS_PENDING.equals(existingRecord.getStatus()) || STATUS_SYNCED.equals(existingRecord.getStatus()))) {
                    log.debug("源文件 {} 未更改(秒级比较)或状态无需更新，跳过处理。", sourceFilePath);
                    return;
                }
                log.info("检测到源文件 {} 更改或状态需要重置 (DB状态: {}, DB时间(原始): {}, DB时间(秒级): {}, 文件时间(秒级): {})。",
                        sourceFilePath, existingRecord.getStatus(), existingRecord.getSourceLastModified(), dbSourceLastModifiedTruncatedToSecond, sourceLastModifiedTimeTruncated);
            }

            Path tempFilePath = tempDirectory.resolve(tempFilename);
            Files.copy(sourceFilePath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("已复制源文件 {} 到临时文件 {}", sourceFilePath, tempFilePath);

            if (isNewEntry) {
                FileSyncMap newRecord = new FileSyncMap(null, relativeDirPath, originalFilename, tempFilename, STATUS_PENDING, null, sourceLastModifiedTimeTruncated);
                fileSyncMapMapper.insert(newRecord);
                log.debug("已插入新记录到数据库，temp 文件名: {}，源文件修改时间(秒级): {}", tempFilename, sourceLastModifiedTimeTruncated);
            } else {
                fileSyncMapMapper.updateStatusAndTimestampsById(existingRecord.getId(), STATUS_PENDING, sourceLastModifiedTimeTruncated);
                log.debug("已更新记录状态为 pending，temp 文件名: {}，源文件修改时间(秒级): {}", tempFilename, sourceLastModifiedTimeTruncated);
            }
        } catch (NoSuchFileException e) {
            log.warn("处理创建/修改事件时文件 {} 已消失。", sourceFilePath, e);
        } catch (IOException e) {
            log.error("复制文件 {} 到临时目录时出错。正在回滚事务...", sourceFilePath, e);
            updateStatusOnError(sourceFilePath, STATUS_ERROR_COPYING, sourceLastModifiedTimeTruncated);
            throw new RuntimeException("复制文件失败: " + sourceFilePath, e);
        } catch (Exception e) {
            log.error("处理文件 {} 时发生意外错误。正在回滚事务...", sourceFilePath, e);
            updateStatusOnError(sourceFilePath, STATUS_ERROR_COPYING, sourceLastModifiedTimeTruncated);
            throw new RuntimeException("处理文件时发生意外错误: " + sourceFilePath, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFileDelete(Path sourceFilePath) {
        log.info("处理删除事件，可能的文件路径: {}", sourceFilePath);
        try {
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();

            FileSyncMap recordToDelete = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);

            if (recordToDelete != null) {
                if (!STATUS_PENDING_DELETION.equals(recordToDelete.getStatus())) {
                    fileSyncMapMapper.updateStatusById(recordToDelete.getId(), STATUS_PENDING_DELETION);
                    log.info("已将源文件 {} 对应的记录 ID {} 标记为待删除。", sourceFilePath, recordToDelete.getId());
                } else {
                    log.debug("记录 ID {} 已标记为待删除，忽略重复的删除事件。", recordToDelete.getId());
                }
            } else {
                log.warn("未找到已删除源文件 {} 对应的数据库记录。", sourceFilePath);
            }
        } catch (Exception e) {
            log.error("处理文件删除事件 {} 时发生意外错误。正在回滚事务...", sourceFilePath, e);
            throw new RuntimeException("处理文件删除时发生意外错误: " + sourceFilePath, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusOnError(Path sourceFilePath, String errorStatus, LocalDateTime sourceLastModifiedTimeTruncated) {
        try {
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();
            FileSyncMap record = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);

            if (record != null) {
                if (sourceLastModifiedTimeTruncated != null) {
                    fileSyncMapMapper.updateStatusAndTimestampsById(record.getId(), errorStatus, sourceLastModifiedTimeTruncated);
                } else {
                    fileSyncMapMapper.updateStatusById(record.getId(), errorStatus);
                }
                log.warn("已在新事务中更新记录 ID {} 的状态为 {}", record.getId(), errorStatus);
            } else {
                log.warn("无法找到记录来更新错误状态: {}", sourceFilePath);
            }
        } catch (Exception dbEx) {
            log.error("在新事务中为文件 {} 更新错误状态 {} 时失败。", sourceFilePath, errorStatus, dbEx);
        }
    }

    private String generateUniqueTempFilename(String originalFilename) {
        String baseName;
        String extension;
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            baseName = originalFilename.substring(0, dotIndex);
            extension = originalFilename.substring(dotIndex);
        } else {
            baseName = originalFilename;
            extension = "";
        }

        String tempFilename = originalFilename;
        int counter = 1;
        while (fileSyncMapMapper.existsByTempFilename(tempFilename)) {
            tempFilename = String.format("%s_%d%s", baseName, counter++, extension);
            if (counter > 1000) {
                log.error("尝试 1000 次后仍无法为 {} 生成唯一的临时文件名。", originalFilename);
                throw new IllegalStateException("无法生成唯一的临时文件名。");
            }
        }
        if (counter > 1) {
            log.warn("为原始文件 '{}' 生成了唯一的临时文件名 '{}'", originalFilename, tempFilename);
        }
        return tempFilename;
    }

    private String formatRelativePath(Path relativePath) {
        String pathStr = relativePath.toString();
        if (pathStr.isEmpty()) {
            return "";
        }
        pathStr = pathStr.replace(FileSystems.getDefault().getSeparator(), "/");
        if (!pathStr.endsWith("/")) {
            pathStr += "/";
        }
        return pathStr;
    }


    // --- 定时全量扫描任务 ---

    @Async
    @Scheduled(cron = "${file.sync.scan.cron:0 0 1 * * ?}")
    @Transactional(propagation = Propagation.NEVER)
    @Override
    public void performFullScan() {
        if (!scanEnabled) {
            log.debug("定时扫描已在配置中禁用，跳过执行。");
            return;
        }
        log.info("开始执行定时全量扫描...");
        long startTime = System.currentTimeMillis();

        Map<String, FileSystemInfo> fileSystemState = scanSourceDirectory();
        if (fileSystemState == null) {
            log.error("全量扫描失败：无法扫描源目录。");
            return;
        }
        log.info("全量扫描：扫描到源目录中 {} 个文件。", fileSystemState.size());

        Map<String, FileSyncMap> dbState = getDatabaseState();
        log.info("全量扫描：从数据库获取到 {} 条相关记录。", dbState.size());

        List<Path> filesToProcess = new ArrayList<>();
        List<Long> idsToMarkForDeletion = new ArrayList<>();

        for (Map.Entry<String, FileSystemInfo> entry : fileSystemState.entrySet()) {
            String fileKey = entry.getKey();
            FileSystemInfo fsInfo = entry.getValue();
            FileSyncMap dbRecord = dbState.get(fileKey);

            if (dbRecord == null) {
                log.debug("全量扫描发现新增文件: {}", fsInfo.fullPath);
                filesToProcess.add(fsInfo.fullPath);
            } else {
                LocalDateTime fsLastModifiedSeconds = fsInfo.lastModifiedTime;
                LocalDateTime dbSourceLastModified = dbRecord.getSourceLastModified();
                LocalDateTime dbSourceLastModifiedSeconds = null;

                if (dbSourceLastModified != null) {
                    dbSourceLastModifiedSeconds = dbSourceLastModified.truncatedTo(ChronoUnit.SECONDS);
                }

                if (fsLastModifiedSeconds != null &&
                        (dbSourceLastModifiedSeconds == null ||
                                fsLastModifiedSeconds.isAfter(dbSourceLastModifiedSeconds))) {
                    log.debug("全量扫描发现修改文件: {}", fsInfo.fullPath);
                    log.debug("文件系统记录时间(秒级): {}", fsLastModifiedSeconds);
                    log.debug("数据库中记录时间(原始): {}", dbRecord.getSourceLastModified());
                    log.debug("数据库中记录时间(秒级): {}", dbSourceLastModifiedSeconds);
                    filesToProcess.add(fsInfo.fullPath);
                }
                dbState.remove(fileKey);
            }
        }

        for (FileSyncMap dbRecord : dbState.values()) {
            if (!STATUS_PENDING_DELETION.equals(dbRecord.getStatus())) {
                log.debug("全量扫描发现数据库记录对应的源文件已删除: ID={}, Path={}{}",
                        dbRecord.getId(), dbRecord.getRelativeDirPath(), dbRecord.getOriginalFilename());
                idsToMarkForDeletion.add(dbRecord.getId());
            }
        }

        log.info("全量扫描对比完成。发现 {} 个新增/修改的文件，{} 个待标记为删除的文件。", filesToProcess.size(), idsToMarkForDeletion.size());

        FileSyncService self = applicationContext.getBean(FileSyncService.class);

        for (Path filePath : filesToProcess) {
            try {
                ((FileSyncServiceImpl) self).handleFileCreateOrModify(filePath);
            } catch (Exception e) {
                log.error("全量扫描处理文件 {} 时出错。", filePath, e);
            }
        }

        if (!idsToMarkForDeletion.isEmpty()) {
            try {
                updateDeletionStatusInNewTransaction(idsToMarkForDeletion);
            } catch (Exception e) {
                log.error("全量扫描批量标记待删除状态时出错。", e);
            }
        }

        // +++ 新增阶段：处理目标目录中多余的空目录 +++
        log.info("全量扫描：开始检查并清理目标目录中多余的空目录...");
        try {
            List<Path> targetSubDirs;
            try (Stream<Path> walk = Files.walk(this.targetDirectory)) {
                targetSubDirs = walk.filter(Files::isDirectory)
                        .filter(p -> !p.equals(this.targetDirectory))
                        .sorted(Comparator.reverseOrder()) // 最深的目录在前，确保先删除子目录
                        .collect(Collectors.toList());
            }

            for (Path targetDirPath : targetSubDirs) {
                // 对于 targetDirectory 下的每个子目录，计算其相对路径
                Path relativeTargetDirPath = this.targetDirectory.relativize(targetDirPath);
                // 构造对应的源目录路径
                Path correspondingSourceDirPath = this.sourceDirectory.resolve(relativeTargetDirPath);

                if (!Files.exists(correspondingSourceDirPath)) {
                    // 如果源目录不存在
                    try (Stream<Path> dirContents = Files.list(targetDirPath)) {
                        if (!dirContents.findAny().isPresent()) {
                            // 且目标目录为空，则删除目标目录
                            try {
                                Files.delete(targetDirPath); // 使用 Files.delete()，如果目录非空会抛出 DirectoryNotEmptyException
                                log.info("全量扫描：已删除空的目标目录 {}", targetDirPath);
                            } catch (DirectoryNotEmptyException dne) {
                                // 这个警告是合理的，因为文件删除是异步的，或者目录可能包含其他未追踪的文件/目录
                                log.warn("全量扫描：尝试删除目录 {} 失败，因为它非空。这可能是因为文件删除尚未完成，或包含未追踪的内容。", targetDirPath);
                            } catch (IOException e) {
                                log.error("全量扫描：删除空的目标目录 {} 时出错。", targetDirPath, e);
                            }
                        } else {
                            log.debug("目标目录 {} 的源目录 {} 不存在，但目标目录非空（可能包含待删除文件或未追踪内容），暂不删除。", targetDirPath, correspondingSourceDirPath);
                        }
                    } catch (NoSuchFileException nsfe) {
                        // 如果在检查 Files.list() 时目录已被并发删除（例如由另一个操作或手动），则忽略
                        log.warn("全量扫描：检查目录 {} 内容时目录已不存在，可能已被其他进程删除。", targetDirPath);
                    } catch (IOException e) {
                        log.error("全量扫描：检查目录 {} 内容时出错。", targetDirPath, e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("全量扫描：遍历目标目录以清理空目录时出错。", e);
        }
        log.info("全量扫描：空目录清理阶段完成。");
        // +++ 目录清理结束 +++

        long endTime = System.currentTimeMillis();
        log.info("定时全量扫描执行完毕，耗时: {} 毫秒", (endTime - startTime));
    }

    private Map<String, FileSystemInfo> scanSourceDirectory() {
        Map<String, FileSystemInfo> fileSystemState = new HashMap<>();
        try (Stream<Path> stream = Files.walk(sourceDirectory)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    Path relativePath = sourceDirectory.relativize(path.getParent());
                    String relativeDirPath = formatRelativePath(relativePath);
                    String originalFilename = path.getFileName().toString();
                    String key = relativeDirPath + "||" + originalFilename;

                    Instant originalInstant = Files.getLastModifiedTime(path).toInstant();
                    Instant truncatedInstant = originalInstant.truncatedTo(ChronoUnit.SECONDS);
                    LocalDateTime lastModifiedTruncatedToSecond = LocalDateTime.ofInstant(truncatedInstant, ZoneId.systemDefault());

                    fileSystemState.put(key, new FileSystemInfo(path, lastModifiedTruncatedToSecond));
                } catch (IOException | SecurityException | InvalidPathException e) {
                    log.error("扫描文件 {} 时读取属性或计算路径出错，跳过该文件。", path, e);
                }
            });
            return fileSystemState;
        } catch (IOException | SecurityException e) {
            log.error("扫描源目录 {} 时出错。", sourceDirectory, e);
            return null;
        }
    }

    @Transactional(readOnly = true)
    protected Map<String, FileSyncMap> getDatabaseState() {
        List<FileSyncMap> dbRecords = fileSyncMapMapper.selectAllRelevantForScan();
        return dbRecords.stream()
                .collect(Collectors.toMap(
                        record -> record.getRelativeDirPath() + "||" + record.getOriginalFilename(),
                        Function.identity(),
                        (existing, replacement) -> {
                            log.warn("数据库中发现重复的路径和文件名组合: {}{}", existing.getRelativeDirPath(), existing.getOriginalFilename());
                            return existing;
                        }
                ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateDeletionStatusInNewTransaction(List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            int updatedRows = fileSyncMapMapper.batchUpdateStatus(ids, STATUS_PENDING_DELETION);
            log.info("已将 {} 条记录的状态标记为 '{}'。", updatedRows, STATUS_PENDING_DELETION);
        }
    }

    private static class FileSystemInfo {
        final Path fullPath;
        final LocalDateTime lastModifiedTime;

        FileSystemInfo(Path fullPath, LocalDateTime lastModifiedTime) {
            this.fullPath = fullPath;
            this.lastModifiedTime = lastModifiedTime;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<PendingFileSyncDto> getPendingSyncFiles(int page, int size) {
        log.debug("获取待处理文件列表 (pending_sync & pending_deletion), 页码: {}, 大小: {}", page, size);
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        List<FileSyncMap> pendingSyncMaps = fileSyncMapMapper.selectByStatus(STATUS_PENDING);
        List<FileSyncMap> pendingDeletionMaps = fileSyncMapMapper.selectByStatus(STATUS_PENDING_DELETION);

        List<FileSyncMap> allPendingMaps = new ArrayList<>();
        allPendingMaps.addAll(pendingSyncMaps);
        allPendingMaps.addAll(pendingDeletionMaps);

        allPendingMaps.sort(Comparator.comparing(FileSyncMap::getLastUpdated, Comparator.nullsLast(Comparator.reverseOrder())));

        long totalElements = allPendingMaps.size();
        int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
        int currentPage = Math.max(1, Math.min(page, totalPages == 0 ? 1 : totalPages));
        int offset = (currentPage - 1) * size;
        int endIndex = Math.min(offset + size, (int) totalElements);
        List<FileSyncMap> mapsOnPage = (offset < endIndex) ? allPendingMaps.subList(offset, endIndex) : Collections.emptyList();

        List<PendingFileSyncDto> dtoList = mapsOnPage.stream()
                .map(this::mapToPendingDto)
                .collect(Collectors.toList());

        log.debug("返回 {} 条待处理记录，用于页码 {}", dtoList.size(), currentPage);
        return new PageDto<>(dtoList, currentPage, size, totalElements);
    }

    private PendingFileSyncDto mapToPendingDto(FileSyncMap entity) {
        long fileSize = -1;
        LocalDateTime lastModifiedSeconds = null;
        try {
            Path tempFilePath = tempDirectory.resolve(entity.getTempFilename());
            if (Files.exists(tempFilePath)) {
                BasicFileAttributes attrs = Files.readAttributes(tempFilePath, BasicFileAttributes.class);
                fileSize = attrs.size();

                Instant originalInstant = attrs.lastModifiedTime().toInstant();
                Instant truncatedInstant = originalInstant.truncatedTo(ChronoUnit.SECONDS);
                lastModifiedSeconds = LocalDateTime.ofInstant(truncatedInstant, ZoneId.systemDefault());

            } else {
                if (!STATUS_PENDING_DELETION.equals(entity.getStatus())) {
                    log.warn("临时文件 {} 未找到 (状态: {})", tempFilePath, entity.getStatus());
                }
            }
        } catch (IOException e) {
            log.error("读取临时文件 {} 属性时出错", entity.getTempFilename(), e);
        }
        String formattedDate = (lastModifiedSeconds != null) ? DateUtils.formatDateTime(DateUtils.convertlocalDateTimeToDate(lastModifiedSeconds)) : "N/A";

        return new PendingFileSyncDto(
                entity.getId(),
                entity.getTempFilename(),
                entity.getOriginalFilename(),
                entity.getRelativeDirPath(),
                fileSize,
                formattedDate,
                entity.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FileSyncStatusDto getSyncStatus() {
        long pendingCount = fileSyncMapMapper.countByStatus(STATUS_PENDING);
        long syncedCount = fileSyncMapMapper.countByStatus(STATUS_SYNCED);
        long errorCopyingCount = fileSyncMapMapper.countByStatus(STATUS_ERROR_COPYING);
        long errorSyncingCount = fileSyncMapMapper.countByStatus(STATUS_ERROR_SYNCING);
        long syncingCount = fileSyncMapMapper.countByStatus(STATUS_SYNCING);
        long pendingDeletionCount = fileSyncMapMapper.countByStatus(STATUS_PENDING_DELETION);

        return new FileSyncStatusDto(
                monitoringActive.get(),
                pendingCount,
                syncedCount,
                errorCopyingCount + errorSyncingCount + syncingCount + pendingDeletionCount,
                syncProcessStatus.get(),
                processedInCurrentRun.get(),
                failedInCurrentRun.get()
        );
    }

    // --- 手动同步控制方法 ---
    @Override
    public FileSyncTaskControlResultDto startManualSync() {
        log.info("Attempting to start manual sync process...");
        if (syncProcessStatus.compareAndSet("idle", "running")) {
            log.info("Starting new manual sync task.");
            pauseFlag.set(false);
            cancelFlag.set(false);
            processedInCurrentRun.set(0);
            failedInCurrentRun.set(0);

            FileSyncService self = applicationContext.getBean(FileSyncService.class);
            Future<?> future = ((FileSyncServiceImpl) self).runSyncCycle();
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
            if (syncProcessStatus.compareAndSet(currentStatus, "stopping")) {
                cancelFlag.set(true);
                pauseFlag.set(false);
                log.info("Stop signal sent to manual sync process. Status set to stopping.");

                Future<?> future = currentSyncTaskFuture.get();
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                    log.info("Attempted to cancel the running sync task future.");
                }
                return new FileSyncTaskControlResultDto(true, "停止信号已发送。", "stopping");
            } else {
                log.warn("Could not set status to stopping, current status is now: {}", syncProcessStatus.get());
                return new FileSyncTaskControlResultDto(false, "无法停止，当前状态已改变: " + syncProcessStatus.get() + ".", syncProcessStatus.get());
            }
        } else {
            log.warn("Cannot stop manual sync. Current status: {}", currentStatus);
            return new FileSyncTaskControlResultDto(false, "无法停止，当前状态: " + currentStatus + ".", currentStatus);
        }
    }


    // --- Asynchronous Sync Cycle ---
    @Async
    public Future<?> runSyncCycle() {
        log.info("Starting asynchronous sync cycle...");
        try {
            while (!cancelFlag.get()) {
                while (pauseFlag.get() && !cancelFlag.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.warn("Sync cycle interrupted during pause. Checking cancel flag.");
                        Thread.currentThread().interrupt();
                        if (cancelFlag.get()) break;
                    }
                }
                if (cancelFlag.get()) break;

                List<FileSyncMap> batchToProcess = selectAndProcessBatch();

                if (CollectionUtils.isEmpty(batchToProcess)) {
                    log.info("No more pending files found in this cycle.");
                    break;
                }

                log.info("Processing batch of {} files...", batchToProcess.size());
                for (FileSyncMap record : batchToProcess) {
                    if (cancelFlag.get()) {
                        log.info("Cancel flag detected during batch processing. Aborting current batch.");
                        break;
                    }
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
                    processSingleFileSyncRecord(record);
                }
            }
        } catch (Exception e) {
            log.error("Unhandled exception in asynchronous sync cycle!", e);
        } finally {
            log.info("Asynchronous sync cycle finished. Processed: {}, Failed: {}. Cancelled: {}",
                    processedInCurrentRun.get(), failedInCurrentRun.get(), cancelFlag.get());
            syncProcessStatus.compareAndSet("running", "idle");
            syncProcessStatus.compareAndSet("paused", "idle");
            syncProcessStatus.compareAndSet("stopping", "idle");
            currentSyncTaskFuture.set(null);
        }
        return null;
    }

    /**
     * Selects a batch of pending files, locks them by updating status to 'syncing'.
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
                    log.error("Mismatch between locked rows ({}) and updated rows ({}). Rolling back batch selection.", batch.size(), updatedRows);
                    transactionManager.rollback(status);
                    return Collections.emptyList();
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
            return Collections.emptyList();
        }
    }


    /**
     * Processes a single FileSyncMap record: moves the file and updates its status.
     * 当前逻辑:
     * 文件从 tempDirectory 移动到 targetDirectory.
     * 如果成功，finalStatus 设置为 STATUS_SYNCED.
     * 调用 updateFileSyncStatusInNewTransaction(record.getId(), finalStatus) 在新事务中更新数据库状态
     */
    private void processSingleFileSyncRecord(FileSyncMap record) {
        Path tempFilePath = tempDirectory.resolve(record.getTempFilename());
        String finalStatus = STATUS_ERROR_SYNCING;
        Path targetPathGlobal = null;
        long targetFileSizeInBytes = -1;
        long targetFileLastModifiedEpochSeconds = -1;

        try {
            if (!Files.exists(tempFilePath)) {
                log.error("Temporary file {} for record ID {} not found. Setting status to error.", tempFilePath, record.getId());
            } else {
                String targetFilename = generateTargetFilename(record.getOriginalFilename());
                Path targetPath = targetDirectory.resolve(record.getRelativeDirPath()).resolve(targetFilename).normalize();
                targetPathGlobal = targetPath;

                if (!targetPath.startsWith(targetDirectory)) {
                    log.error("Target path traversal attempt for record ID {}: {}", record.getId(), targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.move(tempFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Successfully moved temp file {} to target {}", tempFilePath, targetPath);
                    finalStatus = STATUS_SYNCED;
                    processedInCurrentRun.incrementAndGet();

                    BasicFileAttributes attrs = Files.readAttributes(targetPath, BasicFileAttributes.class);
                    targetFileSizeInBytes = attrs.size();
                    targetFileLastModifiedEpochSeconds = attrs.lastModifiedTime().toInstant().truncatedTo(ChronoUnit.SECONDS).getEpochSecond();
                }
            }
        } catch (IOException e) {
            log.error("IOException during file move for record ID {}. Temp: {}, Target attempt: {}. Setting status to error.",
                    record.getId(), tempFilePath, record.getRelativeDirPath() + generateTargetFilename(record.getOriginalFilename()), e);
        } catch (Exception e) {
            log.error("Unexpected exception during file processing for record ID {}. Setting status to error.", record.getId(), e);
        }

        if (STATUS_ERROR_SYNCING.equals(finalStatus)) {
            failedInCurrentRun.incrementAndGet();
        }
        updateFileSyncStatusInNewTransaction(record.getId(), finalStatus, record,
                targetPathGlobal, targetFileSizeInBytes, targetFileLastModifiedEpochSeconds);
    }

    /**
     * Updates the status of a single record in a new transaction.
     * And publishes Kafka event if successful and status is SYNCED.
     */
    private void updateFileSyncStatusInNewTransaction(Long id, String status, FileSyncMap recordForEvent,
                                                      Path targetFullPathForEvent, long targetSizeForEvent, long targetModTimeForEvent) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus txStatus = transactionManager.getTransaction(def);
        try {
            int updated = fileSyncMapMapper.updateStatusById(id, status);
            transactionManager.commit(txStatus); // Commit DB transaction first
            if (updated > 0) {
                log.debug("Updated status to '{}' for record ID {}", status, id);
                if (STATUS_SYNCED.equals(status) && recordForEvent != null && targetFullPathForEvent != null) {
                    // +++ 调用 Kafka 事件发布 +++
                    publishFileUpsertEvent(recordForEvent, targetFullPathForEvent, targetSizeForEvent, targetModTimeForEvent);
                    log.info("已成功在消息队列发布'{}'新增事件。", id);
                }
            } else {
                log.warn("Could not update status to '{}' for record ID {} (record might have been deleted concurrently?)", status, id);
            }
        } catch (Exception e) {
            log.error("Failed to update status to '{}' for record ID {}. Rolling back status update.", status, id, e);
            try {
                if (!txStatus.isCompleted()) { // 确保事务未完成才回滚
                    transactionManager.rollback(txStatus);
                }
            } catch (Exception rbEx) {
                log.error("Error during rollback of status update for record ID {}.", id, rbEx);
            }
            // 如果DB提交失败，就不应该发送Kafka消息
        }
    }

    /**
     * Generates the final filename for the target directory based on configuration.
     */
    private String generateTargetFilename(String originalFilename) {
        if (StringUtils.hasText(targetFilenameRemoveSuffix) && originalFilename.endsWith(targetFilenameRemoveSuffix)) {
            return originalFilename.substring(0, originalFilename.length() - targetFilenameRemoveSuffix.length());
        }
        return originalFilename;
    }


    // --- 用户确认删除的处理方法 ---
    @Override
    @Transactional(propagation = Propagation.NEVER)
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
            boolean deleted = processSingleDeletionConfirmation(id);
            if (deleted) {
                successCount++;
            } else {
                failCount++;
            }
        }
        log.info("用户确认删除处理完成。成功: {}, 失败: {}", successCount, failCount);
    }

    /**
     * 处理单个文件的删除确认（包含文件删除和DB记录删除）。
     * 当前逻辑:
     *
     * 查询并验证记录状态。
     * 删除目标目录中的文件。
     * 删除临时目录中的文件。
     * 从数据库中删除记录 (fileSyncMapMapper.deleteById(id)).
     * 提交事务 (transactionManager.commit(txStatus)).
     */
    private boolean processSingleDeletionConfirmation(Long id) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(def);
        FileSyncMap record = null;

        try {
            record = fileSyncMapMapper.selectByStatus(STATUS_PENDING_DELETION)
                    .stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (record == null) {
                log.warn("确认删除失败：未找到 ID 为 {} 的记录，或其状态不是 '{}'。", id, STATUS_PENDING_DELETION);
                transactionManager.rollback(txStatus);
                return false;
            }

            String targetFilename = generateTargetFilename(record.getOriginalFilename());
            Path targetFilePath = targetDirectory.resolve(record.getRelativeDirPath()).resolve(targetFilename).normalize();
            if (!targetFilePath.startsWith(targetDirectory)) {
                log.error("确认删除失败：目标路径 {} 超出范围。", targetFilePath);
                transactionManager.rollback(txStatus);
                return false;
            }
            try {
                Files.deleteIfExists(targetFilePath); // 尝试删除目标文件
                log.info("删除解密文件 {} (尝试操作)", targetFilePath);
            } catch (IOException | SecurityException e) {
                log.error("确认删除失败：删除解密文件 {} 时出错。", targetFilePath, e);
                transactionManager.rollback(txStatus);
                return false;
            }

            Path tempFilePath = tempDirectory.resolve(record.getTempFilename());
            try {
                Files.deleteIfExists(tempFilePath); // 尝试删除临时文件
                log.info("删除临时文件 {} (尝试操作)", tempFilePath);
            } catch (IOException | SecurityException e) {
                log.error("确认删除失败：删除临时文件 {} 时出错。", tempFilePath, e);
                transactionManager.rollback(txStatus);
                return false;
            }

            int deletedRows = fileSyncMapMapper.deleteById(id);
            if (deletedRows == 0) {
                log.warn("确认删除失败：删除数据库记录 ID {} 时未找到或已被删除。", id);
                transactionManager.rollback(txStatus);
                return false;
            }

            transactionManager.commit(txStatus); // 提交数据库事务
            log.info("已成功确认并删除与记录 ID {} 相关的文件和数据。", id);

            // +++ 在数据库事务成功提交后，发布删除事件 +++
            publishFileDeleteEvent(record);
            log.info("已成功在消息队列发布 {} 删除事件。", id);
            return true;

        } catch (Exception e) {
            log.error("处理删除确认 ID {} 时发生意外错误。", id, e);
            try {
                if (!txStatus.isCompleted()) {
                    transactionManager.rollback(txStatus);
                }
            } catch (Exception rbEx) {
                log.error("回滚删除确认事务 ID {} 时出错。", id, rbEx);
            }
            return false;
        }
    }

    // +++ 新增 Kafka 事件发布辅助方法 (已在Canvas中定义，这里是实际实现) +++
    private void publishFileUpsertEvent(FileSyncMap fileRecord, Path targetFullPath, long targetSize, long targetModTimeEpochSeconds) {
        String eventId = UUID.randomUUID().toString();
        String esDocumentId = generateElasticsearchDocumentId(fileRecord.getRelativeDirPath(), fileRecord.getOriginalFilename());

        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("eventId", eventId);
        messagePayload.put("eventType", "FILE_UPSERTED");
        messagePayload.put("eventTimestamp", Instant.now().toString());
        messagePayload.put("fileSyncMapId", fileRecord.getId());
        messagePayload.put("sourceRelativePath", fileRecord.getRelativeDirPath());
        messagePayload.put("sourceFilename", fileRecord.getOriginalFilename()); // 这是源（加密）文件名

        Path targetRelativeDir = targetDirectory.relativize(targetFullPath.getParent());
        messagePayload.put("targetRelativePath", formatRelativePath(targetRelativeDir));
        messagePayload.put("targetFilename", targetFullPath.getFileName().toString()); // 这是目标（解密后）文件名
        messagePayload.put("targetFileSizeInBytes", targetSize);
        messagePayload.put("targetFileLastModifiedEpochSeconds", targetModTimeEpochSeconds);
        messagePayload.put("elasticsearchDocumentId", esDocumentId);

        try {
            String jsonMessage = objectMapper.writeValueAsString(messagePayload);
            log.info("Publishing FILE_UPSERTED event to Kafka topic '{}' with key '{}': {}", TOPIC_FILE_UPSERT_EVENTS, esDocumentId, jsonMessage);
            kafkaTemplate.send(TOPIC_FILE_UPSERT_EVENTS, esDocumentId, jsonMessage); // 使用 esDocumentId 作为 Kafka key
        } catch (JsonProcessingException e) {
            log.error("Error serializing FILE_UPSERTED event to JSON for record ID {}: {}", fileRecord.getId(), e.getMessage(), e);
        } catch (Exception e) { // KafkaException 等
            log.error("Error publishing FILE_UPSERTED event to Kafka for record ID {}: {}", fileRecord.getId(), e.getMessage(), e);
            // 考虑更复杂的错误处理，如重试或发送到死信队列，但对于初版，记录日志是基础
        }
    }

    private void publishFileDeleteEvent(FileSyncMap fileRecord) {
        String eventId = UUID.randomUUID().toString();
        String esDocumentId = generateElasticsearchDocumentId(fileRecord.getRelativeDirPath(), fileRecord.getOriginalFilename());

        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("eventId", eventId);
        messagePayload.put("eventType", "FILE_DELETED");
        messagePayload.put("eventTimestamp", Instant.now().toString());
        messagePayload.put("elasticsearchDocumentId", esDocumentId);
        messagePayload.put("sourceRelativePath", fileRecord.getRelativeDirPath());
        messagePayload.put("sourceFilename", fileRecord.getOriginalFilename());

        try {
            String jsonMessage = objectMapper.writeValueAsString(messagePayload);
            log.info("Publishing FILE_DELETED event to Kafka topic '{}' with key '{}': {}", TOPIC_FILE_DELETE_EVENTS, esDocumentId, jsonMessage);
            kafkaTemplate.send(TOPIC_FILE_DELETE_EVENTS, esDocumentId, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Error serializing FILE_DELETED event to JSON for record ID {}: {}", fileRecord.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error publishing FILE_DELETED event to Kafka for record ID {}: {}", fileRecord.getId(), e.getMessage(), e);
        }
    }

    /**
     * 根据源文件的相对目录路径和原始文件名生成 Elasticsearch 文档 ID。
     * 使用 SHA-256 哈希以确保唯一性和固定长度。
     * @param sourceRelativeDirPath 源文件的相对目录路径 (例如 "docs/projectA/")
     * @param sourceOriginalFilename 源文件的原始文件名 (例如 "report.docx.enc")
     * @return SHA-256 哈希字符串作为 ES 文档 ID
     */
    private String generateElasticsearchDocumentId(String sourceRelativeDirPath, String sourceOriginalFilename) {
        String normalizedDirPath = sourceRelativeDirPath;
        if (normalizedDirPath == null) {
            normalizedDirPath = ""; // 处理 null 情况
        }
        if (!normalizedDirPath.isEmpty() && !normalizedDirPath.endsWith("/")) {
            normalizedDirPath += "/";
        }
        if (normalizedDirPath.equals("/")) { // 避免根目录变成 "//"
            normalizedDirPath = "";
        }

        String uniqueFileIdentifier = normalizedDirPath + sourceOriginalFilename;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uniqueFileIdentifier.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found for generating Elasticsearch document ID. Falling back to plain identifier (NOT RECOMMENDED).", e);
            return uniqueFileIdentifier.replaceAll("[^a-zA-Z0-9_\\-/.]", "_");
        }
    }
}
