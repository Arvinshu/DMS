/**
 * 文件路径: src/main/resources/static/js/project/statistics/project_statistics_filters.js
 * 开发时间: 2025-05-10 20:25:05 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 处理项目统计页面的筛选逻辑，包括员工下拉框加载、筛选应用与清空。
 */
'use strict';

// HTML 元素选择器 (确保这些ID与HTML Demo中的ID一致)
const STATS_DATE_RANGE_SELECTOR = '#stat-date-range';
const STATS_EMPLOYEE_FILTER_SELECTOR = '#stat-employee-filter';
const STATS_APPLY_FILTER_BTN_SELECTOR = '#apply-stats-filter-btn';
const STATS_CLEAR_FILTER_BTN_SELECTOR = '#clear-stats-filter-btn';
const STATS_CUSTOM_DATE_PICKER_START_SELECTOR = '#stat-custom-date-start'; // 假设的自定义日期选择器ID
const STATS_CUSTOM_DATE_PICKER_END_SELECTOR = '#stat-custom-date-end';   // 假设的自定义日期选择器ID

// 存储员工列表的缓存，避免重复请求
let cachedAssignees = null;
let flatpickrInstances = {}; // 用于存储Flatpickr实例

/**
 * 初始化筛选器：加载员工列表并绑定事件监听器。
 * 此函数应在 project_statistics_main.js 中被调用。
 */
async function initStatsFilters() {
    await loadAssigneeFilter(); // 加载员工下拉框

    const applyFilterBtn = document.querySelector(STATS_APPLY_FILTER_BTN_SELECTOR);
    const clearFilterBtn = document.querySelector(STATS_CLEAR_FILTER_BTN_SELECTOR);
    const dateRangeSelect = document.querySelector(STATS_DATE_RANGE_SELECTOR);

    if (applyFilterBtn) {
        applyFilterBtn.addEventListener('click', handleApplyFilters);
    } else {
        console.error('未找到应用筛选按钮:', STATS_APPLY_FILTER_BTN_SELECTOR);
    }

    if (clearFilterBtn) {
        clearFilterBtn.addEventListener('click', handleClearFilters);
    } else {
        console.error('未找到清空筛选按钮:', STATS_CLEAR_FILTER_BTN_SELECTOR);
    }

    if (dateRangeSelect) {
        dateRangeSelect.addEventListener('change', handleDateRangeChange);
        // 初始化时也检查一下自定义日期选择器的显隐
        toggleCustomDatePickers(dateRangeSelect.value);
    }

    // 初始化自定义日期选择器 (如果HTML中包含它们并且使用了Flatpickr)
    initCustomDatePickers();

    console.log('项目统计筛选器已初始化。');
}

/**
 * 从API加载任务负责人列表并填充到员工筛选下拉框中。
 */
async function loadAssigneeFilter() {
    const employeeSelect = document.querySelector(STATS_EMPLOYEE_FILTER_SELECTOR);
    if (!employeeSelect) {
        console.error('未找到员工筛选下拉框:', STATS_EMPLOYEE_FILTER_SELECTOR);
        return;
    }

    // 清空现有选项 (保留 "所有员工" 选项)
    while (employeeSelect.options.length > 1) {
        employeeSelect.remove(1);
    }

    try {
        if (!cachedAssignees) { // 如果没有缓存，则从API获取
            showLoadingIndicator(employeeSelect); // 显示加载提示
            cachedAssignees = await getAssignees(); // 调用 project_statistics_api.js 中的函数
        }
        if (cachedAssignees && cachedAssignees.length > 0) {
            cachedAssignees.forEach(assignee => {
                if (assignee) { // 确保负责人名称不为空
                    const option = new Option(assignee, assignee);
                    employeeSelect.add(option);
                }
            });
        } else {
            const option = new Option('暂无负责人数据', '');
            option.disabled = true;
            employeeSelect.add(option);
        }
    } catch (error) {
        console.error('填充员工筛选下拉框失败:', error);
        const option = new Option('加载负责人失败', '');
        option.disabled = true;
        employeeSelect.add(option);
        // AppUtils.showMessage('加载员工列表失败，请稍后重试。', 'error'); // API层已有toast
    } finally {
        hideLoadingIndicator(employeeSelect); // 隐藏加载提示
    }
}

