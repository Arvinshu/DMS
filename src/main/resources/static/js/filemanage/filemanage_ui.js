/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_ui.js
 * 文件名: filemanage_ui.js
 * 开发时间: 2025-04-30 13:30:10 EDT (Update: Fixed arguments for setupPagination call)
 * 作者: Gemini
 * 用途: 处理文件管理页面的 DOM 操作和 UI 更新。
 */

const fileManageUI = (() => {

    // --- DOM Element Selectors ---
    const sidebarLinks = document.querySelectorAll('#filemanage-sidebar .list-group-item');
    const contentSections = document.querySelectorAll('#filemanage-content .content-section');
    // const decryptedTableBody = document.getElementById('decrypted-files-table')?.querySelector('tbody');
    const encryptedTableBody = document.getElementById('encrypted-files-table')?.querySelector('tbody');
    const pendingTableBody = document.getElementById('pending-files-table')?.querySelector('tbody');
    const pendingCountSpan = document.getElementById('pending-sync-count');
    const syncStatusSpan = document.getElementById('sync-process-status');
    const syncSuccessSpan = document.getElementById('sync-success-count');
    const syncFailSpan = document.getElementById('sync-fail-count');
    const startSyncBtn = document.getElementById('start-sync-btn');
    const pauseResumeSyncBtn = document.getElementById('pause-resume-sync-btn');
    const stopSyncBtn = document.getElementById('stop-sync-btn');

    // --- Private Helper Functions ---

    const formatFileSize = (bytes) => {
        if (bytes < 0 || bytes === null || bytes === undefined) return 'N/A';
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = (bytes > 0) ? Math.floor(Math.log(bytes) / Math.log(k)) : 0;
        const index = Math.min(i, sizes.length - 1);
        return parseFloat((bytes / Math.pow(k, index)).toFixed(1)) + ' ' + sizes[index];
    };

    const createTableRow = (data, columns, dataIdKey, actionCellRenderer) => {
        const tr = document.createElement('tr');
        if (dataIdKey && data[dataIdKey]) {
            tr.dataset.id = data[dataIdKey];
        }
        columns.forEach(key => {
            const td = document.createElement('td');
            let value = data[key] ?? 'N/A';
            if (key === 'size') value = formatFileSize(value);
            td.textContent = value;
            td.title = value; // Add title for tooltip on hover
            tr.appendChild(td);
        });
        if (actionCellRenderer) {
            const actionTd = document.createElement('td');
            actionCellRenderer(actionTd, data);
            tr.appendChild(actionTd);
        }
        return tr;
    };

    /**
     * 更新分页控件 (调用 pagination.js 中的 AppUtils.setupPagination)
     * @param {string} containerId - 分页控件的容器元素 ID
     * @param {object} pageData - 包含分页信息的对象 (pageNumber, totalPages, totalElements etc.)
     * @param {function} onPageClick - 点击页码时调用的回调函数 (接收页码作为参数)
     */
    const updatePagination = (containerId, pageData, onPageClick) => {
        if (!containerId || typeof containerId !== 'string') {
            console.error("updatePagination: containerId must be a non-empty string.", containerId);
            return;
        }
        const container = document.getElementById(containerId);
        if (!container) {
            console.error(`Pagination container with ID '${containerId}' not found in the DOM.`);
            return;
        }
        if (!pageData) {
            console.error(`updatePagination: pageData is null or undefined for container '${containerId}'.`);
            // Render empty pagination or hide it
            container.innerHTML = ''; // Clear previous pagination
            return;
        }

        if (typeof AppUtils !== 'undefined' && typeof AppUtils.setupPagination === 'function') {
            console.debug(`Calling AppUtils.setupPagination for containerId: '${containerId}' with pageData:`, pageData);
            try {
                // --- CORRECTED CALL: Pass an options object ---
                AppUtils.setupPagination({
                    containerId: containerId,
                    currentPage: pageData.pageNumber, // Match expected property name 'currentPage'
                    totalPages: pageData.totalPages,
                    totalRecords: pageData.totalElements, // Match expected property name 'totalRecords'
                    onPageChange: onPageClick // Match expected property name 'onPageChange'
                    // maxPagesToShow: 5 // Add if you want to configure this
                });
            } catch (error) {
                console.error(`Error calling AppUtils.setupPagination for container '${containerId}':`, error);
                container.innerHTML = `<span class="text-danger">分页控件加载失败</span>`;
            }
        } else {
            console.warn('AppUtils.setupPagination function not found. Skipping pagination update.');
            container.innerHTML = `Page ${pageData.pageNumber} of ${pageData.totalPages} (Total: ${pageData.totalElements})`;
        }
    };


    // --- Public UI Functions ---

    const initSidebar = () => {
        sidebarLinks.forEach(link => {
            link.addEventListener('click', (event) => {
                event.preventDefault();
                const targetSectionId = link.dataset.section;
                if (!targetSectionId) return;
                contentSections.forEach(section => {
                    section.classList.toggle('active-section', section.id === targetSectionId);
                });
                sidebarLinks.forEach(l => l.classList.remove('active'));
                link.classList.add('active');
            });
        });
        const activeLink = document.querySelector('#filemanage-sidebar .list-group-item.active');
        if (activeLink) {
            const targetSectionId = activeLink.dataset.section;
            contentSections.forEach(section => {
                section.classList.toggle('active-section', section.id === targetSectionId);
            });
        }
    };

    const updateFilesTable = (pageDto, onPageChange, onDownloadClick) => {
        if (!encryptedTableBody) {
            console.error("Encrypted files table body not found.");
            return;
        }
        encryptedTableBody.innerHTML = '';
        const pageData = pageDto || {content: [], pageNumber: 1, totalPages: 0, totalElements: 0};

        if (pageData.content.length === 0) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = 5;
            td.textContent = '未找到符合条件的加密文件。';
            td.style.textAlign = 'center';
            tr.appendChild(td);
            encryptedTableBody.appendChild(tr);
        } else {
            const columns = ['filename', 'relativePath', 'size', 'lastModifiedDateFormatted'];
            pageData.content.forEach(fileData => {
                const renderDownloadButton = (cell, data) => {
                    const button = document.createElement('button');
                    button.textContent = '下载';
                    button.classList.add('btn', 'btn-sm', 'btn-success', 'btn-download');
                    button.dataset.relativePath = data.relativePath ?? '';
                    button.dataset.filename = data.filename ?? '';
                    cell.appendChild(button);
                };
                const tr = createTableRow(fileData, columns, null, renderDownloadButton);
                encryptedTableBody.appendChild(tr);
            });
        }
        updatePagination('encrypted-pagination', pageData, onPageChange);
    };


    const updatePendingFilesTable = (pageDto, onPageChange) => {
        if (!pendingTableBody) {
            console.error("Pending files table body not found.");
            return;
        }
        pendingTableBody.innerHTML = '';
        const pageData = pageDto || {content: [], pageNumber: 1, totalPages: 0, totalElements: 0};

        if (pageData.content.length === 0) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = 5;
            td.textContent = '没有待同步的文件。';
            td.style.textAlign = 'center';
            tr.appendChild(td);
            pendingTableBody.appendChild(tr);
        } else {
            const columns = ['tempFilename', 'relativeDirPath', 'originalFilename', 'size', 'lastModifiedDateFormatted'];
            pageData.content.forEach(fileData => {
                const tr = createTableRow(fileData, columns, 'id');
                pendingTableBody.appendChild(tr);
            });
        }
        updatePagination('pending-pagination', pageData, onPageChange);
    };

    const updateSyncStatusDisplay = (statusDto) => {
        if (!statusDto) return;

        if (pendingCountSpan) pendingCountSpan.textContent = statusDto.totalPendingCount ?? 'N/A';
        if (syncStatusSpan) {
            const currentStatusText = mapSyncStatusToText(statusDto.syncProcessStatus ?? '未知');
            if (syncStatusSpan.textContent !== currentStatusText) {
                syncStatusSpan.textContent = currentStatusText;
            }
            syncStatusSpan.dataset.previousStatus = statusDto.syncProcessStatus || 'idle';
        }
        if (syncSuccessSpan) syncSuccessSpan.textContent = statusDto.processedInCurrentRun ?? '0';
        if (syncFailSpan) syncFailSpan.textContent = statusDto.failedInCurrentRun ?? '0';

        updateSyncButtons(statusDto.syncProcessStatus ?? 'idle');
    };

    const mapSyncStatusToText = (status) => {
        switch (status) {
            case 'idle':
                return '空闲';
            case 'running':
                return '运行中';
            case 'paused':
                return '已暂停';
            case 'stopping':
                return '停止中';
            default:
                return '未知';
        }
    };

    const updateSyncButtons = (status) => {
        if (!startSyncBtn || !pauseResumeSyncBtn || !stopSyncBtn) return;
        switch (status) {
            case 'idle':
                startSyncBtn.disabled = false;
                startSyncBtn.textContent = '开始同步';
                pauseResumeSyncBtn.disabled = true;
                pauseResumeSyncBtn.textContent = '暂停';
                stopSyncBtn.disabled = true;
                stopSyncBtn.textContent = '停止';
                break;
            case 'running':
                startSyncBtn.disabled = true;
                startSyncBtn.textContent = '开始同步';
                pauseResumeSyncBtn.disabled = false;
                pauseResumeSyncBtn.textContent = '暂停';
                stopSyncBtn.disabled = false;
                stopSyncBtn.textContent = '停止';
                break;
            case 'paused':
                startSyncBtn.disabled = true;
                startSyncBtn.textContent = '开始同步';
                pauseResumeSyncBtn.disabled = false;
                pauseResumeSyncBtn.textContent = '恢复';
                stopSyncBtn.disabled = false;
                stopSyncBtn.textContent = '停止';
                break;
            case 'stopping':
            default:
                startSyncBtn.disabled = true;
                startSyncBtn.textContent = '开始同步';
                pauseResumeSyncBtn.disabled = true;
                pauseResumeSyncBtn.textContent = '暂停';
                stopSyncBtn.disabled = true;
                stopSyncBtn.textContent = '停止中...';
                break;
        }
    };

    const removePendingTableRow = (recordId) => {
        if (!pendingTableBody) return;
        const rowToRemove = pendingTableBody.querySelector(`tr[data-id="${recordId}"]`);
        if (rowToRemove) {
            rowToRemove.classList.add('removing');
            rowToRemove.addEventListener('transitionend', () => {
                rowToRemove.remove();
            }, {once: true});
            setTimeout(() => {
                if (rowToRemove.parentNode) rowToRemove.remove();
            }, 600);
        } else {
            console.warn(`Row with data-id ${recordId} not found for removal.`);
        }
    };

    return {
        initSidebar,
        updateFilesTable,
        updatePendingFilesTable,
        updateSyncStatusDisplay,
        removePendingTableRow,
        updateSyncButtons
    };

})();
