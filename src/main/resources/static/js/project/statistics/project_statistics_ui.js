/**
 * 文件路径: src/main/resources/static/js/project/statistics/project_statistics_ui.js
 * 开发时间: 2025-05-10 20:45:05 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 负责将从API获取的统计数据渲染到HTML页面的各个部分。
 * 包括更新KPI卡片、表格、列表等。
 */
'use strict';

// HTML 元素 ID 常量 (确保与HTML Demo中的ID一致)
const DEPT_ACTIVE_PROJECTS_ID = 'dept-active-projects';
const DEPT_INPROGRESS_TASKS_ID = 'dept-inprogress-tasks';
const DEPT_TOTAL_OVERDUE_TASKS_ID = 'dept-total-overdue-tasks';
const DEPT_COMPLETED_TASKS_PERIOD_ID = 'dept-completed-tasks-period';
const DEPT_COMPLETED_TASKS_PERIOD_LABEL_ID = 'dept-completed-tasks-period-label'; // KPI卡片中“本期间完成任务”的标题部分，如果需要动态文本

const AT_RISK_PROJECTS_TBODY_ID = 'at-risk-projects-tbody';
const NO_AT_RISK_PROJECTS_ID = 'no-at-risk-projects';

const UPCOMING_DEADLINES_LIST_ID = 'upcoming-deadlines-list';
const NO_UPCOMING_TASKS_ID = 'no-upcoming-tasks';
const OVERDUE_TASKS_LIST_ID = 'overdue-tasks-list';
const NO_OVERDUE_TASKS_ID = 'no-overdue-tasks';

const EMPLOYEE_KPI_CARDS_ID = 'employee-kpi-cards';
const EMP_TODO_TASKS_ID = 'emp-todo-tasks';
const EMP_INPROGRESS_TASKS_ID = 'emp-inprogress-tasks';
const EMP_OVERDUE_TASKS_ID = 'emp-overdue-tasks';
const DISPLAY_EMPLOYEE_NAME_ID = 'display-employee-name'; // 用于显示当前筛选的员工名
const EMPLOYEE_TASK_LIST_CONTAINER_ID = 'employee-tasks-by-project';
const EMPLOYEE_TASK_PLACEHOLDER_ID = 'employee-task-placeholder';


/**
 * 更新部门整体概览的KPI卡片。
 * @param {object} kpiData - 包含KPI数据的对象 (来自API响应中的 kpis 部分)。
 * @param {string} dateRangeString - 当前的日期范围字符串，用于动态更新“本期间”文本。
 */
function updateDepartmentKpiCards(kpiData, dateRangeString) {
    const activeProjectsEl = document.getElementById(DEPT_ACTIVE_PROJECTS_ID);
    const inProgressTasksEl = document.getElementById(DEPT_INPROGRESS_TASKS_ID);
    const totalOverdueTasksEl = document.getElementById(DEPT_TOTAL_OVERDUE_TASKS_ID);
    const completedTasksPeriodEl = document.getElementById(DEPT_COMPLETED_TASKS_PERIOD_ID);
    // const completedTasksLabelEl = document.getElementById(DEPT_COMPLETED_TASKS_PERIOD_LABEL_ID); // 如果标题也需要更新

    if (activeProjectsEl) activeProjectsEl.textContent = kpiData.activeProjects !== undefined ? kpiData.activeProjects : 'N/A';
    if (inProgressTasksEl) inProgressTasksEl.textContent = kpiData.inProgressTasks !== undefined ? kpiData.inProgressTasks : 'N/A';
    if (totalOverdueTasksEl) totalOverdueTasksEl.textContent = kpiData.totalOverdueTasks !== undefined ? kpiData.totalOverdueTasks : 'N/A';
    if (completedTasksPeriodEl) completedTasksPeriodEl.textContent = kpiData.completedTasksThisPeriod !== undefined ? kpiData.completedTasksThisPeriod : 'N/A';

    // 更新“本期间完成任务”的标签文本
    const periodText = getDateRangeText(dateRangeString);
    const completedTasksCardTitle = document.querySelector(`#${DEPT_COMPLETED_TASKS_PERIOD_ID}`)?.closest('.stat-card')?.querySelector('h4');
    if (completedTasksCardTitle) {
        completedTasksCardTitle.textContent = `${periodText}完成任务`;
    }
}

