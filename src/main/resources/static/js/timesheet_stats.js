/**
 * 工时统计页面脚本
 * 文件路径: src/main/resources/static/js/timesheet_stats.js
 * 依赖: common.js, pagination.js
 */

/**
 * 工时统计页面脚本
 * 文件路径: src/main/resources/static/js/timesheet_stats.js
 * 修正：使用固定列宽、文本截断和 title 提示解决横向滚动条问题，并减小字体。
 * 依赖: common.js, pagination.js
 */

document.addEventListener('DOMContentLoaded', function() {
    'use strict';

    // --- 配置和常量 ---
    const API_ENDPOINT = '/api/timesheets/statistics';
    const DEFAULT_PAGE_SIZE = 50;
    const DEBOUNCE_DELAY = 300;

    // --- DOM 元素引用 ---
    let filterInputs = document.querySelectorAll('#timesheet-filters .filter-input');
    let tableBody = document.getElementById('timesheet-table-body');
    let loadingIndicator = document.getElementById('loading-indicator');
    let noDataMessage = document.getElementById('no-data-message');
    // pagination.js 会处理分页容器内部的元素

    // --- 状态变量 ---
    let currentPage = 1;
    let currentFilters = {};

    // --- 函数定义 ---

    /** 获取过滤器值 */
    function getFilterValues() {
        const filters = {};
        filterInputs.forEach(input => {
            if (input.name && input.value.trim() !== '') {
                filters[input.name] = input.value.trim();
            }
        });
        return filters;
    }

    /** 格式化日期为 YYYY-MM-DD */
    function formatDate(dateString) {
        if (!dateString) return '';
        try {
            const date = new Date(dateString + 'T00:00:00'); // 确保按本地日期解析
            if (isNaN(date.getTime())) return dateString; // 无效日期返回原值
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        } catch (e) {
            console.error("日期格式化错误:", e);
            return dateString;
        }
    }

    /**
     * 渲染表格数据 (使用固定列宽和文本截断)
     * @param {Array<object>} data - 从 API 获取的数据数组
     */
    function renderTable(data) {
        if (!tableBody) { console.error("renderTable: tableBody not found"); return; }
        tableBody.innerHTML = ''; // 清空表格体
        if (loadingIndicator) loadingIndicator.style.display = 'none';
        if (noDataMessage) noDataMessage.style.display = 'none';

        if (!data || data.length === 0) {
            if (noDataMessage) noDataMessage.style.display = 'block';
            tableBody.innerHTML = `<tr><td colspan="9" class="text-center py-2 text-gray-500 text-sm">暂无数据</td></tr>`; // 使用 text-sm
            return;
        }

        // 定义基础样式和截断样式
        const cellBaseClasses = ['py-2', 'text-sm']; // 统一使用 text-sm
        const cellPadding = 'px-2';
        const truncateClasses = ['overflow-hidden', 'whitespace-nowrap', 'text-ellipsis'];

        data.forEach(item => {
            const row = tableBody.insertRow();
            row.classList.add('hover:bg-gray-50');

            // 创建单元格并应用样式
            // 注意：w-xx 类需要与 thead 中的 th 保持一致！
            let cell;

            // 工时区间 (w-32, 截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, ...truncateClasses);
            cell.textContent = item.tr || '-';
            cell.title = item.tr || ''; // title 显示完整内容

            // 员工 (w-32, 不截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, 'whitespace-nowrap');
            cell.textContent = item.employee || '-';
            cell.title = item.employee || '';

            // // 部门 (w-32, 截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, ...truncateClasses);

            // 原始部门名称
            const rawDep = item.dep || '-';

            // 如果包含“公司”，就取“公司”之后那部分；否则原样显示
            cell.textContent =  AppUtils.getShortName(rawDep,'数智化');
            cell.title = rawDep;  // 保留完整名称到 title 里


            // 状态 (w-20, 不截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, 'whitespace-nowrap', 'text-center');
            cell.textContent = item.ts_status || '-';
            cell.title = item.ts_status || '';

            // 工时日期 (w-28, 不截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, 'whitespace-nowrap', 'text-center');
            cell.textContent = item.ts_date ? formatDate(item.ts_date) : '-';
            cell.title = item.ts_date || '';

            // 小时数 (w-28, 不截断, 右对齐)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, 'whitespace-nowrap', 'text-center');
            cell.textContent = item.ts_hours !== null ? item.ts_hours : '-';
            cell.title = item.ts_hours !== null ? String(item.ts_hours) : '';

            // 工时编码 (w-32, 截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, ...truncateClasses);
            cell.textContent = item.ts_bm || '-';
            cell.title = item.ts_bm || '';

            // 工时名称 (w-32, 截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, ...truncateClasses);
            cell.textContent = item.ts_name || '-';
            cell.title = item.ts_name || '';

            // 利润中心 (w-40, 截断)
            cell = row.insertCell();
            cell.classList.add(...cellBaseClasses, cellPadding, ...truncateClasses);
            // cell.textContent = item.zone || '-';
            // cell.title = item.zone || '';

            // 原始部门名称
            const rawZone = item.zone || '-';

            // 如果包含“基础业务-”，就取之后那部分；否则原样显示
            cell.textContent = rawZone.includes('基础业务-')
                ? rawZone.split('基础业务-').pop()
                : rawZone;
            cell.title = rawZone;  // 保留完整名称到 title 里

        });
    }


    /** 获取数据并更新UI */
    async function fetchTimesheetData(page = 1, filters = {}) {
        currentPage = page;
        currentFilters = filters;

        // 显示加载状态 (使用初始 HTML 中的结构)
        if (tableBody) tableBody.innerHTML = ''; // 清空旧数据
        if (loadingIndicator) loadingIndicator.style.display = 'block'; // 显示加载器
        if (noDataMessage) noDataMessage.style.display = 'none'; // 隐藏无数据提示
        // 重置分页
        if (window.AppUtils && typeof window.AppUtils.setupPagination === 'function') {
            window.AppUtils.setupPagination({ currentPage: page, totalPages: 0, totalRecords: 0 });
        }
        console.log("重置分页成功");

        const params = new URLSearchParams({ page: page, size: DEFAULT_PAGE_SIZE, ...filters });
        const url = `${API_ENDPOINT}?${params.toString()}`;

        try {
            const response = await AppUtils.get(url);
            renderTable(response.data || []); // 调用新的 renderTable

            if (window.AppUtils && typeof window.AppUtils.setupPagination === 'function') {
                window.AppUtils.setupPagination({
                    containerId: 'timesheet-pagination-container',
                    currentPage: response.currentPage || 1,
                    totalPages: response.totalPages || 1,
                    totalRecords: response.totalRecords || 0,
                    maxPagesToShow: 5,
                    onPageChange: (newPage) => fetchTimesheetData(newPage, currentFilters)
                });
                console.log("[Stats] Pagination setup attempted.");
            }
        } catch (error) {
            console.error('获取工时数据失败:', error);
            if(tableBody) tableBody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-red-500 text-sm">加载数据失败: ${error.message || '未知错误'}</td></tr>`; // 使用 text-sm
            if (loadingIndicator) loadingIndicator.style.display = 'none'; // 隐藏加载器
            AppUtils.showMessage(`加载工时数据失败: ${error.message || '请稍后重试'}`, 'error');
        } finally {
            // 确保加载指示器最终被隐藏 (即使渲染成功也可能需要隐藏初始的)
            if (loadingIndicator && (!tableBody || tableBody.rows.length > 0)) {
                loadingIndicator.style.display = 'none';
            }
        }
    }

    /** 设置事件监听器 */
    function setupEventListeners() {
        if (!filterInputs || !tableBody) {
            console.error("Cannot setup listeners, required elements missing.");
            return;
        }
        // 过滤事件 (防抖)
        const debouncedFetch = AppUtils.debounce(() => {
            const filters = getFilterValues();
            fetchTimesheetData(1, filters); // 过滤器改变，回到第一页
        }, DEBOUNCE_DELAY);

        filterInputs.forEach(input => {
            input.addEventListener('input', debouncedFetch);
            if (input.tagName === 'SELECT' || input.type === 'date') { // date 类型也用 change
                input.addEventListener('change', debouncedFetch);
            }
        });
        console.log("Timesheet Stats event listeners setup complete.");
    }

    /** 初始化模块 */
    function init() {
        console.log("Initializing Timesheet Stats Module...");
        // 确保在 DOMContentLoaded 后执行，此时元素应已存在
        // 注意：tableBody 等引用在函数作用域顶部声明，在此处赋值
        const tb = document.getElementById('timesheet-table-body');
        const li = document.getElementById('loading-indicator');
        const ndm = document.getElementById('no-data-message');
        const fi = document.querySelectorAll('#timesheet-filters .filter-input');

        if (!tb || !li || !ndm || !fi) {
            console.error("Timesheet Stats initialization failed: Could not find required elements.");
            return;
        }
        // 将获取到的元素赋值给模块作用域的变量
        tableBody = tb;
        loadingIndicator = li;
        noDataMessage = ndm;
        filterInputs = fi;


        setupEventListeners(); // 设置事件监听
        fetchTimesheetData(1, getFilterValues()); // 加载初始数据
        console.log("Timesheet Stats Module Initialized.");
    }

    // --- 执行初始化 ---
    if (!window.AppUtils) {
        console.error("common.js (AppUtils) not loaded. Timesheet Stats Module cannot initialize properly.");
        return;
    }
    init();

}); // 结束 DOMContentLoaded

