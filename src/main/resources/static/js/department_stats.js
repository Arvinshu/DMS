/**
 * 部门工作统计页面脚本
 * 文件路径: src/main/resources/static/js/department_stats.js
 * 职责：处理视图切换、日期选择、API调用、表格渲染、图表渲染和截图导出。
 * 依赖: common.js, flatpickr.min.js, html2canvas.min.js, chart.umd.min.js
 */

document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    console.log("[Stats] DOMContentLoaded event fired."); // 确认脚本开始执行

    // --- 确保依赖库已加载 ---
    let flatpickrLoaded = typeof flatpickr !== 'undefined';
    let html2canvasLoaded = typeof html2canvas !== 'undefined';
    let chartjsLoaded = typeof Chart !== 'undefined'; // Chart.js 暴露的是 Chart 对象
    let appUtilsLoaded = typeof window.AppUtils !== 'undefined';

    if (!appUtilsLoaded) {
        console.error("核心工具库 (common.js) 加载失败！页面功能无法初始化。");
        document.body.innerHTML = '<p class="text-red-500 p-4 text-center">错误：核心工具库加载失败。</p>';
        return;
    } else {
        console.debug("AppUtils (common.js) loaded.");
    }
    if (!flatpickrLoaded) {
        console.error("Flatpickr 库加载失败。日期选择器将不可用。");
        // 可以在页面上提示，或者禁用日期输入框
    } else {
        console.debug("Flatpickr library loaded.");
    }
    if (!chartjsLoaded) {
        console.error("Chart.js 库加载失败。");
    } else {
        console.debug("Chart.js library loaded.");
    }
    if (!html2canvasLoaded) {
        console.error("html2canvas 库加载失败。截图导出功能将不可用。");
        // 可以禁用导出按钮
    } else {
        console.debug("html2canvas library loaded.");
    }

    // --- 配置和常量 ---
    const API_BASE_URL = '/api/stats'; // 统计 API 的基础路径
    const DEFAULT_VIEW = 'department_report'; // 默认显示的视图
    const DATE_FORMAT = 'Y-m-d'; // Flatpickr 日期格式
    const CHART_BG_COLORS = [ // 为柱状图提供一些颜色
        'rgba(54, 162, 235, 0.6)', 'rgba(255, 99, 132, 0.6)', 'rgba(75, 192, 192, 0.6)',
        'rgba(255, 206, 86, 0.6)', 'rgba(153, 102, 255, 0.6)', 'rgba(255, 159, 64, 0.6)',
        'rgba(99, 255, 132, 0.6)', 'rgba(132, 99, 255, 0.6)'
    ];
    const CHART_BORDER_COLORS = [
        'rgba(54, 162, 235, 1)', 'rgba(255, 99, 132, 1)', 'rgba(75, 192, 192, 1)',
        'rgba(255, 206, 86, 1)', 'rgba(153, 102, 255, 1)', 'rgba(255, 159, 64, 1)',
        'rgba(99, 255, 132, 1)', 'rgba(132, 99, 255, 1)'
    ];


    // --- DOM 元素引用 ---
    // 在 init 中获取
    let sidebarLinks, viewContainers, commonInfoContainer, commonHeadNameEl, commonEmpCountEl, commonWeekNumEl,
        startDateInput, endDateInput, refreshButton, exportButton, commonStatsErrorEl;
    const tableContainerIds = {
        deptOverview: 'dept-overview-table-container',
        keyProjects: 'key-projects-table-container',
        pcPivot: 'pc-pivot-table-container',
        employeeDetails: 'employee-details-table-container'
    };
    const chartCanvasIds = {
        profitCenter: 'profitCenterChart',
        monthlyProject: 'monthlyProjectChart'
    };
    const tableElements = {}; // 缓存表格元素
    const chartInstances = {}; // 缓存图表实例 { profitCenter: null, monthlyProject: null }


    // --- 状态变量 ---
    let currentView = DEFAULT_VIEW; // 当前视图 ID
    let currentStartDate = getStartOfYear(); // 当前开始日期 YYYY-MM-DD
    let currentEndDate = getToday();   // 当前结束日期 YYYY-MM-DD
    let flatpickrStartInstance = null;
    let flatpickrEndInstance = null;
    let isLoadingData = false; // 防止重复加载

    // --- 工具函数 ---
    /** 获取年初日期 (YYYY-MM-DD) */
    function getStartOfYear() {
        const now = new Date();
        return `${now.getFullYear()}-01-01`;
    }

    /** 获取今天日期 (YYYY-MM-DD) */
    function getToday() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    /** 计算给定日期是当年的第几周 (ISO 8601 标准，周一为一周开始) */
    function getWeekNumber(date) {
        if (!(date instanceof Date) || isNaN(date)) return null;
        const target = new Date(date.valueOf());
        target.setDate(target.getDate() + 3 - (target.getDay() + 6) % 7);
        const firstThursday = new Date(target.getFullYear(), 0, 4);
        firstThursday.setDate(firstThursday.getDate() + 3 - (firstThursday.getDay() + 6) % 7);
        const weekNumber = 1 + Math.round(((target.valueOf() - firstThursday.valueOf()) / 86400000 - 3 + (firstThursday.getDay() + 6) % 7) / 7);
        return weekNumber;
    }

    /** 格式化日期为 YYYY-MM-DD (用于显示和 API) */
    function formatDateForDisplay(date) {
        if (!(date instanceof Date) || isNaN(date)) return '';
        try {
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        } catch (e) {
            console.error("Error formatting date:", date, e);
            return '无效日期';
        }
    }

    /** 格式化数字为带 N 位小数的字符串 */
    function formatNumber(value, digits = 2) {
        if (value === null || value === undefined || value === '') return '-';
        const num = Number(value);
        if (isNaN(num)) return '无效数字';
        return num.toFixed(digits);
    }

    /** 获取表格元素引用 (带缓存和日志) */
    function getTableElements(containerId) {
        // console.debug(`[getTableElements] Getting elements for container: ${containerId}`); // 日志过于频繁，注释掉
        if (tableElements[containerId]) {
            return tableElements[containerId];
        }
        const container = document.getElementById(containerId);
        if (!container) {
            console.error(`[getTableElements] Container element not found: ${containerId}`);
            return null;
        }
        const elements = {
            container: container,
            loader: container.querySelector('.table-loader'),
            table: container.querySelector('.data-table'),
            thead: container.querySelector('thead'),
            tbody: container.querySelector('tbody'),
            tfoot: container.querySelector('tfoot') // 可能为 null
        };
        if (!elements.table) console.warn(`[getTableElements] '.data-table' not found in #${containerId}`);
        if (!elements.thead) console.warn(`[getTableElements] 'thead' not found in #${containerId}`);
        if (!elements.tbody) console.warn(`[getTableElements] 'tbody' not found in #${containerId}`);
        if (!elements.loader) console.warn(`[getTableElements] '.table-loader' not found in #${containerId}`);

        if (elements.table && elements.thead && elements.tbody) {
            tableElements[containerId] = elements;
            // console.debug(`[getTableElements] Elements cached for: ${containerId}`, elements);
            return elements;
        } else {
            console.error(`[getTableElements] Table structure incomplete for container: ${containerId}. Cannot proceed with rendering.`);
            return null;
        }
    }

    /** 显示指定表格的加载状态 */
    function showTableLoading(containerId) {
        console.debug(`[showTableLoading] Showing loading for: ${containerId}`);
        const elements = getTableElements(containerId);
        if (elements) {
            if (elements.loader) elements.loader.classList.remove('hidden');
            if (elements.table) {
                console.debug(`[showTableLoading] Table classes BEFORE adding hidden for #${containerId}:`, elements.table.classList.toString());
                elements.table.classList.add('hidden'); // 隐藏表格主体
                console.debug(`[showTableLoading] Table classes AFTER adding hidden for #${containerId}:`, elements.table.classList.toString());
            }
            if (elements.tbody) elements.tbody.innerHTML = '';
            if (elements.tfoot) elements.tfoot.innerHTML = '';
        } else {
            console.error(`[showTableLoading] Cannot show loading, elements not found for: ${containerId}`);
        }
    }

    /** 隐藏指定表格的加载状态，并显示表格 (添加日志) */
    function hideTableLoading(containerId) {
        console.debug(`[hideTableLoading] Attempting to hide loading for: ${containerId}`);
        const elements = getTableElements(containerId);
        if (elements) {
            if (elements.loader) elements.loader.classList.add('hidden');
            if (elements.table) {
                console.debug(`[hideTableLoading] Table classes BEFORE removing hidden for #${containerId}:`, elements.table.classList.toString());
                elements.table.classList.remove('hidden'); // 移除 hidden 类以显示表格
                console.debug(`[hideTableLoading] Table classes AFTER removing hidden for #${containerId}:`, elements.table.classList.toString());
                // 强制浏览器重绘 (有时有帮助)
                // void elements.table.offsetWidth;
            } else {
                console.warn(`[hideTableLoading] Table element not found for: ${containerId}`);
            }
        } else {
            console.error(`[hideTableLoading] Cannot hide loading, elements not found for: ${containerId}`);
        }
    }

    /** 在指定表格容器中显示错误信息 */
    function showTableError(containerId, message) {
        console.error(`[showTableError] Showing error for ${containerId}: ${message}`);
        const elements = getTableElements(containerId);
        if (elements) {
            hideTableLoading(containerId);
            if (elements.tbody) {
                const colSpan = elements.thead?.rows[0]?.cells?.length || 1;
                elements.tbody.innerHTML = `<tr><td colspan="${colSpan}" class="text-center py-4 text-red-500 text-xs">${message}</td></tr>`;
            } else {
                // 尝试在容器内显示
                const p4 = elements.container.querySelector('.p-4');
                if (p4) {
                    p4.innerHTML = `<p class="text-center py-4 text-red-500 text-xs">${message}</p>`;
                } else {
                    elements.container.innerHTML = `<p class="text-center py-4 text-red-500 text-xs">${message}</p>`;
                }
            }
        } else {
            console.error(`[showTableError] Cannot show error, elements not found for: ${containerId}`);
        }
    }

    // 新增：显示图表加载状态
    function showChartLoading(canvasId) {
        const canvas = document.getElementById(canvasId);
        const container = canvas?.parentElement;
        if (container) {
            const loader = container.querySelector('.chart-loader');
            if (loader) loader.classList.remove('hidden');
            if (canvas) canvas.classList.add('hidden'); // 隐藏 canvas
        }
    }

    // 新增：隐藏图表加载状态
    function hideChartLoading(canvasId) {
        const canvas = document.getElementById(canvasId);
        const container = canvas?.parentElement;
        if (container) {
            const loader = container.querySelector('.chart-loader');
            if (loader) loader.classList.add('hidden');
            if (canvas) canvas.classList.remove('hidden'); // 显示 canvas
        }
    }

    // 新增：显示图表错误
    function showChartError(canvasId, message) {
        const canvas = document.getElementById(canvasId);
        const container = canvas?.parentElement;
        if (container) {
            hideChartLoading(canvasId); // 先隐藏加载器
            container.innerHTML = `<div class="text-center py-4 text-red-500 text-xs">${message}</div>`; // 直接替换内容
        }
    }


    // --- 视图切换 ---

    /** 切换显示的视图 */

    function switchView(viewId) {
        if (!viewId || isLoadingData) {
            if (isLoadingData) AppUtils.showMessage("正在加载数据，请稍候...", "info");
            return;
        }
        console.log(`[Stats] Switching view to: ${viewId}`);
        currentView = viewId; // 更新当前视图状态

        // 更新侧边栏激活状态
        sidebarLinks.forEach(link => {
            link.classList.toggle('active-view', link.getAttribute('data-view') === viewId);
        });

        // --- 用户提供的修复逻辑 ---
        // 获取所有视图容器
        const viewContainersNodeList = document.querySelectorAll('.stats-view'); // 使用不同的变量名以防冲突
        viewContainersNodeList.forEach(container => {
            const shouldBeHidden = container.id !== `${viewId}-view`;
            container.classList.toggle('hidden', shouldBeHidden);
            console.debug(`[Stats] Container ${container.id} hidden: ${shouldBeHidden}`);
        });

        // 强制检查数据可视化视图 (可能不再需要，但保留以防万一)
        const visualizationView = document.getElementById('data-visualization-view');
        if (viewId === 'data_visualization' && visualizationView && visualizationView.classList.contains('hidden')) {
            visualizationView.classList.remove('hidden');
            console.warn('[Stats] Forced removal of hidden class from data-visualization-view');
        }
        // 强制检查部门周报视图
        const departmentView = document.getElementById('department-report-view');
        if (viewId === 'department_report' && departmentView && departmentView.classList.contains('hidden')) {
            departmentView.classList.remove('hidden');
            console.warn('[Stats] Forced removal of hidden class from department-report-view');
        }
        // 强制检查员工工时明细视图
        const employeeView = document.getElementById('employee-detail-view');
        if (viewId === 'employee_detail' && employeeView && employeeView.classList.contains('hidden')) {
            employeeView.classList.remove('hidden');
            console.warn('[Stats] Forced removal of hidden class from employee-detail-view');
        }

        // 刷新当前视图数据
        reloadDataForCurrentView();
    }

    // --- 数据加载 ---

    /** 根据当前视图和日期范围重新加载数据 */
    function reloadDataForCurrentView() {
        if (isLoadingData) {
            console.log("[Stats] Data is already loading, skipping reload request.");
            return;
        }
        // 从 Flatpickr 实例或输入框获取当前日期
        currentStartDate = flatpickrStartInstance?.input?.value || getStartOfYear();
        currentEndDate = flatpickrEndInstance?.input?.value || getToday();

        if (!currentStartDate || !currentEndDate || currentStartDate > currentEndDate) {
            AppUtils.showMessage("请选择有效的开始和结束日期范围。", "warning");
            return;
        }

        console.log(`[Stats] Reloading data for view: ${currentView}, Dates: ${currentStartDate} to ${currentEndDate}`);
        isLoadingData = true;

        loadCommonInfo(currentStartDate, currentEndDate).finally(() => {
            // 确保在 finally 中重置 isLoadingData 标志，即使加载失败
            // 但具体视图的数据加载也需要 finally
            let viewLoadPromise;
            if (currentView === 'department_report') {
                viewLoadPromise = loadDepartmentReportData(currentStartDate, currentEndDate);
            } else if (currentView === 'employee_detail') {
                viewLoadPromise = loadEmployeeDetailsData(currentStartDate, currentEndDate);
            } else if (currentView === 'data_visualization') { // 新增可视化视图加载
                viewLoadPromise = loadVisualizationData(currentStartDate, currentEndDate);
            } else {
                viewLoadPromise = Promise.resolve();
            }
            viewLoadPromise.finally(() => {
                console.log(`[Stats] Finished loading data for view: ${currentView}`);
                isLoadingData = false;
            });
        });
    }

    /** 加载公共信息区数据 */
    async function loadCommonInfo(startDate, endDate) {
        console.log("[Stats] Loading common info...");
        if (commonStatsErrorEl) commonStatsErrorEl.textContent = '';
        if (commonHeadNameEl) commonHeadNameEl.textContent = '加载中...';
        if (commonEmpCountEl) commonEmpCountEl.textContent = '...';
        if (commonWeekNumEl) commonWeekNumEl.textContent = '...';

        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);

        try {
            const info = await AppUtils.get(`${API_BASE_URL}/common-info?${params.toString()}`);
            console.log("[Stats] Common info received:", info);
            if (commonHeadNameEl) commonHeadNameEl.textContent = info.primaryDepartmentHeadName || '未指定';
            if (commonEmpCountEl) commonEmpCountEl.textContent = info.totalEmployeeCount ?? 'N/A';
            if (info.currentWeekOfYear !== null && info.currentWeekOfYear !== undefined) {
                if (commonWeekNumEl) commonWeekNumEl.textContent = info.currentWeekOfYear;
            } else if (endDate) {
                const weekNum = getWeekNumber(new Date(endDate + 'T00:00:00'));
                if (commonWeekNumEl) commonWeekNumEl.textContent = weekNum ?? 'N/A';
            } else {
                if (commonWeekNumEl) commonWeekNumEl.textContent = 'N/A';
            }
        } catch (error) {
            console.error("[Stats] 获取公共信息失败:", error);
            const errorMsg = `获取公共信息失败: ${error.message || '请稍后重试'}`;
            if (commonStatsErrorEl) commonStatsErrorEl.textContent = errorMsg;
            if (commonHeadNameEl) commonHeadNameEl.textContent = '错误';
            if (commonEmpCountEl) commonEmpCountEl.textContent = '错误';
            if (commonWeekNumEl) commonWeekNumEl.textContent = '错误';
        }
    }

    /** 加载部门周报统计视图数据 */
    async function loadDepartmentReportData(startDate, endDate) {
        console.log("[Stats] Loading department report data...");
        showTableLoading(tableContainerIds.deptOverview);
        showTableLoading(tableContainerIds.keyProjects);
        showTableLoading(tableContainerIds.pcPivot);

        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        const queryString = params.toString();

        try {
            const results = await Promise.allSettled([
                AppUtils.get(`${API_BASE_URL}/department-overview?${queryString}`),
                AppUtils.get(`${API_BASE_URL}/key-projects?${queryString}`),
                AppUtils.get(`${API_BASE_URL}/profit-center-pivot?${queryString}`)
            ]);
            console.log("[Stats] Department report API results:", results);

            if (results[0].status === 'fulfilled') {
                console.log("[Stats] Rendering Department Overview Table...");
                renderDepartmentOverviewTable(results[0].value || []);
            } else {
                console.error("[Stats] 获取部门概览数据失败:", results[0].reason);
                showTableError(tableContainerIds.deptOverview, `加载部门概览数据失败: ${results[0].reason.message || '未知错误'}`);
            }
            if (results[1].status === 'fulfilled') {
                console.log("[Stats] Rendering Key Project Table...");
                renderKeyProjectTable(results[1].value || []);
            } else {
                console.error("[Stats] 获取重点项目数据失败:", results[1].reason);
                showTableError(tableContainerIds.keyProjects, `加载重点项目数据失败: ${results[1].reason.message || '未知错误'}`);
            }
            if (results[2].status === 'fulfilled') {
                console.log("[Stats] Rendering Profit Center Pivot Table...");
                renderProfitCenterTable(results[2].value || {departmentHeaders: [], rows: [], departmentTotals: {}});
            } else {
                console.error("[Stats] 获取利润中心交叉表数据失败:", results[2].reason);
                showTableError(tableContainerIds.pcPivot, `加载利润中心数据失败: ${results[2].reason.message || '未知错误'}`);
            }
        } catch (error) {
            console.error("[Stats] 加载部门周报数据时发生意外错误:", error); // Should not happen with allSettled
            AppUtils.showMessage("加载部门周报数据时发生意外错误", "error");
        } finally {
            console.log("[Stats] Hiding loaders for department report view.");
            hideTableLoading(tableContainerIds.deptOverview);
            hideTableLoading(tableContainerIds.keyProjects);
            hideTableLoading(tableContainerIds.pcPivot);
        }
    }

    /** 加载员工工时明细视图数据 */
    async function loadEmployeeDetailsData(startDate, endDate) {
        console.log("[Stats] Loading employee details data...");
        showTableLoading(tableContainerIds.employeeDetails);

        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        const url = `${API_BASE_URL}/employee-details?${params.toString()}`;

        try {
            const data = await AppUtils.get(url);
            console.log("[Stats] Employee details data received, rendering table...");
            renderEmployeeTimesheetTable(data || []);
        } catch (error) {
            console.error("[Stats] 获取员工工时明细失败:", error);
            showTableError(tableContainerIds.employeeDetails, `加载员工工时明细失败: ${error.message || '未知错误'}`);
        } finally {
            console.log("[Stats] Hiding loader for employee details view.");
            hideTableLoading(tableContainerIds.employeeDetails);
        }
    }

    /** 加载数据可视化视图所需的数据 */
    async function loadVisualizationData(startDate, endDate) {
        console.log("[Stats] Loading visualization data...");
        showChartLoading(chartCanvasIds.profitCenter);
        showChartLoading(chartCanvasIds.monthlyProject);

        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        const queryString = params.toString();

        try {
            const results = await Promise.allSettled([
                AppUtils.get(`${API_BASE_URL}/profit-center-pivot?${queryString}`), // 柱状图数据
                AppUtils.get(`${API_BASE_URL}/monthly-project-counts?${queryString}`) // 折线图数据
            ]);
            console.log("[Stats] Visualization API results:", results);

            // 处理利润中心数据 -> 柱状图
            if (results[0].status === 'fulfilled') {
                const pivotData = results[0].value || {departmentHeaders: [], rows: []};
                const barChartData = processPivotDataForBarChart(pivotData);
                renderBarChart(barChartData);
            } else {
                console.error("[Stats] 获取利润中心交叉表数据失败:", results[0].reason);
                showChartError(chartCanvasIds.profitCenter, `加载利润中心图表数据失败: ${results[0].reason.message || '未知错误'}`);
            }

            // 处理月度项目数 -> 折线图
            if (results[1].status === 'fulfilled') {
                const monthlyData = results[1].value || [];
                const lineChartData = processMonthlyDataForLineChart(monthlyData);
                renderLineChart(lineChartData);
            } else {
                console.error("[Stats] 获取月度项目数失败:", results[1].reason);
                showChartError(chartCanvasIds.monthlyProject, `加载月度项目数图表失败: ${results[1].reason.message || '未知错误'}`);
            }

        } catch (error) { // allSettled 不会 reject
            console.error("[Stats] 加载可视化数据时发生意外错误:", error);
        } finally {
            hideChartLoading(chartCanvasIds.profitCenter);
            hideChartLoading(chartCanvasIds.monthlyProject);
        }
    }

    // --- 数据处理 (图表) ---
    // --- 数据处理 (图表 - 修正折线图) ---
    function processPivotDataForBarChart(pivotData) {
        console.debug("[processPivotDataForBarChart] Processing pivot data:", pivotData);
        const labels = pivotData.rows.map(row => row.profitCenterRemark || '未知');
        const departmentHeaders = pivotData.departmentHeaders || [];
        const datasets = [];
        departmentHeaders.forEach((deptName, index) => {
            const dataset = {
                label: deptName,
                data: pivotData.rows.map(row => {
                    const workdays = row.workdaysByDepartment ? row.workdaysByDepartment[deptName] : null;
                    const num = parseFloat(workdays);
                    return isNaN(num) ? 0 : num;
                }),
                backgroundColor: CHART_BG_COLORS[index % CHART_BG_COLORS.length],
                borderColor: CHART_BORDER_COLORS[index % CHART_BORDER_COLORS.length],
                borderWidth: 1
            };
            datasets.push(dataset);
        });
        console.debug("[processPivotDataForBarChart] Processed data:", {labels, datasets});
        return {labels, datasets};
    }

    /** 处理月度数据为折线图格式 */

    /** 处理月度数据为折线图格式 (修正：只显示到结束月份) */
    /** 处理月度数据为折线图格式 (修正：X轴12个月，数据线到结束月) */
    function processMonthlyDataForLineChart(monthlyData) {
        console.debug("[processMonthlyDataForLineChart] Processing monthly data:", monthlyData);

        // 始终显示 12 个月标签
        const labels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
        // 创建 12 个数据点，默认为 null (null 会导致线条中断)
        const data = Array(12).fill(null);

        // 确定最后一个需要显示数据的月份 (基于当前结束日期)
        const endMonth = new Date(currentEndDate + 'T00:00:00').getMonth() + 1; // 1-12

        // 创建一个 Map 方便查找月份数据
        const countsMap = new Map();
        monthlyData.forEach(item => {
            countsMap.set(item.month, item.projectCount);
        });

        // 填充数据，只填充到结束月份，缺失的用 0
        for (let month = 1; month <= endMonth; month++) {
            data[month - 1] = countsMap.get(month) || 0; // 索引是 month - 1
        }
        // endMonth 之后的月份保持为 null

        const datasets = [{
            label: '月度项目数量',
            data: data,
            fill: false,
            borderColor: 'rgb(75, 192, 192)',
            tension: 0.1,
            spanGaps: false // 明确设置不在 null 数据点之间连接线条
        }];

        console.debug("[processMonthlyDataForLineChart] Processed data:", {labels, datasets});
        return {labels, datasets};
    }

    // --- 图表渲染 ---

    /** 渲染分组柱状图 */
    function renderBarChart(chartData) {
        if (!chartjsLoaded) return; // 检查 Chart.js 是否加载
        const canvas = document.getElementById(chartCanvasIds.profitCenter);
        if (!canvas) {
            console.error("Profit center chart canvas not found.");
            return;
        }
        const ctx = canvas.getContext('2d');

        // 销毁旧图表实例
        if (chartInstances.profitCenter) {
            console.debug("Destroying previous profit center chart instance.");
            chartInstances.profitCenter.destroy();
        }

        console.log("Rendering Profit Center Bar Chart with data:", chartData);
        try {
            chartInstances.profitCenter = new Chart(ctx, {
                type: 'bar',
                data: chartData,
                options: {
                    responsive: true,
                    maintainAspectRatio: false, // 允许图表不按比例缩放以适应容器高度
                    plugins: {
                        title: {display: true, text: '各部门在利润中心投入人天分布'},
                        tooltip: {mode: 'index', intersect: false} // 提示框显示同一索引的所有数据
                    },
                    scales: {
                        x: {stacked: false, title: {display: true, text: '利润中心'}}, // X轴非堆叠
                        y: {stacked: false, beginAtZero: true, title: {display: true, text: '人天数'}} // Y轴非堆叠
                    }
                }
            });
        } catch (error) {
            console.error("Error rendering bar chart:", error);
            showChartError(chartCanvasIds.profitCenter, "渲染柱状图时出错");
        }
    }

    /** 渲染月度项目数折线图 */
    function renderLineChart(chartData) {
        if (!chartjsLoaded) return;
        const canvas = document.getElementById(chartCanvasIds.monthlyProject);
        if (!canvas) {
            console.error("Monthly project chart canvas not found.");
            return;
        }
        const ctx = canvas.getContext('2d');

        if (chartInstances.monthlyProject) {
            console.debug("Destroying previous monthly project chart instance.");
            chartInstances.monthlyProject.destroy();
        }

        console.log("Rendering Monthly Project Line Chart with data:", chartData);
        try {
            chartInstances.monthlyProject = new Chart(ctx, {
                type: 'line',
                data: chartData,
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {display: true, text: '月度项目数量趋势'},
                        legend: {display: false} // 单条线可以不显示图例
                    },
                    scales: {
                        x: {title: {display: true, text: '月份'}},
                        y: {beginAtZero: true, title: {display: true, text: '项目数量'}}
                    }
                }
            });
        } catch (error) {
            console.error("Error rendering line chart:", error);
            showChartError(chartCanvasIds.monthlyProject, "渲染折线图时出错");
        }
    }


    // --- 表格渲染函数 (添加详细日志和 Try-Catch) ---

    /** 渲染部门整体情况表格 */
    function renderDepartmentOverviewTable(data) {
        console.log("[renderDepartmentOverviewTable] Started. Data length:", data.length);
        const elements = getTableElements(tableContainerIds.deptOverview);
        if (!elements) {
            console.error("[renderDepartmentOverviewTable] Elements not found.");
            return;
        }
        const {thead, tbody, tfoot} = elements; // 解构获取元素
        tbody.innerHTML = '';
        tfoot.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4 text-gray-500 text-xs">无部门数据</td></tr>`;
            console.log("[renderDepartmentOverviewTable] No data, rendering empty message.");
            return;
        }

        let totalWorkdaysSum = 0, nonProjectWorkdaysSum = 0, projectWorkdaysSum = 0;
        // 使用 Big.js 或 Decimal.js 进行精确计算可能更好，但暂时用 parseFloat
        // 或者确保后端返回的就是精确的数字字符串

        try {
            data.forEach((item, index) => {
                try {
                    console.debug(`[renderDepartmentOverviewTable] Rendering row ${index}`);
                    if (!item) {
                        console.warn(`[renderDepartmentOverviewTable] Item at index ${index} is null/undefined. Skipping.`);
                        return;
                    }

                    const row = tbody.insertRow();
                    row.classList.add('hover:bg-gray-50');

                    const totalWd = parseFloat(item.totalWorkdays || 0);
                    const nonProjWd = parseFloat(item.nonProjectWorkdays || 0);
                    const projWd = parseFloat(item.projectWorkdays || 0);
                    const rate = parseFloat(item.projectWorkdayRate || 0);
                    totalWorkdaysSum += totalWd;
                    nonProjectWorkdaysSum += nonProjWd;
                    projectWorkdaysSum += projWd;

                    // 确保使用 Tailwind 类来控制样式，而不是依赖 style.css 中的 @apply
                    row.innerHTML = `
                    <td class="px-3 py-2 text-xs w-16 text-center font-medium">${item.departmentId}</td>
                    <td class="px-3 py-2 text-xs w-48" title="${item.departmentName || '-'}"> ${formatDepName(item.departmentName)}</td>
                    <td class="px-3 py-2 text-xs w-24">${item.departmentLevel || '-'}</td>
                    <td class="px-3 py-2 text-xs w-32">${item.managerDisplay || '-'}</td>
                    <td class="px-3 py-2 text-xs w-16 text-center">${item.employeeCount ?? '-'}</td>
                    <td class="px-3 py-2 text-xs w-64 wrap-cell" title="${(item.employeeList || []).join(', ')}">${(item.employeeList || []).join(', ')}</td>
                    <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(totalWd)}</td>
                    <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(nonProjWd)}</td>
                    <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(projWd)}</td>
                    <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(rate)}%</td>
                `;
                } catch (error) {
                    console.error(`[renderDepartmentOverviewTable] Error rendering row index ${index}:`, error, "Data:", item);
                    const errorRow = tbody.insertRow();
                    errorRow.innerHTML = `<td colspan="10" class="text-red-500 text-xs px-3 py-2">渲染行 ${index + 1} 出错</td>`;
                }
            });
            console.log(`[renderDepartmentOverviewTable] Finished loop. tbody contains ${tbody.rows.length} rows.`);
        } catch (error) {
            console.log(`errrrrows.`);
        }


        // 渲染汇总行
        if (tfoot) { // 检查 tfoot 是否存在
            const summaryRow = tfoot.insertRow();
            summaryRow.classList.add('bg-gray-100', 'font-semibold'); // 使用 font-semibold 代替 font-bold
            const totalRate = totalWorkdaysSum > 0 ? (projectWorkdaysSum * 100 / totalWorkdaysSum) : 0;
            summaryRow.innerHTML = `
                <td colspan="6" class="px-3 py-2 text-xs text-right font-semibold">总计</td>
                <td class="px-3 py-2 text-xs w-28 text-right font-semibold">${formatNumber(totalWorkdaysSum)}</td>
                <td class="px-3 py-2 text-xs w-28 text-right font-semibold">${formatNumber(nonProjectWorkdaysSum)}</td>
                <td class="px-3 py-2 text-xs w-28 text-right font-semibold">${formatNumber(projectWorkdaysSum)}</td>
                <td class="px-3 py-2 text-xs w-28 text-right font-semibold">${formatNumber(totalRate)}%</td>
            `;
        } else {
            console.warn("[renderDepartmentOverviewTable] tfoot element not found for summary row.");
        }
        console.log("[renderDepartmentOverviewTable] Finished rendering.");
    }

    /** 渲染重点项目表格 */
    function renderKeyProjectTable(data) {
        console.log("[renderKeyProjectTable] Started. Data length:", data.length);
        const elements = getTableElements(tableContainerIds.keyProjects);
        if (!elements) {
            console.error("[renderKeyProjectTable] Elements not found.");
            return;
        }
        const {thead, tbody} = elements;
        thead.innerHTML = '';
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center py-4 text-gray-500 text-xs">无符合条件的重点项目</td></tr>`;
            return;
        }

        try {
            const departmentSet = new Set();
            data.forEach(row => {
                if (row.employeesByDepartment) {
                    Object.keys(row.employeesByDepartment).forEach(depName => departmentSet.add(depName));
                }
            });
            const departmentHeaders = Array.from(departmentSet).sort();
            console.debug("[renderKeyProjectTable] Dynamic headers:", departmentHeaders);

            let headerHtml = '<tr>';
            headerHtml += '<th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-32">项目编码</th>';
            headerHtml += '<th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-48">项目名称</th>';
            headerHtml += '<th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-28 text-right">累计人天</th>';
            departmentHeaders.forEach(depName => {
                headerHtml += `
                <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-40" title="${depName}"> ${formatDepName(depName)} </th>`;
            });
            headerHtml += '</tr>';
            thead.innerHTML = headerHtml;

            data.forEach((item, index) => {
                try {
                    console.debug(`[renderKeyProjectTable] Rendering row ${index}`);
                    if (!item) {
                        console.warn(`[renderKeyProjectTable] Item at index ${index} is null/undefined. Skipping.`);
                        return;
                    }
                    const row = tbody.insertRow();
                    row.classList.add('hover:bg-gray-50');

                    let cell = row.insertCell();
                    cell.classList.add('px-3', 'py-2', 'text-xs', 'w-32', 'overflow-hidden', 'whitespace-nowrap', 'text-ellipsis');
                    cell.textContent = item.projectCode || '-';
                    cell.title = item.projectCode || '';
                    cell = row.insertCell();
                    cell.classList.add('px-3', 'py-2', 'text-xs', 'w-48', 'overflow-hidden', 'whitespace-nowrap', 'text-ellipsis');
                    cell.textContent = item.projectName || '-';
                    cell.title = item.projectName || '';
                    cell = row.insertCell();
                    cell.classList.add('px-3', 'py-2', 'text-xs', 'w-28', 'text-right');
                    cell.textContent = formatNumber(item.totalWorkdays);

                    departmentHeaders.forEach(depName => {
                        cell = row.insertCell();
                        cell.classList.add('px-3', 'py-2', 'text-xs', 'w-40', 'wrap-cell'); // 使用 wrap-cell
                        const employees = item.employeesByDepartment ? item.employeesByDepartment[depName] : null;
                        if (employees && employees.length > 0) {
                            cell.innerHTML = employees.join('<br>');
                            cell.title = employees.join(', ');
                        } else {
                            cell.textContent = '-';
                        }
                    });
                    // --- 新增日志：检查是否添加了行 ---
                    console.log(`[renderKeyProjectTable] Finished loop. tbody contains ${tbody.rows.length} rows.`);
                } catch (error) {
                    console.error(`[renderKeyProjectTable] Error rendering row index ${index}:`, error, "Data:", item);
                    const errorRow = tbody.insertRow();
                    errorRow.innerHTML = `<td colspan="${3 + departmentHeaders.length}" class="text-red-500 text-xs px-3 py-2">渲染行 ${index + 1} 出错</td>`;
                }
                console.log("[renderKeyProjectTable] Finished rendering.");
            });
        } catch (error) {
            console.error("[renderKeyProjectTable] Error during rendering:", error);
            showTableError(tableContainerIds.keyProjects, "渲染重点项目数据时出错，请查看控制台日志。");
        }
        console.log("[renderKeyProjectTable] Finished rendering.");
    }

    /** 渲染利润中心交叉表 */
    function renderProfitCenterTable(pivotData) {
        console.log("[renderProfitCenterTable] Started. Data:", pivotData);
        const elements = getTableElements(tableContainerIds.pcPivot);
        if (!elements) {
            console.error("[renderProfitCenterTable] Elements not found.");
            return;
        }
        const {thead, tbody, tfoot} = elements;
        thead.innerHTML = '';
        tbody.innerHTML = '';
        tfoot.innerHTML = '';

        const {departmentHeaders = [], rows = [], departmentTotals = {}} = pivotData;

        if (rows.length === 0) {
            tbody.innerHTML = `<tr><td colspan="2" class="text-center py-4 text-gray-500 text-xs">无利润中心数据</td></tr>`;
            return;
        }

        try {
            let headerHtml = '<tr>';
            headerHtml += '<th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">序号</th>';
            headerHtml += '<th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-56">利润中心</th>';
            departmentHeaders.forEach(depName => {
                headerHtml += `<th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-28 text-right" title="${depName}"> ${formatDepName(depName)}</th>`;
            });
            headerHtml += '</tr>';
            thead.innerHTML = headerHtml;

            rows.forEach((item, index) => {
                try {
                    console.debug(`[renderProfitCenterTable] Rendering row ${index}`);
                    if (!item) {
                        console.warn(`[renderProfitCenterTable] Item at index ${index} is null/undefined. Skipping.`);
                        return;
                    }
                    const row = tbody.insertRow();
                    row.classList.add('hover:bg-gray-50');
                    let cell = row.insertCell();
                    cell.classList.add('px-3', 'py-2', 'text-xs', 'w-12', 'text-center');
                    cell.textContent = item.sequenceNumber;
                    cell = row.insertCell();
                    cell.classList.add('px-3', 'py-2', 'text-xs', 'w-56', 'overflow-hidden', 'whitespace-nowrap', 'text-ellipsis');
                    cell.textContent = item.profitCenterRemark || '-';
                    cell.title = item.profitCenterRemark || '';
                    departmentHeaders.forEach(depName => {
                        cell = row.insertCell();
                        cell.classList.add('px-3', 'py-2', 'text-xs', 'w-28', 'text-right');
                        const workdays = item.workdaysByDepartment ? item.workdaysByDepartment[depName] : null;
                        // cell.textContent = formatNumber(workdays);

                        const displayNumber = formatNumber(workdays) || '-';

                        // 动态添加颜色级别类
                        if (typeof workdays === 'number') {
                            const level = getWorkdaysLevel(workdays);
                            cell.classList.add(`workdays-level-${level}`);
                        }
                        cell.textContent = displayNumber;

                    });
                } catch (error) {
                    console.error(`[renderProfitCenterTable] Error rendering row index ${index}:`, error, "Data:", item);
                    const errorRow = tbody.insertRow();
                    errorRow.innerHTML = `<td colspan="${2 + departmentHeaders.length}" class="text-red-500 text-xs px-3 py-2">渲染行 ${index + 1} 出错</td>`;
                }
            });
            // --- 新增日志：检查是否添加了行 ---
            console.log(`[renderProfitCenterTable] Finished loop. tbody contains ${tbody.rows.length} rows.`);

            if (tfoot) {
                const summaryRow = tfoot.insertRow();
                summaryRow.classList.add('bg-gray-100', 'font-semibold');
                summaryRow.innerHTML = `<td colspan="2" class="px-3 py-2 text-xs text-right font-semibold">总计</td>`; // 使用 font-semibold
                departmentHeaders.forEach(depName => {
                    const total = departmentTotals[depName];
                    const cell = summaryRow.insertCell();
                    cell.classList.add('px-3', 'py-2', 'text-xs', 'w-28', 'text-right', 'font-semibold');
                    cell.textContent = formatNumber(total);
                });
            }
        } catch (error) {
            console.error("[renderProfitCenterTable] Error during rendering:", error);
            showTableError(tableContainerIds.pcPivot, "渲染利润中心数据时出错，请查看控制台日志。");
        }
        console.log("[renderProfitCenterTable] Finished rendering.");
    }

    /** 渲染员工工时明细表格 */
    function renderEmployeeTimesheetTable(data) {
        console.log("[renderEmployeeTimesheetTable] Started. Data length:", data.length);
        const elements = getTableElements(tableContainerIds.employeeDetails);
        if (!elements) {
            console.error("[renderEmployeeTimesheetTable] Elements not found.");
            return;
        }
        const {thead, tbody} = elements;
        thead.innerHTML = '';
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-gray-500 text-xs">无员工工时数据</td></tr>`;
            return;
        }

        try {
            thead.innerHTML = `
                <tr>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-32">员工信息</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-28 text-right">总工时(人天)</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-28 text-right">非项目工时(人天)</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-28 text-right">项目工时(人天)</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-28 text-right">项目工时率(%)</th>
                    <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24 text-center">项目数量</th>
                </tr>
            `;

            data.forEach((item, index) => {
                try {
                    console.debug(`[renderEmployeeTimesheetTable] Rendering row ${index}`);
                    if (!item) {
                        console.warn(`[renderEmployeeTimesheetTable] Item at index ${index} is null/undefined. Skipping.`);
                        return;
                    }
                    const row = tbody.insertRow();
                    row.classList.add('hover:bg-gray-50');
                    row.innerHTML = `
                         <td class="px-3 py-2 text-xs w-32">${item.employee || '-'}</td>
                         <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(item.totalWorkdays)}</td>
                         <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(item.nonProjectWorkdays)}</td>
                         <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(item.projectWorkdays)}</td>
                         <td class="px-3 py-2 text-xs w-28 text-right">${formatNumber(item.projectWorkdayRate)}%</td>
                         <td class="px-3 py-2 text-xs w-24 text-center">${item.projectCount ?? '-'}</td>
                     `;
                } catch (error) {
                    console.error(`[renderEmployeeTimesheetTable] Error rendering row index ${index}:`, error, "Data:", item);
                    const errorRow = tbody.insertRow();
                    errorRow.innerHTML = `<td colspan="6" class="text-red-500 text-xs px-3 py-2">渲染行 ${index + 1} 出错</td>`;
                }
            });
            // --- 新增日志：检查是否添加了行 ---
            console.log(`[renderEmployeeTimesheetTable] Finished loop. tbody contains ${tbody.rows.length} rows.`);
        } catch (error) {
            console.error("[renderEmployeeTimesheetTable] Error during rendering:", error);
            showTableError(tableContainerIds.employeeDetails, "渲染员工明细数据时出错，请查看控制台日志。");
        }
        console.log("[renderEmployeeTimesheetTable] Finished rendering.");
    }

    // --- 截图导出 ---
    function handleExportScreenshot() {
        console.log("[Stats] Export screenshot button clicked.");
        const commonInfoElClone = commonInfoContainer?.cloneNode(true);
        const activeViewEl = document.querySelector('.stats-view:not(.hidden)');
        if (!commonInfoElClone || !activeViewEl || !html2canvasLoaded) {
            AppUtils.showMessage("无法导出：页面内容未完全加载或截图库缺失。", "error");
            return;
        }
        const activeViewElClone = activeViewEl.cloneNode(true);

        const captureWrapper = document.createElement('div');
        captureWrapper.style.padding = '1rem';
        captureWrapper.style.backgroundColor = '#f9fafb';
        captureWrapper.style.width = '1100px'; // 尝试固定宽度
        captureWrapper.appendChild(commonInfoElClone);
        captureWrapper.appendChild(activeViewElClone);
        captureWrapper.style.position = 'absolute';
        captureWrapper.style.left = '-9999px';
        document.body.appendChild(captureWrapper);

        AppUtils.showMessage("正在生成截图...", "info");

        html2canvas(captureWrapper, {scale: 1.5, useCORS: true, logging: false})
            .then(canvas => {
                const imageData = canvas.toDataURL('image/jpeg', 0.9);
                const link = document.createElement('a');
                link.href = imageData;
                const today = getToday();
                const viewName = currentView === 'department_report' ? '部门周报' : '员工明细';
                link.download = `部门工作统计_${viewName}_${today}.jpg`;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                AppUtils.showMessage("截图已导出", "success");
            }).catch(err => {
            console.error("截图导出失败:", err);
            AppUtils.showMessage("截图导出失败: " + err.message, "error");
        }).finally(() => {
            document.body.removeChild(captureWrapper);
        });
    }

    // --- 初始化与事件监听 ---

    /** 初始化日期选择器 */
    function initDatePickers() {
        if (!flatpickrLoaded || !startDateInput || !endDateInput) return;
        // 引入中文语言包 (需要在 HTML 中引入对应的 flatpickr/dist/l10n/zh.js)
        flatpickr.localize(flatpickr.l10ns.zh);

        const commonOptions = {dateFormat: DATE_FORMAT, locale: "zh"};

        flatpickrStartInstance = flatpickr(startDateInput, {
            ...commonOptions,
            defaultDate: getStartOfYear(),
            onChange: function (selectedDates, dateStr) {
                currentStartDate = dateStr;
                if (flatpickrEndInstance) flatpickrEndInstance.set('minDate', selectedDates[0]);
            },
        });

        flatpickrEndInstance = flatpickr(endDateInput, {
            ...commonOptions,
            defaultDate: getToday(),
            minDate: startDateInput.value,
            onChange: function (selectedDates, dateStr) {
                currentEndDate = dateStr;
            },
        });

        currentStartDate = startDateInput.value;
        currentEndDate = endDateInput.value;
        console.log(`[Stats] Initial date range: ${currentStartDate} to ${currentEndDate}`);
    }

    /* 工具函数 将部门名称按照'数智化'进行拆分，丢弃名称前面的公共部分，保留部门短名称*/
    function formatDepName(name) {
        if (!name) return '-';
        return name.includes('数智化')
            ? name.split('数智化').pop().trim()
            : name;
    }

    /* 工具函数 在工具函数区域添加颜色生成器 */
    function getWorkdaysLevel(num) {
        if (num === 0) return 0;
        return Math.min(Math.ceil(num / 20), 10);
    }


    /** 设置事件监听器 */
    function setupEventListeners() {
        console.debug("[Stats] Setting up event listeners.");
        // 侧边栏视图切换
        sidebarLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const viewId = link.getAttribute('data-view');
                if (viewId && viewId !== currentView && !isLoadingData) {
                    switchView(viewId);
                } else if (isLoadingData) {
                    AppUtils.showMessage("正在加载数据，请稍候...", "info");
                }
            });
        });

        if (!sidebarLinks || !refreshButton /* ... 其他检查 ... */) {
            console.error("[Stats] Cannot setup listeners, required elements missing.");
            return;
        }

        // 查询按钮
        if (refreshButton) {
            refreshButton.addEventListener('click', () => {
                console.log("[Stats] Refresh button clicked.");
                if (!isLoadingData) {
                    currentStartDate = flatpickrStartInstance?.input?.value || getStartOfYear();
                    currentEndDate = flatpickrEndInstance?.input?.value || getToday();
                    reloadDataForCurrentView();
                } else {
                    AppUtils.showMessage("正在加载数据，请稍候...", "info");
                }
            });
        }

        // 截图按钮
        if (exportButton && html2canvasLoaded) {
            exportButton.addEventListener('click', handleExportScreenshot);
        } else if (exportButton) {
            exportButton.disabled = true;
            exportButton.title = "截图库未加载";
            console.warn("Export button disabled because html2canvas is not loaded.");
        }
        console.log("[Stats] Event listeners setup complete.");

    }

    /** 初始化模块 */
    function init() {
        console.log("Initializing Department Stats Module...");
        let allElementsFound = true;
        // 获取 DOM 元素引用
        sidebarLinks = document.querySelectorAll('#stats-sidebar .stats-sidebar-link');
        viewContainers = document.querySelectorAll('.stats-view');
        commonInfoContainer = document.getElementById('common-stats-info');
        commonHeadNameEl = document.getElementById('common-head-name');
        commonEmpCountEl = document.getElementById('common-emp-count');
        commonWeekNumEl = document.getElementById('common-week-num');
        startDateInput = document.getElementById('stats-start-date');
        endDateInput = document.getElementById('stats-end-date');
        refreshButton = document.getElementById('refresh-stats-button');
        exportButton = document.getElementById('export-screenshot-button');
        commonStatsErrorEl = document.getElementById('common-stats-error');

        // 检查核心元素
        const criticalElements = [commonInfoContainer, startDateInput, endDateInput, refreshButton, exportButton];
        criticalElements.forEach(el => {
            if (!el) allElementsFound = false;
        });
        if (sidebarLinks.length === 0) allElementsFound = false;
        if (viewContainers.length === 0) allElementsFound = false;

        if (!allElementsFound) {
            console.error("[Stats] Initialization failed: Could not find critical elements.");
            document.body.innerHTML = '<p class="text-red-500 p-4 text-center">错误：统计页面初始化失败，界面元素缺失。</p>';
            return;
        }
        console.log("[Stats] All critical elements found.");

        initDatePickers();
        setupEventListeners();
        switchView(DEFAULT_VIEW); // 加载默认视图
        console.log("Department Stats Module Initialized.");
    }

    // --- 执行初始化 ---
    init();

}); // 结束 DOMContentLoaded


