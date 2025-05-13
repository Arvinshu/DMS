/**
 * 文件路径: src/main/resources/static/js/project/statistics/project_statistics_main.js
 * 开发时间: 2025-05-12 00:30:00 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 项目统计页面的主JavaScript文件。
 * 更新 (2025-05-13):
 * - 修改 show/hideLoading 的目标元素为统计视图本身，解决全局遮罩不消失问题。
 * - 移除 500ms 检查逻辑。
 * - 添加初始化标志位防止重复执行，优化图表实例管理。
 */
'use strict';

// 全局变量，用于存储图表实例，方便更新
let projectStatusPieChartInstance = null; // 重命名以区分 charts.js 中的内部变量
let employeeLoadBarChartInstance = null;
let projectTypePieChartInstance = null;

// 当前项目构成分析的维度 (用于项目类型分布图)
let currentProjectTypeDimension = 'businessTypeName'; // 默认按业务类型

let statisticsModuleInitialized = false; // 初始化标志位
let statisticsDataLoadedOnce = false; // 标记初始数据是否已加载

/**
 * 统计模块的入口函数。
 * 由 project_main.js 在视图切换到统计页面时调用。
 * 或者在 DOMContentLoaded 时，如果统计页面是默认视图。
 */
async function initializeOrRefreshStatisticsModule() {
    const projectStatisticsView = document.getElementById('project-statistics-view');
    if (!projectStatisticsView) {
        console.warn('[STATS_MAIN] project-statistics-view 元素未找到，无法初始化或刷新统计模块。');
        return;
    }

    // 检查依赖
    if (typeof Chart === 'undefined') {
        console.error('[STATS_MAIN] Chart.js 未加载。统计图表将无法显示。');
        if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
            AppUtils.showMessage('图表库加载失败，部分功能可能无法使用。', 'error');
        }
        // 即使图表库失败，筛选器和部分数据加载逻辑仍可尝试执行
    }
    if (typeof flatpickr === 'undefined') {
        console.warn('[STATS_MAIN] Flatpickr 未加载。自定义日期选择器将不可用。');
    }

    if (!statisticsModuleInitialized) {
        console.log('[STATS_MAIN] 统计模块首次初始化...');

        // 初始化筛选器 (只执行一次)
        if (typeof initStatsFilters === 'function') {
            console.log('[STATS_MAIN] 调用 initStatsFilters...');
            await initStatsFilters();
            console.log('[STATS_MAIN] initStatsFilters 调用完成。');
        } else {
            console.error('[STATS_MAIN] initStatsFilters 函数未定义。');
            return; // 关键依赖缺失
        }

        // 初始化图表 (只执行一次)
        if (typeof initAllCharts === 'function') {
            console.log('[STATS_MAIN] 调用 initAllCharts...');
            const chartInstances = initAllCharts( // 传递现有实例以便销毁（如果 charts.js 需要）
                projectStatusPieChartInstance,
                employeeLoadBarChartInstance,
                projectTypePieChartInstance
            );
            projectStatusPieChartInstance = chartInstances.projectStatusPieChart;
            employeeLoadBarChartInstance = chartInstances.employeeLoadBarChart;
            projectTypePieChartInstance = chartInstances.projectTypePieChart;
            console.log('[STATS_MAIN] initAllCharts 调用完成。');
        } else {
            console.error('[STATS_MAIN] initAllCharts 函数未定义。');
        }

        // 设置图表切换按钮 (只执行一次)
        if (typeof setupProjectTypeToggleButtons === 'function') {
            setupProjectTypeToggleButtons(handleProjectTypeDimensionChange);
        } else {
            console.warn('[STATS_MAIN] setupProjectTypeToggleButtons 函数未定义。');
        }

        statisticsModuleInitialized = true;
    } else {
        console.log('[STATS_MAIN] 统计模块已初始化，将刷新数据。');
    }

    // 无论是否首次初始化，都加载/刷新数据
    // 但首次加载数据应该在所有UI元素（如图表canvas）准备好之后
    if (typeof getStatsFilterValues === 'function') {
        const currentFilters = getStatsFilterValues();
        if (currentFilters) {
            console.log('[STATS_MAIN] 准备加载/刷新统计数据...');
            await loadAllStatisticsData(currentFilters);
            statisticsDataLoadedOnce = true;
        } else {
            console.error("[STATS_MAIN] 无法获取筛选条件，数据加载/刷新中止。");
            if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
                AppUtils.showMessage('无法加载筛选条件，页面可能无法正常显示数据。', 'error');
            }
        }
    } else {
        console.error("[STATS_MAIN] getStatsFilterValues 函数未定义。");
    }
    console.log('[STATS_MAIN] initializeOrRefreshStatisticsModule 完成。');
}