/**
 * 根据日期范围字符串返回用户友好的文本。
 * @param {string} dateRangeString - 日期范围字符串。
 * @returns {string} - 例如 "本周", "本月", "自定义期间"。
 */
function getDateRangeText(dateRangeString) {
    if (!dateRangeString) return "本期间";
    if (dateRangeString.startsWith('custom_')) return "自定义期间";
    switch (dateRangeString) {
        case 'this_week': return "本周";
        case 'last_week': return "上周";
        case 'this_month': return "本月";
        case 'last_month': return "上月";
        case 'this_quarter': return "本季度";
        case 'last_quarter': return "上季度";
        default: return "本期间";
    }
}


/**
 * 渲染风险项目列表表格。
 * @param {Array<object>} projects - 风险项目数据数组。
 */
function renderAtRiskProjectsTable(projects) {
    const tbody = document.getElementById(AT_RISK_PROJECTS_TBODY_ID);
    const noDataEl = document.getElementById(NO_AT_RISK_PROJECTS_ID);

    if (!tbody || !noDataEl) {
        console.error('风险项目表格或无数据提示元素未找到。');
        return;
    }
    tbody.innerHTML = ''; // 清空现有行

    if (!projects || projects.length === 0) {
        noDataEl.style.display = 'block';
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-gray-500 py-4">暂无明显风险项目。</td></tr>`;
        return;
    }

    noDataEl.style.display = 'none';
    projects.forEach(project => {
        const row = tbody.insertRow();
        row.innerHTML = `
            <td class="px-3 py-2 whitespace-nowrap text-sm text-gray-700">${AppUtils.escapeHTML(project.projectName) || 'N/A'}</td>
            <td class="px-3 py-2 whitespace-nowrap text-sm text-gray-700">${AppUtils.escapeHTML(project.projectManager) || 'N/A'}</td>
            <td class="px-3 py-2 whitespace-nowrap text-sm text-center font-semibold ${getRiskLevelColorClass(project.riskLevel)}">${AppUtils.escapeHTML(project.riskLevel) || 'N/A'}</td>
            <td class="px-3 py-2 whitespace-nowrap text-sm text-gray-700 text-center">${project.overdueTaskCount !== undefined ? project.overdueTaskCount : 'N/A'}</td>
            <td class="px-3 py-2 text-sm text-gray-700">${AppUtils.escapeHTML(project.riskDescription) || 'N/A'}</td>
            <td class="px-3 py-2 whitespace-nowrap text-sm text-center">
                <button class="btn btn-xs btn-view-details" data-project-id="${project.projectId}" onclick="viewProjectDetails(${project.projectId})">查看</button>
            </td>
        `;
    });
}

/**
 * 根据风险等级返回对应的文本颜色类名。
 * @param {string} riskLevel - 风险等级字符串。
 * @returns {string} - Tailwind CSS颜色类名。
 */
function getRiskLevelColorClass(riskLevel) {
    if (!riskLevel) return 'text-gray-700';
    switch (riskLevel.toLowerCase()) {
        case '高': return 'text-red-500';
        case '中': return 'text-yellow-500';
        case '低': return 'text-blue-500'; // 或者其他颜色
        default: return 'text-gray-700';
    }
}

/**
 * 查看项目详情的占位符函数。
 * @param {number} projectId - 项目ID。
 */
function viewProjectDetails(projectId) {
    // TODO: 实现跳转到项目详情页或打开模态框的逻辑
    // 例如: window.location.href = `${CONTEXT_PATH}/project/detail/${projectId}`;
    // 或者调用 project_main.js 中的函数打开项目详情模态框
    AppUtils.showMessage(`查看项目详情: ${projectId} (功能待实现)`, 'info');
}


