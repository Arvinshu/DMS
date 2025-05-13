/**
 * 文件路径: src/main/resources/static/js/project/statistics/project_statistics_charts.js
 * 开发时间: 2025-05-12 00:30:05 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 负责使用Chart.js初始化和更新项目统计页面中的所有图表。
 * 更新: 确保在初始化新图表前销毁旧实例。
 */
'use strict';

// Chart.js实例存储，由 main.js 传入并管理
// let charts = {
//     projectStatusPie: null,
//     employeeLoadBar: null,
//     projectTypePie: null
// };
// 改为直接在 init 函数中接收和返回实例

// 图表元素ID
const PROJECT_STATUS_CHART_ID = 'project-status-pie-chart';
const EMPLOYEE_LOAD_CHART_ID = 'employee-load-bar-chart';
const PROJECT_TYPE_CHART_ID = 'project-type-pie-chart';

// 项目类型分布图切换按钮ID
const TOGGLE_TYPE_BUSINESS_BTN_ID = 'toggle-type-business';
const TOGGLE_TYPE_PROFIT_BTN_ID = 'toggle-type-profit';


/**
 * 初始化页面上的所有图表。
 * @param {Chart} existingStatusChart - 已存在的项目状态图表实例 (可选, 用于销毁)。
 * @param {Chart} existingLoadChart - 已存在的员工负载图表实例 (可选, 用于销毁)。
 * @param {Chart} existingTypeChart - 已存在的项目类型图表实例 (可选, 用于销毁)。
 * @returns {object} 包含新创建图表实例的对象。
 */
function initAllCharts(existingStatusChart, existingLoadChart, existingTypeChart) {
    console.log('[CHARTS] initAllCharts called.');
    const newCharts = {};

    if (existingStatusChart && typeof existingStatusChart.destroy === 'function') {
        console.log('[CHARTS] Destroying existing project status chart.');
        existingStatusChart.destroy();
    }
    newCharts.projectStatusPieChart = initPieChart(PROJECT_STATUS_CHART_ID, '项目状态分布', [], []);

    if (existingLoadChart && typeof existingLoadChart.destroy === 'function') {
        console.log('[CHARTS] Destroying existing employee load chart.');
        existingLoadChart.destroy();
    }
    newCharts.employeeLoadBarChart = initBarChart(EMPLOYEE_LOAD_CHART_ID, '员工任务负载', [], {
        xLabel: '员工',
        yLabel: '任务数'
    }, true);

    if (existingTypeChart && typeof existingTypeChart.destroy === 'function') {
        console.log('[CHARTS] Destroying existing project type chart.');
        existingTypeChart.destroy();
    }
    newCharts.projectTypePieChart = initPieChart(PROJECT_TYPE_CHART_ID, '项目构成分析 (按业务类型)', [], []);

    console.log('[CHARTS] All charts initialized/re-initialized.');
    return newCharts;
}

/**
 * 通用饼图/环形图初始化函数。
 * @param {string} canvasId - canvas元素的ID。
 * @param {string} chartTitle - 图表标题。
 * @param {Array<string>} labels - 初始标签数组。
 * @param {Array<number>} data - 初始数据数组。
 * @returns {Chart|null} Chart.js实例或null（如果canvas未找到或Chart未定义）。
 */