document.addEventListener('DOMContentLoaded', async function () {
    // 这个监听器主要用于页面首次加载时，如果统计视图是默认显示的，则触发初始化。
    // 如果统计视图是通过点击tab切换的，project_main.js 中的视图切换逻辑应该调用 initializeOrRefreshStatisticsModule()。
    const projectStatisticsView = document.getElementById('project-statistics-view');
    if (projectStatisticsView && projectStatisticsView.classList.contains('active')) { // 假设 active 类表示当前视图
        console.log('[STATS_MAIN] DOMContentLoaded: 统计视图是活动视图，开始初始化...');
        await initializeOrRefreshStatisticsModule();
    } else {
        console.log('[STATS_MAIN] DOMContentLoaded: 统计视图非活动，等待切换。');
    }
});

async function handleProjectTypeDimensionChange(newDimension) {
    if (currentProjectTypeDimension !== newDimension) {
        currentProjectTypeDimension = newDimension;
        console.log(`[STATS_MAIN] 项目类型分布图维度已切换为: ${currentProjectTypeDimension}`);
        const currentFilters = getStatsFilterValues();

        const chartCanvas = document.getElementById(PROJECT_TYPE_CHART_ID);
        const chartContainer = chartCanvas ? chartCanvas.parentElement : null;

        if (chartContainer && typeof AppUtils !== 'undefined' && AppUtils.showLoading) AppUtils.showLoading(chartContainer);

        try {
            const overviewData = await getDepartmentOverviewStats(currentFilters.dateRange, currentProjectTypeDimension);
            if (overviewData && overviewData.projectTypeDistribution) {
                if (typeof updateProjectTypePieChart === 'function') {
                    updateProjectTypePieChart(projectTypePieChartInstance, overviewData.projectTypeDistribution, currentProjectTypeDimension);
                }
            }
        } catch (error) {
            console.error(`[STATS_MAIN] 切换维度并重新加载项目类型分布图失败: `, error);
            if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
                AppUtils.showMessage(`加载项目类型分布图 (${currentProjectTypeDimension}) 失败`, 'error');
            }
        } finally {
            if (chartContainer && typeof AppUtils !== 'undefined' && AppUtils.hideLoading) AppUtils.hideLoading(chartContainer);
        }
    }
}