/**
 * 渲染即将到期和已逾期的任务列表。
 * @param {Array<object>} upcomingTasks - 即将到期的任务数据数组。
 * @param {Array<object>} overdueTasks - 已逾期的任务数据数组。
 */
function renderTaskDeadlineLists(upcomingTasks, overdueTasks) {
    const upcomingListEl = document.getElementById(UPCOMING_DEADLINES_LIST_ID);
    const noUpcomingEl = document.getElementById(NO_UPCOMING_TASKS_ID);
    const overdueListEl = document.getElementById(OVERDUE_TASKS_LIST_ID);
    const noOverdueEl = document.getElementById(NO_OVERDUE_TASKS_ID);

    // 渲染即将到期任务
    if (upcomingListEl && noUpcomingEl) {
        upcomingListEl.innerHTML = ''; // 清空
        if (!upcomingTasks || upcomingTasks.length === 0) {
            noUpcomingEl.style.display = 'block';
            upcomingListEl.innerHTML = `<p class="text-center text-gray-500 py-4">未来7天内无即将到期的任务。</p>`;
        } else {
            noUpcomingEl.style.display = 'none';
            upcomingTasks.forEach(task => {
                const item = document.createElement('div');
                item.className = 'task-item py-2 border-b border-gray-200 last:border-b-0';
                item.innerHTML = `
                    <p class="text-sm font-medium text-gray-800 truncate" title="${AppUtils.escapeHTML(task.taskName)}">${AppUtils.escapeHTML(task.taskName)} (${AppUtils.escapeHTML(task.projectName)})</p>
                    <p class="text-xs text-gray-500">负责人: ${AppUtils.escapeHTML(task.assignee) || 'N/A'} - 截止: ${task.dueDate || 'N/A'} ${task.priority ? `(优先级: ${AppUtils.escapeHTML(task.priority)})` : ''}</p>
                `;
                upcomingListEl.appendChild(item);
            });
        }
    }

    // 渲染已逾期任务
    if (overdueListEl && noOverdueEl) {
        overdueListEl.innerHTML = ''; // 清空
        if (!overdueTasks || overdueTasks.length === 0) {
            noOverdueEl.style.display = 'block';
            overdueListEl.innerHTML = `<p class="text-center text-gray-500 py-4">暂无已逾期的任务。</p>`;
        } else {
            noOverdueEl.style.display = 'none';
            overdueTasks.forEach(task => {
                const item = document.createElement('div');
                item.className = 'task-item py-2 border-b border-gray-200 last:border-b-0';
                item.innerHTML = `
                    <p class="text-sm font-medium text-red-700 truncate" title="${AppUtils.escapeHTML(task.taskName)}">${AppUtils.escapeHTML(task.taskName)} (${AppUtils.escapeHTML(task.projectName)})</p>
                    <p class="text-xs text-gray-500">负责人: ${AppUtils.escapeHTML(task.assignee) || 'N/A'} - 原定截止: ${task.dueDate || 'N/A'} (已逾期 ${task.overdueDays || 0}天)</p>
                `;
                overdueListEl.appendChild(item);
            });
        }
    }
}

/**
 * 显示选定员工的KPI和任务列表。
 * @param {string} employeeName - 选定的员工姓名。
 * @param {object} employeeData - 包含员工KPI和任务列表的对象。
 */