function initPieChart(canvasId, chartTitle, labels = [], data = []) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) {
        console.error(`[CHARTS] Canvas ID "${canvasId}" 未找到。`);
        return null;
    }
    if (typeof Chart === 'undefined') {
        console.error('[CHARTS] Chart.js 未定义。');
        return null;
    }

    // 注意：Chart.js 在创建新图表时，如果canvas上已有图表，它会自动处理旧图表的销毁。
    // 但为了更明确地控制，我们可以在调用此函数前，由调用者（如initAllCharts）来销毁旧实例。
    // 或者，如果这个函数可能被独立调用，可以在这里检查并销毁：
    // const existingChart = Chart.getChart(canvasId);
    // if (existingChart) {
    //     existingChart.destroy();
    // }

    console.log(`[CHARTS] Initializing Pie Chart on canvas: ${canvasId}`);
    return new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels.length > 0 ? labels : ['暂无数据'],
            datasets: [{
                label: chartTitle,
                data: data.length > 0 ? data : [1],
                backgroundColor: getDefaultChartColors(data.length || 1),
                borderColor: '#fff',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                },
                title: {
                    display: true,
                    text: chartTitle,
                    font: { size: 16 }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.parsed !== null && !isNaN(context.parsed)) {
                                label += context.parsed;
                            } else if (Array.isArray(context.raw) && context.raw.length > context.dataIndex && !isNaN(context.raw[context.dataIndex])) {
                                label += context.raw[context.dataIndex]; // 尝试从原始数据获取
                            } else {
                                label += 'N/A';
                            }
                            return label;
                        }
                    }
                }
            }
        }
    });
}

/**
 * 通用条形图初始化函数。
 * @param {string} canvasId - canvas元素的ID。
 * @param {string} chartTitle - 图表标题。
 * @param {Array<object>} initialDatasets - 初始数据集数组 (Chart.js格式)。
 * @param {object} axisLabels - 包含xLabel和yLabel的对象。
 * @param {boolean} stacked - 是否为堆叠条形图。
 * @returns {Chart|null} Chart.js实例或null。
 */
function initBarChart(canvasId, chartTitle, initialDatasets = [], axisLabels = { xLabel: 'X轴', yLabel: 'Y轴' }, stacked = false) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) {
        console.error(`[CHARTS] Canvas ID "${canvasId}" 未找到。`);
        return null;
    }
    if (typeof Chart === 'undefined') {
        console.error('[CHARTS] Chart.js 未定义。');
        return null;
    }

    console.log(`[CHARTS] Initializing Bar Chart on canvas: ${canvasId}`);
    return new Chart(ctx, {
        type: 'bar',
        data: {
            labels: [],
            datasets: initialDatasets.length > 0 ? initialDatasets : [{ label: '暂无数据', data: [], backgroundColor: '#cccccc' }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    title: { display: true, text: axisLabels.xLabel },
                    stacked: stacked,
                },
                y: {
                    title: { display: true, text: axisLabels.yLabel },
                    stacked: stacked,
                    beginAtZero: true
                }
            },
            plugins: {
                legend: {
                    position: 'top',
                },
                title: {
                    display: true,
                    text: chartTitle,
                    font: { size: 16 }
                }
            }
        }
    });
}

function updateProjectStatusPieChart(chartInstance, dataPoints) {
    if (!chartInstance || !dataPoints) {
        console.warn('[CHARTS] updateProjectStatusPieChart: 无效的图表实例或数据。');
        return;
    }
    console.log('[CHARTS] Updating Project Status Pie Chart with data:', dataPoints);
    if (dataPoints.length === 0) {
        chartInstance.data.labels = ['暂无数据'];
        chartInstance.data.datasets[0].data = [1]; // Chart.js 需要至少一个数据点来渲染
        chartInstance.data.datasets[0].backgroundColor = ['#cccccc'];
    } else {
        chartInstance.data.labels = dataPoints.map(p => p.label);
        chartInstance.data.datasets[0].data = dataPoints.map(p => p.value);
        chartInstance.data.datasets[0].backgroundColor = getDefaultChartColors(dataPoints.length);
    }
    chartInstance.update();
}