/**
 * 处理日期范围选择变化事件，特别是针对 "custom" 选项。
 * @param {Event} event - change事件对象。
 */
function handleDateRangeChange(event) {
    const selectedValue = event.target.value;
    toggleCustomDatePickers(selectedValue);
}

/**
 * 根据选择的日期范围，显示或隐藏自定义日期选择器。
 * @param {string} selectedDateRangeValue - 日期范围下拉框的当前值。
 */
function toggleCustomDatePickers(selectedDateRangeValue) {
    const customDateStartContainer = document.querySelector(STATS_CUSTOM_DATE_PICKER_START_SELECTOR)?.parentElement; // 假设选择器是input，获取其父容器
    const customDateEndContainer = document.querySelector(STATS_CUSTOM_DATE_PICKER_END_SELECTOR)?.parentElement;

    if (customDateStartContainer && customDateEndContainer) {
        if (selectedDateRangeValue === 'custom') {
            customDateStartContainer.style.display = 'block'; // 或者 'flex', 'grid' 等，取决于布局
            customDateEndContainer.style.display = 'block';
        } else {
            customDateStartContainer.style.display = 'none';
            customDateEndContainer.style.display = 'none';
        }
    }
}

/**
 * 初始化自定义日期选择器 (例如使用 Flatpickr)。
 * 仅当HTML中存在对应的input元素时执行。
 */
function initCustomDatePickers() {
    if (typeof flatpickr !== 'undefined') {
        const startDateInput = document.querySelector(STATS_CUSTOM_DATE_PICKER_START_SELECTOR);
        const endDateInput = document.querySelector(STATS_CUSTOM_DATE_PICKER_END_SELECTOR);

        if (startDateInput) {
            flatpickrInstances.start = flatpickr(startDateInput, {
                dateFormat: "Y-m-d",
                altInput: true,
                altFormat: "Y-m-d",
                locale: "zh", // 需要引入 flatpickr/dist/l10n/zh.js
                onChange: function(selectedDates, dateStr, instance) {
                    if (flatpickrInstances.end && selectedDates[0]) {
                        flatpickrInstances.end.set('minDate', selectedDates[0]);
                    }
                }
            });
            // 初始隐藏
            if(startDateInput.parentElement) startDateInput.parentElement.style.display = 'none';
        }
        if (endDateInput) {
            flatpickrInstances.end = flatpickr(endDateInput, {
                dateFormat: "Y-m-d",
                altInput: true,
                altFormat: "Y-m-d",
                locale: "zh",
                onChange: function(selectedDates, dateStr, instance) {
                    if (flatpickrInstances.start && selectedDates[0]) {
                        flatpickrInstances.start.set('maxDate', selectedDates[0]);
                    }
                }
            });
            if(endDateInput.parentElement) endDateInput.parentElement.style.display = 'none';
        }
    } else {
        console.warn('Flatpickr 未定义。自定义日期选择器将无法使用高级功能。');
    }
}


/**
 * 应用筛选条件的处理函数。
 * 它会收集当前所有筛选器的值，并触发全局的数据加载/刷新函数。
 */
function handleApplyFilters() {
    const filterValues = getStatsFilterValues();
    console.log('应用筛选:', filterValues);
    AppUtils.showMessage(`正在应用筛选条件...`, 'info');

    // 在 project_statistics_main.js 中应该有一个全局函数来处理数据加载
    // 例如: loadAllStatisticsData(filterValues);
    if (typeof loadAllStatisticsData === 'function') {
        loadAllStatisticsData(filterValues);
    } else {
        console.warn('全局函数 loadAllStatisticsData 未定义。无法刷新统计数据。');
        AppUtils.showMessage('筛选功能配置不完整，无法刷新数据。', 'warning');
    }
}