// **说明:**
//
// 1.  **依赖检查:** 在脚本开头检查 `flatpickr`, `html2canvas`, `AppUtils` 是否已加载。
// 2.  **初始化 (`init`)**:
//     * 获取 DOM 元素引用。
//     * 调用 `initDatePickers` 初始化日期选择器，设置默认值（年初至今），并更新 `currentStartDate`, `currentEndDate` 状态。
//     * 调用 `setupEventListeners` 绑定侧边栏、查询按钮、导出按钮的事件。
//     * 调用 `switchView(DEFAULT_VIEW)` 加载默认视图（部门周报统计）的数据。
// 3.  **视图切换 (`switchView`)**:
//     * 更新侧边栏链接的 `active-view` 类。
//     * 使用 `hidden` 类切换视图容器 (`#department-report-view`, `#employee-detail-view`) 的显示。
//     * 调用 `reloadDataForCurrentView` 加载新视图的数据。
// 4.  **数据加载 (`reloadDataForCurrentView`, `load...`)**:
//     * `reloadDataForCurrentView`: 获取当前选定的日期范围，调用 `loadCommonInfo` 和当前视图对应的数据加载函数。
//     * `loadCommonInfo`: 调用 API 获取公共信息并更新对应 DOM 元素。
//     * `loadDepartmentReportData`: 使用 `Promise.allSettled` **并行**请求部门概览、重点项目、利润中心交叉表三个 API 的数据，并在所有请求完成后调用各自的渲染函数或显示错误。
//     * `loadEmployeeDetailsData`: 调用 API 获取员工明细数据并调用渲染函数。
//     * 所有加载函数都包含显示/隐藏加载状态 (`showTableLoading`/`hideTableLoading`) 和错误处理 (`showTableError`) 的逻辑。
// 5.  **表格渲染 (`render...Table`)**:
//     * **通用:** 清空 `thead`, `tbody`, `tfoot`；处理空数据情况；使用 `formatNumber` 格式化数字。
//     * **`renderDepartmentOverviewTable`:** 渲染部门概览数据，并在 `tfoot` 中计算并渲染汇总行。
//     * **`renderKeyProjectTable`:** **动态生成表头**：先从所有数据中收集不重复的参与部门名称，然后生成包含固定列和动态部门列的 `<thead>`。渲染行数据时，根据动态表头查找并显示对应部门的员工列表（使用 `<br>` 换行）。
//     * **`renderProfitCenterTable`:** **动态生成表头和行:** 接收后端处理好的 `ProfitCenterPivotTable` DTO，直接使用 `departmentHeaders` 生成 `<thead>`，遍历 `rows` 生成 `<tbody>`（根据 `departmentHeaders` 顺序填充工时），使用 `departmentTotals` 生成 `<tfoot>`。
//     * **`renderEmployeeTimesheetTable`:** 渲染员工工时明细表格。
// 6.  **截图导出 (`handleExportScreenshot`)**:
//     * 获取公共信息区和当前可见的视图容器的**克隆**。
//     * 将克隆的元素放入一个临时的、屏幕外的 `div` 中进行截图（避免截取滚动条，并确保内容完整）。
//     * 使用 `html2canvas` 生成 canvas。
//     * 将 canvas 转换为 JPG 数据 URL。
//     * 创建临时链接触发下载。
//     * 移除临时 `div`。
// 7.  **工具函数:** 包含了日期、周数、数字格式化、加载状态处理等辅助函数。
