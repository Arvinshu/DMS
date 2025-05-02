/**
 * 目录: src/main/java/org/ls/service/impl/FileSyncServiceImpl.java
 * 文件名: FileSyncServiceImpl.java
 * 开发时间: 2025-04-29 10:12:00 EDT (Part 2: Async Control Logic)
 * 作者: Gemini
 * 用途: 文件同步服务实现类，负责后台监控源目录、管理数据库记录以及提供同步状态和控制接口。
 * (包含后台监控、状态查询、待同步查询、异步手动同步控制逻辑)
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
import org.ls.utils.DateUtils; // Assuming DateUtils exists
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor; // Use Spring's TaskExecutor
import org.springframework.scheduling.annotation.Async;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Future; // Import Future
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileSyncServiceImpl implements FileSyncService {

    private final FileSyncMapMapper fileSyncMapMapper;
    private final Environment env;
    private final TaskExecutor taskExecutor; // Inject TaskExecutor
    private final PlatformTransactionManager transactionManager; // For programmatic transactions
    private final ApplicationContext applicationContext; // To get self-proxy for @Async calls

    private final Path sourceDirectory;
    private final Path tempDirectory;
    private final Path targetDirectory;
    private final boolean monitoringEnabled;
    private final String targetFilenameRemoveSuffix; // Configurable suffix to remove

    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();
    private final AtomicBoolean monitoringActive = new AtomicBoolean(false);
    private Thread monitoringThread;

    // --- Status and Control for Manual Sync ---
    private final AtomicReference<String> syncProcessStatus = new AtomicReference<>("idle"); // idle, running, paused, stopping
    private final AtomicBoolean pauseFlag = new AtomicBoolean(false);
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);
    private final AtomicReference<Future<?>> currentSyncTaskFuture = new AtomicReference<>(null); // Store Future of the async task
    private final AtomicInteger processedInCurrentRun = new AtomicInteger(0);
    private final AtomicInteger failedInCurrentRun = new AtomicInteger(0);
    private static final int SYNC_BATCH_SIZE = 10; // Number of files to process per batch


    private static final String STATUS_PENDING = "pending_sync";
    private static final String STATUS_SYNCED = "synced";
    private static final String STATUS_ERROR_COPYING = "error_copying";
    private static final String STATUS_ERROR_SYNCING = "error_syncing";
    private static final String STATUS_SYNCING = "syncing";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";


    @Autowired
    public FileSyncServiceImpl(FileSyncMapMapper fileSyncMapMapper, Environment env,
                               @Qualifier("taskExecutor") TaskExecutor taskExecutor,
                               PlatformTransactionManager transactionManager,
                               ApplicationContext applicationContext) {
        this.fileSyncMapMapper = fileSyncMapMapper;
        this.env = env;
        this.taskExecutor = taskExecutor;
        this.transactionManager = transactionManager;
        this.applicationContext = applicationContext;

        // Load and validate directory configurations
        this.sourceDirectory = getRequiredDirectoryPath("file.sync.source-dir");
        this.tempDirectory = getRequiredDirectoryPath("file.sync.temp-dir");
        this.targetDirectory = getRequiredDirectoryPath("file.sync.target-dir");
        this.monitoringEnabled = Boolean.parseBoolean(env.getProperty("file.sync.enabled", "false"));
        this.targetFilenameRemoveSuffix = env.getProperty("file.sync.target-filename.remove-suffix"); // Get suffix config

        log.info("FileSyncService initialized.");
        log.info("Source Directory: {}", this.sourceDirectory);
        log.info("Temp Directory: {}", this.tempDirectory);
        log.info("Target Directory: {}", this.targetDirectory);
        log.info("Monitoring Enabled: {}", this.monitoringEnabled);
        log.info("Target Filename Remove Suffix: '{}'", this.targetFilenameRemoveSuffix);
    }

    /**
     * Helper to get and validate required directory paths from environment properties.
     */
    private Path getRequiredDirectoryPath(String propertyKey) {
        String pathStr = env.getProperty(propertyKey);
        if (!StringUtils.hasText(pathStr)) {
            log.error("Configuration property '{}' is not set.", propertyKey);
            throw new IllegalStateException("Required directory path '" + propertyKey + "' is not configured.");
        }
        Path path = Paths.get(pathStr).toAbsolutePath();
        try {
            if (propertyKey.equals("file.sync.source-dir")) {
                if (!Files.exists(path) || !Files.isDirectory(path)) {
                    log.error("Source directory '{}' does not exist or is not a directory: {}", propertyKey, path);
                    throw new IllegalStateException("Source directory must exist and be a directory.");
                }
                if (!Files.isReadable(path)) {
                    log.error("Source directory '{}' is not readable: {}", propertyKey, path);
                    throw new IllegalStateException("Source directory must be readable.");
                }
            } else {
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    log.info("Created directory for '{}': {}", propertyKey, path);
                } else if (!Files.isDirectory(path)) {
                    log.error("Configured path for '{}' is not a directory: {}", propertyKey, path);
                    throw new IllegalStateException("Configured path '" + propertyKey + "' is not a directory.");
                }
                if (!Files.isWritable(path)) {
                    log.error("Directory '{}' is not writable: {}", propertyKey, path);
                    throw new IllegalStateException("Directory '" + propertyKey + "' must be writable.");
                }
            }
        } catch (IOException e) {
            log.error("Failed to access or create directory for '{}': {}", propertyKey, path, e);
            throw new IllegalStateException("Failed to initialize directory path '" + propertyKey + "'.", e);
        }
        return path;
    }

    // --- Lifecycle Management for Background Monitoring ---

    @PostConstruct
    public void initializeMonitoring() {
        if (!monitoringEnabled) {
            log.info("Background file monitoring is disabled via configuration.");
            return;
        }
        if (monitoringActive.compareAndSet(false, true)) {
            log.info("Initializing background file monitoring...");
            try {
                watchService = FileSystems.getDefault().newWatchService();
                registerDirectoryTree(sourceDirectory);
                monitoringThread = new Thread(this::processWatchEvents, "FileSyncWatcher");
                monitoringThread.setDaemon(true); // Allow JVM to exit if this is the only thread left
                monitoringThread.start();
                log.info("Background file monitoring started successfully.");
            } catch (IOException e) {
                log.error("Failed to initialize WatchService or register directories. Monitoring NOT started.", e);
                monitoringActive.set(false);
                closeWatchService();
            }
        } else {
            log.warn("Monitoring initialization called but already active.");
        }
    }

    @PreDestroy
    public void shutdownMonitoring() {
        if (monitoringActive.compareAndSet(true, false)) {
            log.info("Shutting down background file monitoring...");
            closeWatchService();
            if (monitoringThread != null && monitoringThread.isAlive()) {
                monitoringThread.interrupt(); // Interrupt the thread if it's waiting in take()
                try {
                    monitoringThread.join(5000); // Wait for thread to finish
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for monitoring thread to shut down.");
                    Thread.currentThread().interrupt();
                }
            }
            log.info("Background file monitoring shutdown complete.");
        }
        // Also attempt to cancel any running manual sync task on shutdown
        if (syncProcessStatus.get().equals("running") || syncProcessStatus.get().equals("paused")) {
            log.info("Attempting to stop manual sync task during application shutdown...");
            stopManualSync();
            Future<?> future = currentSyncTaskFuture.get();
            if (future != null) {
                future.cancel(true); // Attempt to interrupt the async task
            }
        }
    }

    private void closeWatchService() {
        if (watchService != null) {
            try {
                watchService.close();
                log.info("WatchService closed.");
            } catch (IOException e) {
                log.error("Error closing WatchService.", e);
            } finally {
                watchService = null;
                watchKeys.clear();
            }
        }
    }

    private void registerDirectoryTree(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws IOException {
        if (!Files.isReadable(dir)) {
            log.warn("Directory is not readable, skipping registration: {}", dir);
            return;
        }
        try {
            WatchKey key = dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            watchKeys.put(key, dir);
            log.debug("Registered directory for watching: {}", dir);
        } catch (IOException e) {
            log.error("Failed to register directory for watching: {}", dir, e);
            throw e;
        }
    }

    private void processWatchEvents() {
        log.info("Watch event processing thread started.");
        while (monitoringActive.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                log.info("Monitoring thread interrupted. Exiting.");
                Thread.currentThread().interrupt();
                monitoringActive.set(false);
                break;
            } catch (ClosedWatchServiceException e) {
                log.info("WatchService closed. Exiting monitoring thread.");
                monitoringActive.set(false);
                break;
            }

            Path watchedDir = watchKeys.get(key);
            if (watchedDir == null) {
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    log.warn("WatchService OVERFLOW event detected for directory: {}. Some events might have been lost.", watchedDir);
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path relativePathFromWatchedDir = ev.context();
                Path fullPath = watchedDir.resolve(relativePathFromWatchedDir);

                log.debug("Event detected: {} for path: {}", kind.name(), fullPath);

                try {
                    // Use self-proxy to ensure @Transactional works correctly when calling from within the same class
                    FileSyncService self = applicationContext.getBean(FileSyncService.class);

                    if (Files.isDirectory(fullPath, LinkOption.NOFOLLOW_LINKS)) {
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            log.info("New directory detected: {}. Registering for monitoring.", fullPath);
                            registerDirectoryTree(fullPath);
                        }
                    } else {
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            // Need to call via self-proxy for transactionality
                            ((FileSyncServiceImpl) self).handleFileCreateOrModify(fullPath);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            // Need to call via self-proxy for transactionality
                            ((FileSyncServiceImpl) self).handleFileDelete(fullPath);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing watch event for path: {}", fullPath, e);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                log.warn("WatchKey for directory {} is no longer valid. Removing from watch list.", watchedDir);
                watchKeys.remove(key);
                // Optional: Cleanup DB records for the removed directory path
                // handleDirectoryDeleteCleanup(watchedDir);
            }
        }
        log.info("Watch event processing thread finished.");
    }


    // --- Event Handling Logic (Transactional methods called via self-proxy) ---

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFileCreateOrModify(Path sourceFilePath) { // Made public for self-proxy call
        if (!Files.isRegularFile(sourceFilePath)) {
            log.debug("Skipping non-regular file event: {}", sourceFilePath);
            return;
        }
        log.info("Processing CREATE/MODIFY event for: {}", sourceFilePath);
        try {
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();

            FileSyncMap existingRecord = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);
            String tempFilename;
            boolean isNewEntry = false;

            if (existingRecord == null) {
                isNewEntry = true;
                tempFilename = generateUniqueTempFilename(originalFilename);
            } else {
                tempFilename = existingRecord.getTempFilename();
                if (STATUS_ERROR_COPYING.equals(existingRecord.getStatus()) || STATUS_ERROR_SYNCING.equals(existingRecord.getStatus())) {
                    log.info("Overwriting entry with previous error status for source: {}", sourceFilePath);
                } else if (STATUS_SYNCING.equals(existingRecord.getStatus()) || STATUS_SYNCED.equals(existingRecord.getStatus())) {
                    log.info("Source file {} modified. Resetting status to pending.", sourceFilePath);
                }
            }

            Path tempFilePath = tempDirectory.resolve(tempFilename);
            Files.copy(sourceFilePath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Copied source file {} to temp file {}", sourceFilePath, tempFilePath);

            if (isNewEntry) {
                FileSyncMap newRecord = new FileSyncMap(null, relativeDirPath, originalFilename, tempFilename, STATUS_PENDING, null);
                fileSyncMapMapper.insert(newRecord);
                log.debug("Inserted new record into DB for {}", tempFilename);
            } else {
                fileSyncMapMapper.updateStatusByTempFilename(tempFilename, STATUS_PENDING);
                log.debug("Updated record status to pending for {}", tempFilename);
            }

        } catch (NoSuchFileException e) {
            log.warn("File disappeared during processing (CREATE/MODIFY): {}", sourceFilePath, e);
        } catch (IOException e) {
            log.error("Error copying file {} to temp directory. Rolling back transaction.", sourceFilePath, e);
            updateStatusOnError(sourceFilePath, STATUS_ERROR_COPYING);
            throw new RuntimeException("Failed to copy file: " + sourceFilePath, e);
        } catch (Exception e) {
            log.error("Unexpected error processing file {}. Rolling back transaction.", sourceFilePath, e);
            updateStatusOnError(sourceFilePath, STATUS_ERROR_COPYING);
            throw new RuntimeException("Unexpected error processing file: " + sourceFilePath, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFileDelete(Path sourceFilePath) { // Made public for self-proxy call
        log.info("Processing DELETE event for potential file: {}", sourceFilePath);
        try {
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();

            FileSyncMap recordToDelete = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);

            if (recordToDelete != null) {
                Path tempFilePath = tempDirectory.resolve(recordToDelete.getTempFilename());
                boolean deleted = Files.deleteIfExists(tempFilePath);
                log.info("Deleted temp file: {} (Success: {})", tempFilePath, deleted);

                int deletedRows = fileSyncMapMapper.deleteById(recordToDelete.getId());
                log.info("Deleted DB record for source file: {} (Rows affected: {})", sourceFilePath, deletedRows);
            } else {
                log.warn("No DB record found for deleted source file: {}", sourceFilePath);
            }
        } catch (IOException e) {
            log.error("Error deleting temp file for source {}. Rolling back transaction.", sourceFilePath, e);
            throw new RuntimeException("Failed to delete temp file: " + sourceFilePath, e);
        } catch (Exception e) {
            log.error("Unexpected error processing file deletion {}. Rolling back transaction.", sourceFilePath, e);
            throw new RuntimeException("Unexpected error processing file deletion: " + sourceFilePath, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusOnError(Path sourceFilePath, String errorStatus) { // Made public for self-proxy call
        try {
            Path relativePath = sourceDirectory.relativize(sourceFilePath.getParent());
            String relativeDirPath = formatRelativePath(relativePath);
            String originalFilename = sourceFilePath.getFileName().toString();
            FileSyncMap record = fileSyncMapMapper.selectBySourcePath(relativeDirPath, originalFilename);
            if (record != null) {
                fileSyncMapMapper.updateStatusById(record.getId(), errorStatus);
                log.warn("Updated status to {} for record ID {}", errorStatus, record.getId());
            } else {
                log.warn("Could not find record to update error status for: {}", sourceFilePath);
            }
        } catch (Exception dbEx) {
            log.error("Failed to update status to {} for file {}", errorStatus, sourceFilePath, dbEx);
        }
    }

    private String generateUniqueTempFilename(String originalFilename) {
        String baseName; String extension;
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            baseName = originalFilename.substring(0, dotIndex); extension = originalFilename.substring(dotIndex);
        } else { baseName = originalFilename; extension = ""; }
        String tempFilename = originalFilename; int counter = 1;
        while (fileSyncMapMapper.existsByTempFilename(tempFilename)) {
            tempFilename = String.format("%s_%d%s", baseName, counter++, extension);
            if (counter > 1000) { throw new IllegalStateException("Could not generate unique temporary filename."); }
        }
        if (counter > 1) { log.warn("Generated unique temp filename '{}' for original '{}'", tempFilename, originalFilename); }
        return tempFilename;
    }

    private String formatRelativePath(Path relativePath) {
        String pathStr = relativePath.toString();
        if (pathStr.isEmpty()) { return ""; }
        pathStr = pathStr.replace(FileSystems.getDefault().getSeparator(), "/");
        if (!pathStr.endsWith("/")) { pathStr += "/"; }
        return pathStr;
    }


    // --- Public Service Methods ---

    @Override
    @Transactional(readOnly = true)
    public PageDto<PendingFileSyncDto> getPendingSyncFiles(int page, int size) {
        log.debug("Fetching pending sync files, page: {}, size: {}", page, size);
        // IMPORTANT: Implement proper pagination in Mapper XML (LIMIT/OFFSET or use PageHelper)
        // Fetching all and slicing manually is NOT scalable.
        List<FileSyncMap> allPendingMaps = fileSyncMapMapper.selectByStatus(STATUS_PENDING);
        long totalElements = allPendingMaps.size();
        int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int offset = (currentPage - 1) * size;
        int endIndex = Math.min(offset + size, (int) totalElements);
        List<FileSyncMap> pendingMapsOnPage = (offset < endIndex) ? allPendingMaps.subList(offset, endIndex) : new ArrayList<>();

        List<PendingFileSyncDto> dtoList = pendingMapsOnPage.stream()
                .map(this::mapToPendingDto)
                .collect(Collectors.toList());

        log.debug("Returning {} pending files for page {}", dtoList.size(), currentPage);
        return new PageDto<>(dtoList, currentPage, size, totalElements);
    }

    private PendingFileSyncDto mapToPendingDto(FileSyncMap entity) {
        long fileSize = -1; LocalDateTime lastModified = null;
        try {
            Path tempFilePath = tempDirectory.resolve(entity.getTempFilename());
            if (Files.exists(tempFilePath)) {
                BasicFileAttributes attrs = Files.readAttributes(tempFilePath, BasicFileAttributes.class);
                fileSize = attrs.size();
                lastModified = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
            } else { log.warn("Temporary file not found while creating DTO: {}", tempFilePath); }
        } catch (IOException e) { log.error("Error reading attributes for temp file: {}", entity.getTempFilename(), e); }

        // 将 LocalDateTime 转换为 Date
        // 虽然java 8以后更推荐使用 LocalDateTime 但是修改 DateUtils.java 后编译器报错，只好作罢
        // 后面有精力了将所有 Date 更换为 LocalDateTime
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = lastModified.atZone(zoneId);
        Date date = Date.from(zdt.toInstant());

        String formattedDate = (lastModified != null) ? DateUtils.formatDateTime(date) : "N/A";
        return new PendingFileSyncDto(entity.getId(), entity.getTempFilename(), entity.getOriginalFilename(), entity.getRelativeDirPath(), fileSize, formattedDate);
    }

    @Override
    @Transactional(readOnly = true)
    public FileSyncStatusDto getSyncStatus() {
        long pendingCount = fileSyncMapMapper.countByStatus(STATUS_PENDING);
        long syncedCount = fileSyncMapMapper.countByStatus(STATUS_SYNCED);
        long errorCopyingCount = fileSyncMapMapper.countByStatus(STATUS_ERROR_COPYING);
        long errorSyncingCount = fileSyncMapMapper.countByStatus(STATUS_ERROR_SYNCING);
        long syncingCount = fileSyncMapMapper.countByStatus(STATUS_SYNCING);

        return new FileSyncStatusDto(
                monitoringActive.get(),
                pendingCount,
                syncedCount,
                errorCopyingCount + errorSyncingCount + syncingCount, // Combine errors and active
                syncProcessStatus.get(),
                processedInCurrentRun.get(), // Get current run stats
                failedInCurrentRun.get()
        );
    }

    // --- Manual Sync Control Methods Implementation ---

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
            }
            else if ("stopping".equals(currentStatus)) {
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
                        try { Thread.sleep(500); } catch (InterruptedException e) {
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
                int updatedRows = fileSyncMapMapper.updateStatusForIds(idsToUpdate, STATUS_SYNCING);
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

}