async function loadAllStatisticsData(filterValues) {
    if (!filterValues) {
        console.warn('[STATS_MAIN] loadAllStatisticsData 调用时未提供筛选条件，将尝试重新获取。');
        if (typeof getStatsFilterValues !== 'function') {
            console.error('[STATS_MAIN] getStatsFilterValues 函数未定义，无法继续加载数据。');
            if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
                AppUtils.showMessage('筛选功能未正确初始化，无法加载数据。', 'error');
            }
            return;
        }
        filterValues = getStatsFilterValues();
    }
    console.log('[STATS_MAIN] 开始加载所有统计数据，筛选条件:', filterValues);

    // **修改点：获取统计视图元素**
    const statsView = document.getElementById('project-statistics-view');

    if (statsView && typeof AppUtils !== 'undefined' && AppUtils.showLoading) {
        // **修改点：在统计视图上显示加载**
        console.log('[STATS_MAIN] 调用 AppUtils.showLoading(statsView) 显示统计视图加载...');
        AppUtils.showLoading(statsView);
    }

    const { dateRange, employee: selectedEmployee } = filterValues;
    let employeeDetailsData = null;

    try {
        if (selectedEmployee && selectedEmployee !== 'all') {
            const employeeDetailSection = document.getElementById('employee-details-section');
            if (employeeDetailSection && typeof AppUtils !== 'undefined' && AppUtils.showLoading) AppUtils.showLoading(employeeDetailSection);
            try {
                console.log(`[STATS_MAIN] 准备获取员工 "${selectedEmployee}" 的详情...`);
                employeeDetailsData = await getEmployeeDetails(selectedEmployee, dateRange);
                console.log(`[STATS_MAIN] 员工 "${selectedEmployee}" 详情获取成功。`);
            } catch (error) {
                console.error(`[STATS_MAIN] 加载员工 "${selectedEmployee}" 详情失败:`, error);
            } finally {
                if (employeeDetailSection && typeof AppUtils !== 'undefined' && AppUtils.hideLoading) AppUtils.hideLoading(employeeDetailSection);
            }
        }

        console.log('[STATS_MAIN] 准备并行加载概览、风险和期限数据...');
        const [overviewData, atRiskData, deadlineData] = await Promise.all([
            getDepartmentOverviewStats(dateRange, currentProjectTypeDimension),
            getAtRiskProjects(),
            getTaskDeadlineInfo(7)
        ]).catch(error => {
            console.error('[STATS_MAIN] Promise.all 中至少一个API调用失败:', error);
            return [null, null, null];
        });
        console.log('[STATS_MAIN] 并行数据加载完成。 Overview:', !!overviewData, 'AtRisk:', !!atRiskData, 'Deadline:', !!deadlineData);

        console.log('[STATS_MAIN] 开始更新UI...');
        if (overviewData) {
            if (typeof updateDepartmentKpiCards === 'function' && overviewData.kpis) {
                updateDepartmentKpiCards(overviewData.kpis, dateRange);
            }
            if (typeof updateProjectStatusPieChart === 'function' && overviewData.projectStatusDistribution) {
                updateProjectStatusPieChart(projectStatusPieChartInstance, overviewData.projectStatusDistribution);
            }
            if (typeof updateEmployeeLoadBarChart === 'function' && overviewData.employeeLoad) {
                updateEmployeeLoadBarChart(employeeLoadBarChartInstance, overviewData.employeeLoad);
            }
            if (typeof updateProjectTypePieChart === 'function' && overviewData.projectTypeDistribution) {
                updateProjectTypePieChart(projectTypePieChartInstance, overviewData.projectTypeDistribution, currentProjectTypeDimension);
            }
        } else {
            console.warn("[STATS_MAIN] 部门概览数据(overviewData)为空，相关UI部分可能未更新。");
        }

        if (atRiskData && typeof renderAtRiskProjectsTable === 'function') {
            renderAtRiskProjectsTable(atRiskData);
        } else if (atRiskData === null && typeof renderAtRiskProjectsTable === 'function') {
            renderAtRiskProjectsTable([]);
        }

        if (deadlineData && typeof renderTaskDeadlineLists === 'function') {
            renderTaskDeadlineLists(deadlineData.upcomingTasks, deadlineData.overdueTasks);
        } else if (deadlineData === null && typeof renderTaskDeadlineLists === 'function') {
            renderTaskDeadlineLists([], []);
        }

        if (selectedEmployee && selectedEmployee !== 'all') {
            if (typeof displayEmployeeDetails === 'function') {
                if (employeeDetailsData) {
                    displayEmployeeDetails(selectedEmployee, employeeDetailsData);
                } else {
                    displayEmployeeDetails(selectedEmployee, { kpis: {}, tasksByProject: [] });
                    if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
                        AppUtils.showMessage(`未能成功加载员工 ${selectedEmployee} 的详细数据。`, 'warning');
                    }
                }
            }
        } else {
            if (typeof clearEmployeeDetails === 'function') {
                clearEmployeeDetails();
            }
        }
        console.log('[STATS_MAIN] UI更新完成。');

        if (overviewData || atRiskData || deadlineData || employeeDetailsData) {
            if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
                // AppUtils.showMessage('统计数据已加载并更新。', 'success'); // 避免过于频繁的成功提示
            }
        }

    } catch (error) {
        console.error('[STATS_MAIN] loadAllStatisticsData 主逻辑发生错误:', error);
        if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
            AppUtils.showMessage('加载统计数据过程中发生意外错误。', 'error');
        }
    } finally {
        if (statsView && typeof AppUtils !== 'undefined' && AppUtils.hideLoading) {
            // **修改点：在统计视图上隐藏加载**
            console.log('[STATS_MAIN] 进入 loadAllStatisticsData 的 finally 块，准备移除统计视图加载指示器...');
            AppUtils.hideLoading(statsView);
            console.log('[STATS_MAIN] AppUtils.hideLoading(statsView) 已调用。');

            // **修改点：移除 500ms 检查逻辑**
            // setTimeout(() => { ... }, 500); // 移除这部分代码
        }
        console.log('[STATS_MAIN] loadAllStatisticsData finally block executed.');
    }
}