function displayEmployeeDetails(employeeName, employeeData) {
    const kpiCardsContainer = document.getElementById(EMPLOYEE_KPI_CARDS_ID);
    const empTodoEl = document.getElementById(EMP_TODO_TASKS_ID);
    const empInProgressEl = document.getElementById(EMP_INPROGRESS_TASKS_ID);
    const empOverdueEl = document.getElementById(EMP_OVERDUE_TASKS_ID);
    const displayEmployeeNameEl = document.getElementById(DISPLAY_EMPLOYEE_NAME_ID);
    const taskListContainer = document.getElementById(EMPLOYEE_TASK_LIST_CONTAINER_ID);
    const taskPlaceholderEl = document.getElementById(EMPLOYEE_TASK_PLACEHOLDER_ID);

    if (displayEmployeeNameEl) displayEmployeeNameEl.textContent = AppUtils.escapeHTML(employeeName);

    if (kpiCardsContainer && employeeData.kpis) {
        if (empTodoEl) empTodoEl.textContent = employeeData.kpis.employeeTodoTasks !== undefined ? employeeData.kpis.employeeTodoTasks : 'N/A';
        if (empInProgressEl) empInProgressEl.textContent = employeeData.kpis.employeeInProgressTasks !== undefined ? employeeData.kpis.employeeInProgressTasks : 'N/A';
        if (empOverdueEl) empOverdueEl.textContent = employeeData.kpis.employeeOverdueTasks !== undefined ? employeeData.kpis.employeeOverdueTasks : 'N/A';
        kpiCardsContainer.style.display = 'grid'; // 或者 'flex' 等，根据布局
    }

    if (taskListContainer && taskPlaceholderEl) {
        taskListContainer.innerHTML = ''; // 清空
        if (!employeeData.tasksByProject || employeeData.tasksByProject.length === 0) {
            taskPlaceholderEl.textContent = `员工 ${AppUtils.escapeHTML(employeeName)} 当前没有分配的任务或在此期间无任务记录。`;
            taskPlaceholderEl.style.display = 'block';
        } else {
            taskPlaceholderEl.style.display = 'none';
            employeeData.tasksByProject.forEach(projectGroup => {
                const groupDiv = document.createElement('div');
                groupDiv.className = 'project-task-group mb-4 p-3 bg-gray-50 rounded shadow-sm';

                const projectTitle = document.createElement('h5');
                projectTitle.className = 'font-semibold text-gray-700 mb-2 border-b pb-1';
                projectTitle.textContent = AppUtils.escapeHTML(projectGroup.projectName) || '未知项目';
                groupDiv.appendChild(projectTitle);

                if (projectGroup.tasks && projectGroup.tasks.length > 0) {
                    const ul = document.createElement('ul');
                    ul.className = 'list-none pl-0 space-y-1';
                    projectGroup.tasks.forEach(task => {
                        const li = document.createElement('li');
                        li.className = `text-sm ${task.isOverdue ? 'text-red-600' : 'text-gray-600'}`;
                        li.innerHTML = `
                            <span class="font-medium">${AppUtils.escapeHTML(task.taskName)}</span> 
                            (状态: ${AppUtils.escapeHTML(task.status)}, 
                            截止: ${task.dueDate || 'N/A'}, 
                            优先级: ${AppUtils.escapeHTML(task.priority) || 'N/A'})
                            ${task.isOverdue ? '<span class="font-bold"> - 已逾期!</span>' : ''}
                        `;
                        ul.appendChild(li);
                    });
                    groupDiv.appendChild(ul);
                } else {
                    const noTasksP = document.createElement('p');
                    noTasksP.className = 'text-sm text-gray-500';
                    noTasksP.textContent = '此项目下无相关任务。';
                    groupDiv.appendChild(noTasksP);
                }
                taskListContainer.appendChild(groupDiv);
            });
        }
    }
}

/**
 * 清空员工详情区域 (当选择 "所有员工" 时)。
 */
function clearEmployeeDetails() {
    const kpiCardsContainer = document.getElementById(EMPLOYEE_KPI_CARDS_ID);
    const displayEmployeeNameEl = document.getElementById(DISPLAY_EMPLOYEE_NAME_ID);
    const taskListContainer = document.getElementById(EMPLOYEE_TASK_LIST_CONTAINER_ID);
    const taskPlaceholderEl = document.getElementById(EMPLOYEE_TASK_PLACEHOLDER_ID);

    if (displayEmployeeNameEl) displayEmployeeNameEl.textContent = '所有员工';
    if (kpiCardsContainer) kpiCardsContainer.style.display = 'none';

    if (taskListContainer) taskListContainer.innerHTML = ''; // 清空任务列表
    if (taskPlaceholderEl) {
        taskPlaceholderEl.textContent = '请在上方筛选器中选择一位员工以查看其任务详情。';
        taskPlaceholderEl.style.display = 'block';
    }
}
