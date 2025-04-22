/**
 * 数据同步功能区脚本
 * 文件路径: src/main/resources/static/js/sync_data.js
 * 职责：获取初始数量统计，填充页面，并处理“开始同步”按钮的点击事件（调用后端 API）。
 * 依赖: common.js
 */

(function(window) {
    'use strict';

    // 创建或获取模块命名空间
    const SyncDataModule = window.SyncDataModule || {};
    window.SyncDataModule = SyncDataModule;

    // --- 配置和常量 ---
    const API_COUNTS_ENDPOINT = '/api/sync/counts';
    const API_SYNC_BASE_URL = '/api/sync'; // 同步 API 的基础路径

    // --- DOM 元素 ID 映射 ---
    const elementIds = {
        // 部门
        sourceDeptCount: 'sync-source-dept-count',
        targetDeptCount: 'sync-target-dept-count',
        syncDeptButton: 'sync-dept-button',
        syncDeptProgress: 'sync-dept-progress',
        syncDeptResult: 'sync-dept-result',
        // 员工
        sourceEmpCount: 'sync-source-emp-count',
        targetEmpCount: 'sync-target-emp-count',
        syncEmpButton: 'sync-emp-button',
        syncEmpProgress: 'sync-emp-progress',
        syncEmpResult: 'sync-emp-result',
        // 工时编码
        sourceTscCount: 'sync-source-tsc-count',
        targetTscCount: 'sync-target-tsc-count',
        syncTscButton: 'sync-tsc-button',
        syncTscProgress: 'sync-tsc-progress',
        syncTscResult: 'sync-tsc-result',
        // 利润中心
        sourcePcCount: 'sync-source-pc-count',
        targetPcCount: 'sync-target-pc-count',
        syncPcButton: 'sync-pc-button',
        syncPcProgress: 'sync-pc-progress',
        syncPcResult: 'sync-pc-result'
    };

    // --- DOM 元素引用 ---
    let elements = {}; // 存储获取到的元素

    // --- 状态 ---
    let isSyncing = false; // 防止多个同步同时进行

    // --- 函数定义 ---

    /**
     * 获取初始数量统计并更新页面显示
     */
    async function loadInitialCounts() {
        console.log("[SyncData] Loading initial counts...");
        // 显示加载状态
        Object.keys(elementIds).forEach(key => {
            const el = elements[key];
            if (key.includes('Count') && el && el.tagName === 'STRONG') {
                el.textContent = '加载中...';
                el.classList.remove('text-red-500'); // 清除可能存在的错误样式
            }
            if (key.includes('Result') && el) { // 清除旧的结果信息
                el.textContent = '';
                el.classList.remove('text-green-600', 'text-red-600');
            }
            if (key.includes('Progress') && el) { // 隐藏进度条
                el.classList.add('hidden');
            }
            if (key.includes('Button') && el) { // 启用按钮
                el.disabled = false;
                // 恢复按钮原始文本 (如果之前修改过)
                const moduleKey = key.replace('sync','').replace('Button','').toLowerCase();
                el.innerHTML = `开始同步${getModuleName(moduleKey)}`; // 使用 innerHTML 以防之前有图标
            }
        });

        try {
            const counts = await AppUtils.get(API_COUNTS_ENDPOINT);
            console.log("[SyncData] Counts received:", counts);

            // 更新页面元素文本，检查 counts 对象是否存在对应的属性
            if (elements.sourceDeptCount) elements.sourceDeptCount.textContent = counts.sourceDepartmentCount ?? 'N/A';
            if (elements.targetDeptCount) elements.targetDeptCount.textContent = counts.targetDepartmentCount ?? 'N/A';
            if (elements.sourceEmpCount) elements.sourceEmpCount.textContent = counts.sourceEmployeeCount ?? 'N/A';
            if (elements.targetEmpCount) elements.targetEmpCount.textContent = counts.targetEmployeeCount ?? 'N/A';
            if (elements.sourceTscCount) elements.sourceTscCount.textContent = counts.sourceTimesheetCodeCount ?? 'N/A';
            if (elements.targetTscCount) elements.targetTscCount.textContent = counts.targetTimesheetCodeCount ?? 'N/A';
            if (elements.sourcePcCount) elements.sourcePcCount.textContent = counts.sourceProfitCenterCount ?? 'N/A';
            if (elements.targetPcCount) elements.targetPcCount.textContent = counts.targetProfitCenterCount ?? 'N/A';

            // AppUtils.showMessage("统计信息加载完成", "success"); // 加载完成通常不需要提示

        } catch (error) {
            console.error("[SyncData] 获取统计数量失败:", error);
            AppUtils.showMessage(`获取统计数量失败: ${error.message || '请稍后重试'}`, 'error');
            // 在页面上显示错误状态
            Object.keys(elementIds).forEach(key => {
                const el = elements[key];
                if (key.includes('Count') && el && el.tagName === 'STRONG') {
                    el.textContent = '加载失败';
                    el.classList.add('text-red-500');
                }
            });
        }
    }

    /**
     * 处理同步按钮点击事件
     * @param {string} moduleKey - 同步模块的键名 (如 'dept', 'emp', 'tsc', 'pc')
     * @param {string} apiPath - 对应的后端 API 路径 (如 '/departments')
     */
    async function handleSyncClick(moduleKey, apiPath) {
        if (isSyncing) {
            AppUtils.showMessage("已有同步任务正在进行中，请稍候...", "warning");
            return;
        }

        // 构造元素 key
        const buttonKey = `sync${capitalizeFirstLetter(moduleKey)}Button`;
        const progressKey = `sync${capitalizeFirstLetter(moduleKey)}Progress`;
        const resultKey = `sync${capitalizeFirstLetter(moduleKey)}Result`;

        const button = elements[buttonKey];
        const progress = elements[progressKey];
        const resultEl = elements[resultKey];

        if (!button || !progress || !resultEl) {
            console.error(`[SyncData] Cannot find elements for module: ${moduleKey}`);
            AppUtils.showMessage(`无法启动同步：界面元素丢失 (${moduleKey})`, 'error');
            return;
        }

        console.log(`[SyncData] Starting sync for: ${moduleKey}`);
        isSyncing = true;
        button.disabled = true;
        // 使用 innerHTML 添加加载动画和文本
        button.innerHTML = `<div class="loader inline-block" style="width:16px; height:16px; border-width:2px; margin: 0 5px 0 0; vertical-align: middle;"></div> 处理中...`;
        progress.classList.remove('hidden'); // 显示进度条
        resultEl.textContent = '正在同步，请稍候...'; // 设置初始提示
        resultEl.classList.remove('text-green-600', 'text-red-600'); // 清除旧样式

        try {
            // 调用后端同步 API
            const result = await AppUtils.post(`${API_SYNC_BASE_URL}${apiPath}`, {});
            console.log(`[SyncData] Sync result for ${moduleKey}:`, result);

            // 显示结果消息
            resultEl.textContent = result.message || '同步处理完成，但未收到明确消息。';
            // 根据成功或失败数量设置文本颜色
            if (result.failureCount > 0) {
                resultEl.classList.add('text-red-600');
                AppUtils.showMessage(`${getModuleName(moduleKey)} 同步完成，但有失败记录，详情请查看日志`, 'warning');
            } else if (result.successCount >= 0) { // 即使新增0条也算成功完成
                resultEl.classList.add('text-green-600');
                AppUtils.showMessage(`${getModuleName(moduleKey)} 同步成功完成`, 'success');
            }

            // 同步完成后立即刷新统计数量
            await loadInitialCounts();

        } catch (error) {
            console.error(`[SyncData] 同步 ${moduleKey} 失败:`, error);
            const errorMsg = `同步失败: ${error.message || '未知错误，请检查后端日志'}`;
            resultEl.textContent = errorMsg;
            resultEl.classList.add('text-red-600');
            AppUtils.showMessage(errorMsg, 'error');
            // 即使失败，也尝试刷新一次数量，看看目标表是否有变化
            await loadInitialCounts();
        } finally {
            isSyncing = false;
            button.disabled = false;
            button.innerHTML = `开始同步${getModuleName(moduleKey)}`; // 恢复按钮文本
            progress.classList.add('hidden'); // 隐藏进度条
            // 可以在几秒后清除结果文本
            setTimeout(() => {
                if (resultEl && !resultEl.classList.contains('text-red-600')) { // 只清除成功的消息
                    resultEl.textContent = '';
                    resultEl.classList.remove('text-green-600');
                }
            }, 10000); // 10秒后清除成功消息
        }
    }

    // 辅助函数：首字母大写
    function capitalizeFirstLetter(string) {
        if (!string) return '';
        return string.charAt(0).toUpperCase() + string.slice(1);
    }
    // 辅助函数：获取模块中文名
    function getModuleName(key) {
        switch(key) {
            case 'dept': return '部门';
            case 'emp': return '员工';
            case 'tsc': return '工时编码';
            case 'pc': return '利润中心';
            default: return '';
        }
    }

    /** 设置同步按钮的事件监听器 */
    function setupSyncButtonListeners() {
        console.debug("[SyncData] Setting up sync button listeners.");
        // 定义模块键名和对应的 API 路径
        const syncMappings = [
            { key: 'dept', path: '/departments' },
            { key: 'emp', path: '/employees' },
            { key: 'tsc', path: '/timesheet-codes' },
            { key: 'pc', path: '/profit-centers' }
        ];

        syncMappings.forEach(mapping => {
            // 构造按钮的 ID
            const buttonId = `sync${capitalizeFirstLetter(mapping.key)}Button`;
            const button = elements[buttonId]; // 从已获取的元素中查找

            if (button) {
                button.addEventListener('click', () => handleSyncClick(mapping.key, mapping.path));
                console.log(`[SyncData] Listener attached for ${mapping.key} sync button.`);
            } else {
                console.warn(`[SyncData] Sync button with ID "${buttonId}" not found for module: ${mapping.key}`);
            }
        });
    }


    /** 初始化模块 */
    SyncDataModule.init = function() {
        console.log("Initializing Sync Data Module...");
        // 获取所有需要的 DOM 元素引用
        let allElementsFound = true;
        for (const key in elementIds) {
            elements[key] = document.getElementById(elementIds[key]);
            if (!elements[key]) {
                console.error(`[SyncData] Initialization failed: Element with ID "${elementIds[key]}" not found.`);
                allElementsFound = false;
            }
        }

        if (!allElementsFound) {
            const section = document.getElementById('sync-data-section');
            if(section) section.innerHTML = '<p class="text-red-500 p-4">数据同步模块加载失败，部分界面元素缺失，请联系管理员。</p>';
            return; // 如果关键元素缺失，则不继续初始化
        }

        console.log("[SyncData] All required elements found.");
        loadInitialCounts(); // 加载初始数量
        setupSyncButtonListeners(); // 设置按钮监听
        console.log("Sync Data Module Initialized.");
    };

    // 注意：init 函数由 datamaintenance_main.js 在显示此 section 时调用

})(window);