function updateEmployeeLoadBarChart(chartInstance, employeeLoadData) {
    if (!chartInstance || !employeeLoadData) {
        console.warn('[CHARTS] updateEmployeeLoadBarChart: 无效的图表实例或数据。');
        return;
    }
    console.log('[CHARTS] Updating Employee Load Bar Chart with data:', employeeLoadData);
    if (employeeLoadData.length === 0) {
        chartInstance.data.labels = ['暂无数据'];
        chartInstance.data.datasets = [
            { label: '进行中任务', data: [], backgroundColor: '#a0aec0' },
            { label: '待办任务', data: [], backgroundColor: '#cbd5e0' }
        ];
    } else {
        chartInstance.data.labels = employeeLoadData.map(e => e.employeeName);
        chartInstance.data.datasets = [
            {
                label: '进行中任务',
                data: employeeLoadData.map(e => e.inProgressTaskCount || 0),
                backgroundColor: '#4299e1',
            },
            {
                label: '待办任务',
                data: employeeLoadData.map(e => e.todoTaskCount || 0),
                backgroundColor: '#f6ad55',
            }
        ];
    }
    chartInstance.update();
}

function updateProjectTypePieChart(chartInstance, dataPoints, dimensionName) {
    if (!chartInstance || !dataPoints) {
        console.warn('[CHARTS] updateProjectTypePieChart: 无效的图表实例或数据。');
        return;
    }
    console.log('[CHARTS] Updating Project Type Pie Chart with data:', dataPoints, 'Dimension:', dimensionName);

    let title = '项目构成分析';
    const chartTitleEl = document.getElementById('project-type-chart-title'); // 获取标题元素

    if (dimensionName === 'businessTypeName') {
        title += ' (按业务类型)';
    } else if (dimensionName === 'profitCenterZone') {
        title += ' (按利润中心)';
    }

    if (chartTitleEl) { // 更新HTML中的标题元素
        chartTitleEl.textContent = title;
    } else { // 如果HTML中没有特定标题元素，则更新Chart.js的options
        chartInstance.options.plugins.title.text = title;
    }


    if (dataPoints.length === 0) {
        chartInstance.data.labels = ['暂无数据'];
        chartInstance.data.datasets[0].data = [1];
        chartInstance.data.datasets[0].backgroundColor = ['#cccccc'];
    } else {
        chartInstance.data.labels = dataPoints.map(p => p.label || '未知类型');
        chartInstance.data.datasets[0].data = dataPoints.map(p => p.value);
        chartInstance.data.datasets[0].backgroundColor = getDefaultChartColors(dataPoints.length);
    }
    chartInstance.update();
}

function setupProjectTypeToggleButtons(callback) {
    const businessBtn = document.getElementById(TOGGLE_TYPE_BUSINESS_BTN_ID);
    const profitBtn = document.getElementById(TOGGLE_TYPE_PROFIT_BTN_ID);

    function handleToggle(event, newDimension) {
        // 防止重复调用
        if (event.currentTarget.classList.contains('active')) {
            return;
        }
        if (typeof callback === 'function') {
            callback(newDimension); // 这个callback是 project_statistics_main.js 中的 handleProjectTypeDimensionChange
        }
        // 更新按钮激活状态
        if(businessBtn) businessBtn.classList.toggle('active', newDimension === 'businessTypeName');
        if(profitBtn) profitBtn.classList.toggle('active', newDimension === 'profitCenterZone');
    }

    if (businessBtn) {
        businessBtn.addEventListener('click', (e) => handleToggle(e, 'businessTypeName'));
    }
    if (profitBtn) {
        profitBtn.addEventListener('click', (e) => handleToggle(e, 'profitCenterZone'));
    }
}

function getDefaultChartColors(count) {
    const defaultColors = [
        '#4299e1', '#f6ad55', '#68d391', '#f56565', '#b794f4',
        '#4fd1c5', '#ed8936', '#a0aec0', '#fc8181', '#d53f8c',
        '#ecc94b', '#38b2ac', '#9f7aea', '#ed64a6', '#63b3ed'
    ];
    if (count <= 0) return ['#cccccc']; // 处理空数据情况
    if (count <= defaultColors.length) {
        return defaultColors.slice(0, count);
    }
    const colors = [];
    for (let i = 0; i < count; i++) {
        colors.push(defaultColors[i % defaultColors.length]);
    }
    return colors;
}