/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_sync.js
 * 文件名: filemanage_sync.js
 * 开发时间: 2025-04-29 10:35:20 EDT
 * 作者: Gemini
 * 用途: 处理文件管理页面中“文档同步管理”部分的逻辑。
 */

// Ensure dependencies are loaded first (api, ui)

const fileManageSync = (() => {

    // --- DOM Elements ---
    const pendingTableBody = document.getElementById('pending-files-table')?.querySelector('tbody');
    const pendingPaginationContainer = document.getElementById('pending-pagination');
    const startSyncBtn = document.getElementById('start-sync-btn');
    const pauseResumeSyncBtn = document.getElementById('pause-resume-sync-btn');
    const stopSyncBtn = document.getElementById('stop-sync-btn');
    const syncStatusSpan = document.getElementById('sync-process-status'); // For getting previous status

    // --- State ---
    let currentPage = 1;
    const pageSize = 10; // Or get from config/UI
    let syncStatusIntervalId = null;
    const SYNC_STATUS_POLL_INTERVAL = 5000; // Poll every 5 seconds

    // --- Core Functions ---

    /**
     * 加载并显示待同步文件列表的指定页面。
     * @param {number} page - 要加载的页码 (从 1 开始)。
     */
    const loadPendingFiles = (page) => {
        currentPage = page; // Update current page state
        console.log(`Sync Module: Loading pending files: page=${page}`);
        // Add loading indicator
        fileManageApi.getPendingFiles(page, pageSize)
            .then(pageDto => {
                fileManageUI.updatePendingFilesTable(pageDto, loadPendingFiles); // Pass self for pagination
            })
            .catch(error => {
                console.error('Sync Module: Error loading pending files:', error);
                alert(`加载待同步文件列表失败: ${error.message}`);
                fileManageUI.updatePendingFilesTable({ content: [], pageNumber: 1, totalPages: 0, totalElements: 0 }, loadPendingFiles); // Clear table
            });
        // Remove loading indicator
    };

    /**
     * 获取并更新同步状态显示。
     */
    const updateSyncStatus = () => {
        fileManageApi.getSyncStatus()
            .then(statusDto => {
                const previousStatus = syncStatusSpan ? syncStatusSpan.dataset.previousStatus || 'idle' : 'idle';
                const currentStatus = statusDto.syncProcessStatus || 'idle';

                // Update UI elements (counts, status text, buttons)
                fileManageUI.updateSyncStatusDisplay(statusDto);

                // --- Basic Progress Handling ---
                // Reload the pending list when sync finishes or is stopped to show remaining files.
                if (currentStatus === 'idle' && (previousStatus === 'running' || previousStatus === 'stopping')) {
                    console.log("Sync Module: Sync process finished or stopped, reloading pending files list.");
                    // Check if the current page might now be empty or out of bounds
                    // For simplicity, just reload the current page. A more robust solution
                    // might involve checking total count and adjusting page number.
                    loadPendingFiles(currentPage);
                }

                // Store current status for next check
                if(syncStatusSpan) syncStatusSpan.dataset.previousStatus = currentStatus;

                // Stop polling if the process is definitively idle and wasn't just stopping
                // Or keep polling indefinitely? Let's keep polling for now.
                // if (currentStatus === 'idle' && previousStatus !== 'stopping') {
                //     stopSyncStatusPolling();
                // }

            })
            .catch(error => {
                console.error('Sync Module: Error fetching sync status:', error);
                if(syncStatusSpan) syncStatusSpan.textContent = '状态获取失败';
                // Disable all buttons on error?
                fileManageUI.updateSyncButtons('error'); // Add an 'error' state handler in UI if needed
                stopSyncStatusPolling(); // Stop polling on error
            });
    };

    /**
     * 启动状态轮询。
     */
    const startSyncStatusPolling = () => {
        if (syncStatusIntervalId === null) {
            console.log(`Sync Module: Starting sync status polling every ${SYNC_STATUS_POLL_INTERVAL}ms`);
            updateSyncStatus(); // Immediate check first
            syncStatusIntervalId = setInterval(updateSyncStatus, SYNC_STATUS_POLL_INTERVAL);
        }
    };

    /**
     * 停止状态轮询。
     */
    const stopSyncStatusPolling = () => {
        if (syncStatusIntervalId !== null) {
            console.log('Sync Module: Stopping sync status polling.');
            clearInterval(syncStatusIntervalId);
            syncStatusIntervalId = null;
        }
    };


    // --- Event Handlers ---

    const handleStartSync = () => {
        console.log('Sync Module: Start Sync button clicked');
        if(startSyncBtn) {
            startSyncBtn.disabled = true;
            startSyncBtn.textContent = '启动中...';
        }
        fileManageApi.startSync()
            .then(result => {
                alert(result.message || '启动同步请求已发送');
                updateSyncStatus(); // Update UI immediately
                startSyncStatusPolling(); // Ensure polling is active
            })
            .catch(error => {
                console.error('Sync Module: Error starting sync:', error);
                alert(`启动同步失败: ${error.message}`);
                updateSyncStatus(); // Update UI to reflect potential state change failure
            });
    };

    const handlePauseResumeSync = () => {
        if (!pauseResumeSyncBtn) return;
        const currentAction = pauseResumeSyncBtn.textContent === '暂停' ? 'pause' : 'resume';
        console.log(`Sync Module: ${currentAction} Sync button clicked`);

        pauseResumeSyncBtn.disabled = true;
        pauseResumeSyncBtn.textContent = '处理中...';

        const actionPromise = currentAction === 'pause' ? fileManageApi.pauseSync() : fileManageApi.resumeSync();

        actionPromise
            .then(result => {
                alert(result.message || (currentAction === 'pause' ? '暂停同步请求已发送' : '恢复同步请求已发送'));
                updateSyncStatus();
                if (result.newStatus === 'running') startSyncStatusPolling(); // Ensure polling if resumed
            })
            .catch(error => {
                console.error(`Sync Module: Error ${currentAction} sync:`, error);
                alert(`${currentAction}同步失败: ${error.message}`);
                updateSyncStatus();
            });
    };

    const handleStopSync = () => {
        console.log('Sync Module: Stop Sync button clicked');
        if (!confirm('确定要停止当前的同步任务吗？已处理的文件不会回滚。')) {
            return;
        }
        if(stopSyncBtn) {
            stopSyncBtn.disabled = true;
            stopSyncBtn.textContent = '停止中...';
        }
        if(pauseResumeSyncBtn) pauseResumeSyncBtn.disabled = true; // Disable pause/resume too

        fileManageApi.stopSync()
            .then(result => {
                alert(result.message || '停止同步请求已发送');
                updateSyncStatus(); // Update should show 'stopping' then 'idle'
            })
            .catch(error => {
                console.error('Sync Module: Error stopping sync:', error);
                alert(`停止同步失败: ${error.message}`);
                updateSyncStatus();
            });
    };

    /**
     * 处理分页控件点击事件 (事件委托)。
     * @param {Event} event - 点击事件对象。
     */
    const handlePaginationClick = (event) => {
        const target = event.target;
        // Check if a page link inside the correct pagination container was clicked
        if (target.tagName === 'A' && target.closest('.pagination') === pendingPaginationContainer.querySelector('.pagination')) {
            event.preventDefault();
            const pageNum = target.dataset.page;
            if (pageNum && !target.parentElement.classList.contains('disabled') && !target.parentElement.classList.contains('active')) {
                const page = parseInt(pageNum, 10);
                if (page !== currentPage) { // Load only if page changed
                    loadPendingFiles(page);
                }
            }
        }
        // Add handling for buttons if using button-based pagination
    };

    // --- Initialization ---
    const init = () => {
        console.log('Initializing File Manage Sync Module...');
        if (startSyncBtn) startSyncBtn.addEventListener('click', handleStartSync);
        if (pauseResumeSyncBtn) pauseResumeSyncBtn.addEventListener('click', handlePauseResumeSync);
        if (stopSyncBtn) stopSyncBtn.addEventListener('click', handleStopSync);

        // Add delegated listener for pagination
        if (pendingPaginationContainer) {
            // Ensure listener is added only once by main module or here
            // pendingPaginationContainer.removeEventListener('click', handlePaginationClick);
            // pendingPaginationContainer.addEventListener('click', handlePaginationClick);
        }

        // Initial load and status check
        loadPendingFiles(currentPage);
        startSyncStatusPolling(); // Start polling status

        console.log('File Manage Sync Module Initialized.');
    };

    // --- Public Interface ---
    return {
        init,
        loadPendingFiles, // Expose if needed by main
        updateSyncStatus, // Expose if needed by main
        startSyncStatusPolling,
        stopSyncStatusPolling
    };

})();