/**
 * 清空所有筛选条件并重置为默认值，然后重新应用筛选。
 */
function handleClearFilters() {
    console.log('清空筛选条件...');
    const dateRangeSelect = document.querySelector(STATS_DATE_RANGE_SELECTOR);
    const employeeSelect = document.querySelector(STATS_EMPLOYEE_FILTER_SELECTOR);

    if (dateRangeSelect) {
        dateRangeSelect.value = 'this_week'; // 默认值
        toggleCustomDatePickers('this_week'); // 隐藏自定义日期选择器
    }
    if (employeeSelect) {
        employeeSelect.value = 'all'; // 默认值 "所有员工"
    }

    // 清空 Flatpickr 实例的值
    if (flatpickrInstances.start) flatpickrInstances.start.clear();
    if (flatpickrInstances.end) flatpickrInstances.end.clear();


    AppUtils.showMessage('筛选条件已清空。', 'info');
    handleApplyFilters(); // 重新加载数据
}

/**
 * 收集并返回当前所有筛选器的值。
 * @returns {object} 包含当前筛选值的对象。
 */
function getStatsFilterValues() {
    const dateRangeSelect = document.querySelector(STATS_DATE_RANGE_SELECTOR);
    const employeeSelect = document.querySelector(STATS_EMPLOYEE_FILTER_SELECTOR);

    let dateRangeValue = dateRangeSelect ? dateRangeSelect.value : 'this_week';
    const employeeValue = employeeSelect ? employeeSelect.value : 'all';

    if (dateRangeValue === 'custom') {
        const startDateInput = document.querySelector(STATS_CUSTOM_DATE_PICKER_START_SELECTOR);
        const endDateInput = document.querySelector(STATS_CUSTOM_DATE_PICKER_END_SELECTOR);
        const customStart = startDateInput ? startDateInput.value : '';
        const customEnd = endDateInput ? endDateInput.value : '';

        if (customStart && customEnd) {
            // 校验日期顺序 (可选，Flatpickr已处理部分)
            if (new Date(customStart) > new Date(customEnd)) {
                AppUtils.showMessage('自定义日期范围中，开始日期不能晚于结束日期。', 'warning');
                // 可以选择不更新 dateRangeValue 或使用一个默认值
            } else {
                dateRangeValue = `custom_${customStart}_${customEnd}`;
            }
        } else {
            AppUtils.showMessage('选择了自定义日期范围，但未指定开始或结束日期。将使用默认范围。', 'warning');
            dateRangeValue = 'this_week'; // 或者保持 'custom' 让后端处理/报错
        }
    }

    return {
        dateRange: dateRangeValue,
        employee: employeeValue
        // 未来可以添加更多筛选条件，如项目类型、标签等
    };
}


/**
 * 辅助函数：显示加载指示器 (示例)。
 * @param {HTMLElement} element - 要在其上显示加载指示器的元素或其容器。
 */
function showLoadingIndicator(element) {
    // 实际项目中，这里可能会添加一个旋转的spinner或修改元素样式
    if (element && element.form) { // 如果是表单元素，可以在其旁边显示
        //  element.form.classList.add('loading');
    }
    console.log('显示加载指示器 for', element);
}

/**
 * 辅助函数：隐藏加载指示器 (示例)。
 * @param {HTMLElement} element - 从其上移除加载指示器的元素或其容器。
 */
function hideLoadingIndicator(element) {
    if (element && element.form) {
        // element.form.classList.remove('loading');
    }
    console.log('隐藏加载指示器 for', element);
}

// 注意: 此文件定义了函数，但通常不会直接执行顶层代码（除非是事件绑定）。
// initStatsFilters() 函数应由 project_statistics_main.js 在文档加载完毕后调用。
// 例如:
// document.addEventListener('DOMContentLoaded', function() {
//     if (document.getElementById('project-statistics-view')) { // 确保在统计页面
//         initStatsFilters();
//     }
// });
