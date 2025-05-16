/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_ui.js
 * 文件名: filemanage_ui.js
 * 开发时间: 2025-04-30 14:45:10 EDT (Update: Handle pending_deletion status and add confirm button)
 * 作者: Gemini
 * 用途: 处理文件管理页面的 DOM 操作和 UI 更新。
 */

const fileManageUI = (() => {

    // --- DOM 元素选择器 ---
    const sidebarLinks = document.querySelectorAll('#filemanage-sidebar .list-group-item');
    const contentSections = document.querySelectorAll('#filemanage-content .content-section');
    const decryptedTableBody = document.getElementById('decrypted-files-table')?.querySelector('tbody');
    const pendingTableBody = document.getElementById('pending-files-table')?.querySelector('tbody');
    // 分页容器现在通过 ID 传递给 updatePagination
    // const decryptedPaginationContainer = document.getElementById('decrypted-pagination');
    // const pendingPaginationContainer = document.getElementById('pending-pagination');
    const pendingCountSpan = document.getElementById('pending-sync-count');
    const syncStatusSpan = document.getElementById('sync-process-status');
    const syncSuccessSpan = document.getElementById('sync-success-count');
    const syncFailSpan = document.getElementById('sync-fail-count');
    const startSyncBtn = document.getElementById('start-sync-btn');
    const pauseResumeSyncBtn = document.getElementById('pause-resume-sync-btn');
    const stopSyncBtn = document.getElementById('stop-sync-btn');

    // --- 常量 ---
    const STATUS_PENDING_DELETION = "pending_deletion"; // 定义待删除状态常量

    // --- 私有辅助函数 ---
    // --- DOM 元素选择器 (补充) ---
    const batchDeleteProgressInfoDiv = document.getElementById('batch-delete-progress-info');
    const batchDeleteTotalSpan = document.getElementById('batch-delete-total');
    const batchDeleteProcessedSpan = document.getElementById('batch-delete-processed');
    const batchDeleteSuccessSpan = document.getElementById('batch-delete-success');
    const batchDeleteFailSpan = document.getElementById('batch-delete-fail');
    const batchDeleteStatusSpan = document.getElementById('batch-delete-status');


    /**
     * 格式化文件大小
     * @param {number} bytes - 文件大小（字节）
     * @returns {string} - 格式化后的字符串 (例如: "1.2 MB", "512 KB")
     */
    const formatFileSize = (bytes) => {
        if (bytes < 0 || bytes === null || bytes === undefined) return 'N/A'; // 更健壮的检查
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        // 处理潜在的 log(0) 或负值
        const i = (bytes > 0) ? Math.floor(Math.log(bytes) / Math.log(k)) : 0;
        // 确保索引在有效范围内
        const index = Math.min(i, sizes.length - 1);
        return parseFloat((bytes / Math.pow(k, index)).toFixed(1)) + ' ' + sizes[index];
    };

    /**
     * 创建表格行 (通用)
     * @param {object} data - 行数据对象
     * @param {string[]} columns - 要显示的 data 中的 key 列表 (按顺序)
     * @param {string} [dataIdKey] - data 中用作 tr data-id 的 key (可选)
     * @param {function} [actionCellRenderer] - 用于渲染操作单元格的函数 (可选)
     * @param {string} [rowClass] - 要添加到 tr 元素的 CSS 类 (可选)
     * @returns {HTMLTableRowElement} - 创建的 tr 元素
     */
    const createTableRow = (data, columns, dataIdKey, actionCellRenderer, rowClass) => {
        const tr = document.createElement('tr');
        if (dataIdKey && data[dataIdKey]) {
            tr.dataset.id = data[dataIdKey]; // 设置 data-id 属性
        }
        if (rowClass) { // 添加行样式类
            tr.classList.add(rowClass);
        }

        // 填充数据单元格
        columns.forEach(key => {
            const td = document.createElement('td');
            let value = data[key] ?? 'N/A'; // 处理 null/undefined
            if (key === 'size') value = formatFileSize(value);
            // 特殊处理状态列，显示中文
            if (key === 'status') value = mapStatusToText(value);

            td.textContent = value;
            td.title = value; // 为可能的文本溢出添加 title 提示
            tr.appendChild(td);
        });

        // 如果提供了操作列渲染器，调用它来创建操作单元格
        if (actionCellRenderer) {
            const actionTd = document.createElement('td');
            actionCellRenderer(actionTd, data); // 调用渲染函数填充单元格
            tr.appendChild(actionTd);
        }
        return tr;
    };

    /**
     * 将后端状态映射为用户友好的中文文本
     * @param {string} status - 后端状态
     * @returns {string} - 显示文本
     */
    const mapStatusToText = (status) => {
        switch (status) {
            case 'pending_sync':
                return '待同步';
            case 'pending_deletion':
                return '待删除'; // 新增状态文本
            case 'synced':
                return '已同步';
            case 'syncing':
                return '同步中';
            case 'error_copying':
                return '复制错误';
            case 'error_syncing':
                return '同步错误';
            default:
                return status || '未知'; // 返回原始状态或未知
        }
    };


    /**
     * 更新分页控件 (调用 pagination.js 中的 AppUtils.setupPagination)
     * @param {string} containerId - 分页控件的容器元素 ID
     * @param {object} pageData - 包含分页信息的对象 (pageNumber, totalPages, totalElements etc.)
     * @param {function} onPageClick - 点击页码时调用的回调函数 (接收页码作为参数)
     */
    const updatePagination = (containerId, pageData, onPageClick) => {
        // 健壮性检查
        if (!containerId || typeof containerId !== 'string') {
            console.error("updatePagination: containerId 必须是一个非空字符串。", containerId);
            return;
        }
        const container = document.getElementById(containerId);
        if (!container) {
            console.error(`未在 DOM 中找到 ID 为 '${containerId}' 的分页容器。`);
            return;
        }
        if (!pageData) {
            console.error(`updatePagination: 容器 '${containerId}' 的 pageData 为 null 或 undefined。`);
            container.innerHTML = ''; // 清空分页
            return;
        }

        // 检查并调用分页库函数
        if (typeof AppUtils !== 'undefined' && typeof AppUtils.setupPagination === 'function') {
            console.debug(`正在为容器 ID '${containerId}' 调用 AppUtils.setupPagination，分页数据:`, pageData);
            try {
                // 调用 pagination.js 提供的函数，传递 options 对象
                AppUtils.setupPagination({
                    containerId: containerId,
                    currentPage: pageData.pageNumber,
                    totalPages: pageData.totalPages,
                    totalRecords: pageData.totalElements,
                    onPageChange: onPageClick // 回调函数
                    // maxPagesToShow: 5 // 可选参数
                });
            } catch (error) {
                console.error(`为容器 '${containerId}' 调用 AppUtils.setupPagination 时出错:`, error);
                container.innerHTML = `<span class="text-danger">分页控件加载失败</span>`; // 在容器内显示错误
            }
        } else {
            console.warn('未找到 AppUtils.setupPagination 函数。跳过分页更新。');
            // 提供基础的回退显示
            container.innerHTML = `第 ${pageData.pageNumber} 页 / 共 ${pageData.totalPages} 页 (总计 ${pageData.totalElements} 条)`;
        }
    };


    // --- 公共 UI 更新函数 ---

    /**
     * 初始化侧边栏导航逻辑。
     */
    const initSidebar = () => {
        sidebarLinks.forEach(link => {
            link.addEventListener('click', (event) => {
                event.preventDefault();
                const targetSectionId = link.dataset.section;
                if (!targetSectionId) return;
                // 切换内容区域的显示
                contentSections.forEach(section => {
                    section.classList.toggle('active-section', section.id === targetSectionId);
                });
                // 更新侧边栏链接的激活状态
                sidebarLinks.forEach(l => l.classList.remove('active'));
                link.classList.add('active');
            });
        });
        // 确保默认激活的侧边栏链接对应的内容区域是可见的
        const activeLink = document.querySelector('#filemanage-sidebar .list-group-item.active');
        if (activeLink) {
            const targetSectionId = activeLink.dataset.section;
            contentSections.forEach(section => {
                section.classList.toggle('active-section', section.id === targetSectionId);
            });
        } else if (sidebarLinks.length > 0) {
            // 如果没有默认激活的，则激活第一个
            sidebarLinks[0].click();
        }
    };

    /**
     * 更新（当前用于显示加密/源文件的）查询结果表格。
     * @param {PageDto<DecryptedFileDto>} pageDto - 后端返回的分页数据
     * @param {function} onPageChange - 分页控件点击时的回调 (接收页码)
     * @param {function} onDownloadClick - 下载按钮点击时的回调 (接收 data)
     */
    const updateDecryptedFilesTable = (pageDto, onPageChange, onDownloadClick) => {
        if (!decryptedTableBody) {
            console.error("文件查询表格主体 (tbody) 未找到。");
            return;
        }
        decryptedTableBody.innerHTML = ''; // 清空旧内容
        const pageData = pageDto || {content: [], pageNumber: 1, totalPages: 0, totalElements: 0}; // 提供默认空数据

        if (pageData.content.length === 0) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = 5; // 匹配表格列数
            td.textContent = '未找到符合条件的文件。';
            td.style.textAlign = 'center';
            td.classList.add('py-4'); // 添加一些垂直内边距
            tr.appendChild(td);
            decryptedTableBody.appendChild(tr);
        } else {
            // 定义要显示的列
            const columns = ['filename', 'relativePath', 'size', 'lastModifiedDateFormatted'];
            pageData.content.forEach(fileData => {
                // 定义下载按钮的渲染函数
                const renderDownloadButton = (cell, data) => {
                    const button = document.createElement('button');
                    button.textContent = '下载';
                    button.classList.add('btn', 'btn-sm', 'btn-success', 'btn-download');
                    // 将下载所需信息存储在 data-* 属性中
                    button.dataset.relativePath = data.relativePath ?? '';
                    button.dataset.filename = data.filename ?? '';
                    // 注意：实际的点击事件监听器应通过事件委托添加到表格主体上
                    cell.appendChild(button);
                };
                // 创建表格行，包含操作列
                const tr = createTableRow(fileData, columns, null, renderDownloadButton, null); // 第三个参数为 null (无 data-id)，最后一个为 null (无特殊行样式)
                decryptedTableBody.appendChild(tr);
            });
        }
        // 更新分页控件
        updatePagination('decrypted-pagination', pageData, onPageChange);
    };


    /**
     * 更新待同步/待删除文件表格。
     * @param {PageDto<PendingFileSyncDto>} pageDto - 后端返回的分页数据 (包含 status)
     * @param {function} onPageChange - 分页控件点击时的回调 (接收页码)
     */
    const updatePendingFilesTable = (pageDto, onPageChange) => {
        if (!pendingTableBody) {
            console.error("待处理文件表格主体 (tbody) 未找到。");
            return;
        }
        pendingTableBody.innerHTML = ''; // 清空现有内容
        const pageData = pageDto || {content: [], pageNumber: 1, totalPages: 0, totalElements: 0}; // 提供默认空数据

        if (pageData.content.length === 0) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            // 调整 colspan 以匹配新的列数 (增加了状态和操作列)
            const numberOfColumns = 6; // 临时文件名, 原始路径, 原始文件名, 大小, 状态, 操作
            td.colSpan = numberOfColumns;
            td.textContent = '没有待处理的文件。';
            td.style.textAlign = 'center';
            td.classList.add('py-4'); // 添加一些垂直内边距
            tr.appendChild(td);
            pendingTableBody.appendChild(tr);
        } else {
            // 定义要显示的列，包含 status，最后一列将是操作列
            const columns = ['tempFilename', 'relativeDirPath', 'originalFilename', 'size', 'status'];
            pageData.content.forEach(fileData => {
                let rowClass = '';
                let actionCellRenderer = null;

                // 检查状态并设置样式和操作按钮
                if (fileData.status === STATUS_PENDING_DELETION) {
                    rowClass = 'pending-deletion-row'; // 添加待删除样式类 (需要在 CSS 中定义)
                    // 定义确认删除按钮的渲染逻辑
                    actionCellRenderer = (cell, data) => {
                        const button = document.createElement('button');
                        button.textContent = '确认删除';
                        // 添加特定类以便事件委托识别
                        button.classList.add('btn', 'btn-sm', 'btn-danger', 'btn-confirm-delete');
                        // 将记录 ID 存储在按钮上，供事件处理器使用
                        button.dataset.id = data.id;
                        cell.appendChild(button);
                    };
                } else {
                    // 对于其他状态 (如 pending_sync)，操作列可以为空或显示其他信息
                    actionCellRenderer = (cell, data) => {
                        // 目前为空
                        // cell.textContent = '-'; // 或显示占位符
                    };
                }

                // 创建表格行，传入样式类和操作渲染器
                const tr = createTableRow(fileData, columns, 'id', actionCellRenderer, rowClass);
                pendingTableBody.appendChild(tr);
            });
        }
        // 更新分页
        updatePagination('pending-pagination', pageData, onPageChange);
    };

    /**
     * 更新同步状态显示区域的文本和按钮状态。
     * @param {FileSyncStatusDto} statusDto - 后端返回的状态数据
     */
    const updateSyncStatusDisplay = (statusDto) => {
        if (!statusDto) return;

        // 安全地更新 DOM 元素内容
        if (pendingCountSpan) pendingCountSpan.textContent = statusDto.totalPendingCount ?? 'N/A';
        if (syncStatusSpan) {
            const currentStatusText = mapStatusToText(statusDto.syncProcessStatus ?? '未知');
            // 仅在文本变化时更新，减少不必要的 DOM 操作
            if (syncStatusSpan.textContent !== currentStatusText) {
                syncStatusSpan.textContent = currentStatusText;
            }
            // 安全地存储上一个状态
            syncStatusSpan.dataset.previousStatus = statusDto.syncProcessStatus || 'idle';
        }
        if (syncSuccessSpan) syncSuccessSpan.textContent = statusDto.processedInCurrentRun ?? '0';
        if (syncFailSpan) syncFailSpan.textContent = statusDto.failedInCurrentRun ?? '0';

        // 更新按钮的启用/禁用状态和文本
        updateSyncButtons(statusDto.syncProcessStatus ?? 'idle');
    };

    /**
     * 根据同步进程状态更新控制按钮的UI。
     * @param {string} status - 当前同步进程状态 ('idle', 'running', 'paused', 'stopping')
     */
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
                pauseResumeSyncBtn.textContent = '恢复'; // 文本改为“恢复”
                stopSyncBtn.disabled = false;
                stopSyncBtn.textContent = '停止';
                break;
            case 'stopping': // 停止中状态
            default: // 包括未知或错误状态
                startSyncBtn.disabled = true;
                startSyncBtn.textContent = '开始同步';
                pauseResumeSyncBtn.disabled = true;
                pauseResumeSyncBtn.textContent = '暂停';
                stopSyncBtn.disabled = true;
                stopSyncBtn.textContent = '停止中...'; // 按钮文本提示正在停止
                break;
        }
    };

    /**
     * 从待同步表格中移除指定 ID 的行（带动画）。
     * @param {number | string} recordId - 要移除的记录 ID (对应 tr 的 data-id)
     */
    const removePendingTableRow = (recordId) => {
        if (!pendingTableBody) return;
        const rowToRemove = pendingTableBody.querySelector(`tr[data-id="${recordId}"]`);
        if (rowToRemove) {
            rowToRemove.classList.add('removing'); // 添加 CSS 类触发动画
            // 监听动画结束事件来移除 DOM 元素
            rowToRemove.addEventListener('transitionend', () => {
                // 确保元素仍然存在于 DOM 中再移除
                if (rowToRemove.parentNode === pendingTableBody) {
                    rowToRemove.remove();
                }
            }, {once: true}); // 监听器只执行一次

            // 设置一个回退的 setTimeout，以防 transitionend 事件因某些原因未触发
            setTimeout(() => {
                if (rowToRemove.parentNode === pendingTableBody) { // 再次检查
                    console.warn(`移除行 (ID: ${recordId}) 时 transitionend 未触发，使用 setTimeout 回退。`);
                    rowToRemove.remove();
                }
            }, 600); // 时间应略长于 CSS 动画的持续时间
        } else {
            console.warn(`未找到 data-id 为 ${recordId} 的行进行移除。`);
        }
    };

    /**
     * 更新批量删除操作的进度显示。
     * @param {object} progress - 包含 total, processed, success, fail, statusText 的对象。
     * @param {boolean} [show=true] - 是否显示进度区域。
     */
    const updateBatchDeleteProgressDisplay = (progress, show = true) => {
        if (batchDeleteProgressInfoDiv) {
            batchDeleteProgressInfoDiv.style.display = show ? 'block' : 'none';
            if (show && progress) {
                if (batchDeleteTotalSpan) batchDeleteTotalSpan.textContent = progress.total ?? '0';
                if (batchDeleteProcessedSpan) batchDeleteProcessedSpan.textContent = progress.processed ?? '0';
                if (batchDeleteSuccessSpan) batchDeleteSuccessSpan.textContent = progress.success ?? '0';
                if (batchDeleteFailSpan) batchDeleteFailSpan.textContent = progress.fail ?? '0';
                if (batchDeleteStatusSpan) batchDeleteStatusSpan.textContent = progress.statusText ?? '未开始';
            }
        }
    };


    // --- 公共接口 ---
    // 暴露需要被其他模块调用的函数
    return {
        initSidebar,
        updateDecryptedFilesTable,
        updatePendingFilesTable, // 更新后的函数，支持状态和删除按钮
        updateSyncStatusDisplay,
        removePendingTableRow, // 暴露移除行的函数
        updateSyncButtons, // 可能需要暴露以供外部调用（例如出错时）
        updateBatchDeleteProgressDisplay,  //批量执行页面待删除文件的删除操作

        // 确保 mapStatusToText 和其他需要的辅助函数也在作用域内或已暴露
        // 如果需要通用确认模态框，确保它已存在或在此处添加
        // 例如:
        showGeneralConfirmationModal: (title, message, onConfirm) => {
            // 假设你有一个通用的模态框HTML结构，并用JS控制它
            // 这里用 window.confirm 作为简单替代
            if (window.confirm(`<span class="math-inline">\{title\}\\n\\n</span>{message}`)) {
                onConfirm();
            }
        }
    };

})();