//
// **修改说明:**
//
// 1.  **`loadInitialCounts` 函数:**
//     * 在开始加载前，会清除所有结果区域的旧消息和样式，隐藏进度条，并启用按钮。
//     * 在获取到数据后，使用 `?? 'N/A'` 或 `?? '错误'` 来处理后端可能返回 `null` 或 `undefined` 的情况。
//     * 移除了加载完成时的成功提示，因为这通常不是必要的用户反馈。
//     * 在捕获到错误时，会给计数区域添加红色文本样式。
// 2.  **`handleSyncClick` 函数:**
//     * **状态管理:** 正确检查和设置 `isSyncing` 标志，禁用/启用按钮。
//     * **UI 反馈:**
//         * 点击后，按钮文本变为包含加载动画的“处理中...”。
//         * 显示对应的进度条（通过移除 `hidden` 类）。
//         * 在结果区域显示“正在同步...”的初始提示。
//     * **API 调用:** 使用 `AppUtils.post` 调用正确的后端 API 端点。
//     * **结果处理:**
//         * 从后端返回的 `SyncResult` 对象中获取消息并显示。
//         * 根据 `failureCount` 或 `successCount` 设置结果文本的颜色（成功为绿色，失败为红色）。
//         * **调用 `loadInitialCounts()` 刷新页面上的统计数量。**
//     * **错误处理:** 在 `catch` 块中显示错误消息。
//     * **`finally` 块:** 确保 `isSyncing` 状态被重置，按钮恢复原始文本并重新启用，进度条被隐藏。添加了一个 `setTimeout` 在 10 秒后清除成功的消息（失败消息保留）。
// 3.  **`setupSyncButtonListeners` 函数:**
//     * 定义了一个 `syncMappings` 数组来存储模块键和对应的 API 路径。
//     * 循环遍历 `syncMappings`，为每个模块查找对应的按钮元素（从 `elements` 缓存中）并添加 `click` 事件监听器，监听器调用 `handleSyncClick` 并传入正确的模块键和 API 路径。
// 4.  **`init` 函数:**
//     * 确保在调用 `loadInitialCounts` 和 `setupSyncButtonListeners` 之前，所有需要的元素都已成功获取。
//
// **操作建议:**
//
// 1.  将您本地的 `src/main/resources/static/js/sync_data.js` 文件**全文替换**为上面提供的代码。
// 2.  清除浏览器缓存并重新加载 `datamaintenance.html` 页面。
// 3.  导航到侧边栏的“数据同步”菜单项。
// 4.  **测试:**
//     * 确认页面加载后，四个模块的初始数量是否正确显示。
//     * 点击每个模块的“开始同步”按钮。
//     * 观察按钮是否变为“处理中...”并显示加载动画，进度条是否显示，结果区域是否显示“正在同步...”。
//     * 观察同步完成后（根据后端处理时间），按钮是否恢复，进度条是否隐藏，结果区域是否显示正确的成功/失败消息和颜色，并且上方的统计数量是否已刷新。
//     * 测试并发点击，确认 `isSyncing` 标志是否阻止了同时进行多个同步任务。
//
// 现在，数据同步功能的前端交互逻辑应该已经完成。您需要确保后端的 `SyncServiceImpl` 中的同步方法 (`syncDepartments` 等) 已正确实现才能看到完整的同步
//
