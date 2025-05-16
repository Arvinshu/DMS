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
    const pageSize = 100; // Or get from config/UI
    let syncStatusIntervalId = null;
    const SYNC_STATUS_POLL_INTERVAL = 5000; // Poll every 5 seconds

    // --- Core Functions ---

    /**
     * 加载并显示待同步文件列表的指定页面。
     * @param {number} page - 要加载的页码 (从 1 开始)。
     */
    const loadPendingFiles = (page) => {
        currentPage = page;
        console.log(`Sync Module: Loading pending/deletion files: page=${page}`);
        // Add loading indicator
        if (pendingTableBody) pendingTableBody.innerHTML = '<tr><td colspan="6" class="text-center py-4">加载中...</td></tr>'; // Colspan updated to 6

        fileManageApi.getPendingFiles(page, pageSize)
            .then(pageDto => {
                // Pass self for pagination callback
                fileManageUI.updatePendingFilesTable(pageDto, loadPendingFiles);
            })
            .catch(error => {
                console.error('Sync Module: Error loading pending files:', error);
                alert(`加载待处理文件列表失败: ${error.message}`);
                if (pendingTableBody) pendingTableBody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-danger">加载失败</td></tr>'; // Colspan updated
                fileManageUI.updatePendingFilesTable(null, loadPendingFiles); // Clear pagination
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
        const paginationNav = target.closest('.pagination');
        if (paginationNav && pendingPaginationContainer && pendingPaginationContainer.contains(paginationNav)) {
            if (target.tagName === 'A' || target.tagName === 'BUTTON') {
                event.preventDefault();
                const pageNum = target.dataset.page;
                if (pageNum && !target.disabled && !target.closest('li')?.classList.contains('disabled') && !target.closest('li')?.classList.contains('active')) {
                    const page = parseInt(pageNum, 10);
                    if (page !== currentPage) {
                        loadPendingFiles(page);
                    }
                }
            }
        }
    };

    /**
     * 处理确认删除按钮点击事件 (事件委托)。
     * @param {Event} event - 点击事件对象。
     */
    const handleConfirmDeleteDelegation = (event) => {
        // 检查点击的是否是确认删除按钮
        if (event.target && event.target.classList.contains('btn-confirm-delete')) {
            const button = event.target;
            const recordId = button.dataset.id; // 从按钮获取记录 ID

            if (recordId) {
                // 弹出确认对话框
                if (window.confirm(`您确定要永久删除这个文件记录及其对应的解密文件吗？此操作不可恢复。`)) {
                    console.log(`Sync Module: User confirmed deletion for record ID: ${recordId}`);
                    // 禁用按钮防止重复点击
                    button.disabled = true;
                    button.textContent = '删除中...';

                    // 调用 API 发送删除请求 (注意：API 期望一个数组)
                    fileManageApi.confirmDelete([parseInt(recordId, 10)]) // 将 ID 转为数字并放入数组
                        .then(response => {
                            console.log(`Sync Module: Deletion API response for ID ${recordId}:`, response);
                            alert(response.message || `记录 ID ${recordId} 删除成功。`);
                            // 从 UI 移除该行
                            fileManageUI.removePendingTableRow(recordId);
                            // 可选：立即更新状态计数，或等待下一次轮询
                            updateSyncStatus();
                        })
                        .catch(error => {
                            console.error(`Sync Module: Error confirming delete for record ID ${recordId}:`, error);
                            alert(`删除记录 ID ${recordId} 失败: ${error.message}`);
                            // 重新启用按钮
                            button.disabled = false;
                            button.textContent = '确认删除';
                        });
                } else {
                    console.log(`Sync Module: User cancelled deletion for record ID: ${recordId}`);
                }
            } else {
                console.error("无法获取要删除的记录 ID。");
            }
        }
    };

    const batchClearPendingBtn = document.getElementById('batch-clear-pending-btn');

    /**
     * 处理批量清理当前页面可见的待删除文件。
     */
    const handleBatchClearVisiblePending = async () => { // 使用 async 以便内部使用 await
        if (!pendingTableBody) {
            console.error("待处理文件表格主体 (tbody) 未找到。");
            fileManageUI.updateBatchDeleteProgressDisplay({ statusText: "错误：表格未找到" }, true);
            return;
        }

        // 1. 收集当前表格中所有带有 'btn-confirm-delete' 按钮的行（这些是UI上标记为待删除的）
        const rowsToDelete = pendingTableBody.querySelectorAll('tr td button.btn-confirm-delete');
        const recordIdsToDelete = [];
        rowsToDelete.forEach(button => {
            const row = button.closest('tr');
            if (row && row.dataset.id) {
                // 确保按钮仍然可见且行未被标记为正在移除 (以防用户快速连点)
                if (!row.classList.contains('removing') && button.offsetParent !== null) {
                    recordIdsToDelete.push(row.dataset.id);
                }
            }
        });

        if (recordIdsToDelete.length === 0) {
            alert('当前页面没有待删除的文件可供批量处理。');
            fileManageUI.updateBatchDeleteProgressDisplay({ statusText: "无待处理文件" }, false); // 可以选择隐藏或显示无文件
            return;
        }

        // 2. 用户确认
        fileManageUI.showGeneralConfirmationModal(
            '确认批量删除',
            `您确定要删除当前列表中的 ${recordIdsToDelete.length} 个待删除文件吗？此操作将逐个执行，且不可恢复。`,
            async () => { // 确认后的回调也设为 async
                if (batchClearPendingBtn) batchClearPendingBtn.disabled = true;
                // 禁用其他可能冲突的同步按钮
                if (startSyncBtn) startSyncBtn.disabled = true;
                if (pauseResumeSyncBtn) pauseResumeSyncBtn.disabled = true;
                if (stopSyncBtn) stopSyncBtn.disabled = true;


                let totalToDelete = recordIdsToDelete.length;
                let processedCount = 0;
                let successCount = 0;
                let failCount = 0;

                fileManageUI.updateBatchDeleteProgressDisplay({
                    total: totalToDelete,
                    processed: processedCount,
                    success: successCount,
                    fail: failCount,
                    statusText: '开始处理...'
                }, true);

                // 3. 循环执行删除
                for (const recordId of recordIdsToDelete) {
                    processedCount++;
                    fileManageUI.updateBatchDeleteProgressDisplay({
                        total: totalToDelete,
                        processed: processedCount,
                        success: successCount,
                        fail: failCount,
                        statusText: `正在删除 ID: <span class="math-inline">\{recordId\} \(</span>{processedCount}/${totalToDelete})`
                    }, true);

                    try {
                        // 调用已有的单个删除API，API期望一个数组
                        const response = await fileManageApi.confirmDelete([parseInt(recordId, 10)]);
                        console.log(`Sync Module: Batch Deletion API response for ID ${recordId}:`, response);
                        // 假设API成功响应包含 success:true 或类似，或者不抛出错误即为成功
                        // filemanage_api.js中的handleResponse会在非ok时reject
                        // 如果API返回的response.message指示成功，也可以用它
                        if (response && (response.success || response.message?.includes("成功"))) { // 根据实际API响应调整判断条件
                            successCount++;
                            fileManageUI.removePendingTableRow(recordId); // 从UI移除行
                        } else {
                            // 即使API调用本身是200 OK，但业务逻辑上可能失败
                            failCount++;
                            console.warn(`Sync Module: Deletion for ID ${recordId} reported as failed by API or response unclear:`, response);
                            // 可以在这里给对应的行添加一个失败标记，而不是移除
                            const failedRow = pendingTableBody.querySelector(`tr[data-id="${recordId}"]`);
                            if (failedRow) {
                                const statusCell = failedRow.querySelector('td:nth-child(5)'); // 假设状态在第5列
                                if (statusCell) statusCell.innerHTML = '<span class="text-danger">删除失败</span>';
                                const actionCell = failedRow.querySelector('td:last-child button.btn-confirm-delete');
                                if(actionCell) actionCell.disabled = true; // 禁用该失败项的删除按钮
                            }
                        }
                    } catch (error) {
                        failCount++;
                        console.error(`Sync Module: Error batch deleting record ID ${recordId}:`, error);
                        // 标记UI中的对应行为删除失败
                        const failedRow = pendingTableBody.querySelector(`tr[data-id="${recordId}"]`);
                        if (failedRow) {
                            const statusCell = failedRow.querySelector('td:nth-child(5)');
                            if (statusCell) statusCell.innerHTML = '<span class="text-danger">删除出错</span>';
                            const actionCell = failedRow.querySelector('td:last-child button.btn-confirm-delete');
                            if(actionCell) actionCell.disabled = true;
                        }
                    }

                    fileManageUI.updateBatchDeleteProgressDisplay({
                        total: totalToDelete,
                        processed: processedCount,
                        success: successCount,
                        fail: failCount,
                        statusText: `正在处理... (<span class="math-inline">\{processedCount\}/</span>{totalToDelete})`
                    }, true);
                } // 结束循环

                // 4. 完成后更新状态
                const finalStatusText = `批量删除完成。成功: ${successCount}, 失败: ${failCount}。`;
                fileManageUI.updateBatchDeleteProgressDisplay({
                    total: totalToDelete,
                    processed: processedCount,
                    success: successCount,
                    fail: failCount,
                    statusText: finalStatusText
                }, true);
                alert(finalStatusText);

                if (batchClearPendingBtn) batchClearPendingBtn.disabled = false;
                // 根据主同步状态重新启用其他同步按钮
                updateSyncStatus(); // 调用此函数会根据当前主同步状态正确设置按钮
            }
        );
    };


    // --- Initialization ---
    const init = () => {
        console.log('Initializing File Manage Sync Module...');
        if (startSyncBtn) startSyncBtn.addEventListener('click', handleStartSync);
        if (pauseResumeSyncBtn) pauseResumeSyncBtn.addEventListener('click', handlePauseResumeSync);
        if (stopSyncBtn) stopSyncBtn.addEventListener('click', handleStopSync);

        // 绑定分页事件委托
        if (pendingPaginationContainer) {
            pendingPaginationContainer.removeEventListener('click', handlePaginationClick);
            pendingPaginationContainer.addEventListener('click', handlePaginationClick);
        }

        // 绑定确认删除按钮的事件委托到表格主体
        if (pendingTableBody) {
            pendingTableBody.removeEventListener('click', handleConfirmDeleteDelegation); // 移除旧的（如果有）
            pendingTableBody.addEventListener('click', handleConfirmDeleteDelegation); // 添加新的
        }

        // 绑定批量删除按钮监听
        if (batchClearPendingBtn) {
            batchClearPendingBtn.addEventListener('click', handleBatchClearVisiblePending);
        }

        // 初始加载和状态检查
        loadPendingFiles(currentPage);
        startSyncStatusPolling();

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