// * **说明:**
//     * 代码在 `DOMContentLoaded` 事件后执行。
//     * 定义了 API 端点和分页大小常量。
//     * 获取了过滤输入框、表格体、加载/无数据提示元素的引用。
//     * `getFilterValues`: 从所有带有 `filter-input` 类的输入框中收集有效的过滤条件。
//     * `renderTable`: 负责将从 API 获取的数据动态渲染到 HTML 表格中，并处理空数据和加载状态。
//     * `formatDate`: 一个简单的日期格式化函数。
//     * `fetchTimesheetData`: 核心函数，负责构建 API 请求 URL（包含分页和过滤参数），调用 `AppUtils.get` 发送请求，处理响应（渲染表格、更新分页），并处理错误。
//     * 事件监听：为所有过滤输入框添加了 `input` (或 `change`) 事件监听器，并使用 `common.js` 中的 `AppUtils.debounce` 进行防抖处理，避免在用户连续输入时频繁发送请求。过滤器改变时，会重新加载第一页的数据。
//     * 初始化：页面加载完成后，调用 `fetchTimesheetData()` 获取并显示第一页的


//
// **修改说明:**
//
// 1.  **`renderTable` 函数:**
// * **字体大小:** 为所有单元格 (`<td>`) 添加了 `text-sm` 类。
// * **内边距:** 统一减小了内边距，使用了 `py-2` 和 `px-3`。
// * **固定宽度:** 为每个单元格 (`<td>`) 添加了与 `<thead>` 中对应的 `<th>` 一致的 `w-xx` 类（例如 `w-32`, `w-48`）。**请务必根据您的实际需要调整这些 `w-xx` 值！**
// * **文本截断:** 为需要截断的列（工时区间、部门、工时编码、工时名称、利润中心）添加了 `overflow-hidden`, `whitespace-nowrap`, `text-ellipsis` 类。
// * **`title` 属性:** 为所有应用了截断的单元格添加了 `title` 属性，其值设置为该单元格对应的完整数据 (`item.propertyName || ''`)，以便鼠标悬停时显示完整内容。
// * **不换行列:** 为不需要换行的列（员工、状态、日期、小时数）添加了 `whitespace-nowrap` 类。
// * **加载/无数据提示:** 确保在函数开始时隐藏提示，并在没有数据时正确显示无数据提示（也使用了 `text-sm`）。
// 2.  **`fetchTimesheetData` 函数:**
// * 在出错时显示的提示信息中也加入了 `text-sm`。
// * 在函数开始时清空 `tableBody` 并显示加载状态（如果需要的话，可以通过 CSS 控制 `#loading-indicator` 的显示）。
// * 在 `finally` 块中确保加载指示器被隐藏。
// 3.  **`init` 函数:**
// * 添加了对关键 DOM 元素获取的检查，并在失败时报错。将获取到的元素赋值给模块作用域的变量。
//
// **操作建议:**
//
// 1.  修改 `timesheet_statistics.html` 中的 `<thead>` 部分，为你表格的每一列 (`<th>`) 设置合适的固定宽度 (`w-xx`) 和 `text-sm` 类。
// 2.  将您本地的 `src/main/resources/static/js/timesheet_stats.js` 文件**全文替换**为上面提供的代码。
// 3.  清除浏览器缓存并重新加载 `/timesheets/statistics` 页面。
//
// 现在，表格应该会以较小的字体显示，并且列宽是固定的。如果内容超出列宽，会显示省略号 (...)，鼠标悬停在单元格上时会通过 `title` 提示显示完整内容，并且不应再出现横向滚