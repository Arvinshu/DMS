/**
 * 文件路径: src/main/resources/static/js/project/project_tasks.js
 * 开发时间: 2025-04-25 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 处理项目展开行中任务列表的显示、分组、行内 CRUD 操作。
 * 更新内容: 修正 handleSaveTask 中新增任务成功后未移除编辑表单的问题。
 * 依赖: common.js (AppUtils), project_api.js (ProjectApiModule), flatpickr
 */
const ProjectTasksModule = (() => {
    // 常量定义
    const TABLE_BODY_ID = 'project-table-body';

    // DOM 元素引用
    let tableBody;

    // 状态变量
    let currentlyExpandedProjectId = null;
    let stagesData = [];
    let employeesData = [];
    let flatpickrTaskInstances = [];

    // 初始化函数
    function init(stages, employees) {
        console.log('Initializing ProjectTasksModule...');
        stagesData = stages || [];
        employeesData = employees || [];
        console.log('ProjectTasksModule received employeesData:', JSON.stringify(employeesData.slice(0, 5)));
        tableBody = document.getElementById(TABLE_BODY_ID);
        if (!tableBody) {
            console.error('ProjectTasksModule: Table body element not found.');
            return;
        }
        tableBody.addEventListener('click', handleToggleTasksClick);
        tableBody.addEventListener('click', handleTaskContainerClick);
        console.log('ProjectTasksModule initialized.');
    }

    // 处理项目行展开/收起按钮点击
    function handleToggleTasksClick(event) {
        const target = event.target;
        const toggleButton = target.closest('.toggle-tasks-btn');
        if (!toggleButton) return;
        const projectRow = target.closest('tr');
        const projectId = projectRow.dataset.projectId;
        const taskDetailsRow = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"]`);
        const icon = toggleButton.querySelector('i');
        if (!taskDetailsRow || !icon) return;
        if (currentlyExpandedProjectId === projectId) {
            taskDetailsRow.style.display = 'none';
            icon.classList.remove('icon-chevron-down');
            icon.classList.add('icon-chevron-right');
            currentlyExpandedProjectId = null;
            destroyFlatpickrInstancesForRow(taskDetailsRow);
        } else {
            if (currentlyExpandedProjectId !== null) {
                const previousTaskRow = tableBody.querySelector(`tr.task-details-row[data-project-id="${currentlyExpandedProjectId}"]`);
                const previousButton = tableBody.querySelector(`tr[data-project-id="${currentlyExpandedProjectId}"] .toggle-tasks-btn i`);
                if (previousTaskRow) {
                    previousTaskRow.style.display = 'none';
                    destroyFlatpickrInstancesForRow(previousTaskRow);
                }
                if (previousButton) {
                    previousButton.classList.remove('icon-chevron-down');
                    previousButton.classList.add('icon-chevron-right');
                }
            }
            taskDetailsRow.style.display = '';
            icon.classList.remove('icon-chevron-right');
            icon.classList.add('icon-chevron-down');
            currentlyExpandedProjectId = projectId;
            const taskContainer = taskDetailsRow.querySelector('.task-container');
            if (taskContainer && !taskContainer.classList.contains('loaded')) {
                loadTasksForProject(projectId, taskContainer);
            }
        }
    }

    // 为指定项目加载并渲染任务
    async function loadTasksForProject(projectId, containerElement) {
        containerElement.innerHTML = '<div class="loading p-4 text-center text-gray-500">加载任务中...</div>';
        containerElement.classList.remove('loaded', 'error');
        try {
            const tasks = await ProjectApiModule.fetchTasks(projectId);
            renderTasks(projectId, containerElement, tasks);
            containerElement.classList.add('loaded');
        } catch (error) {
            console.error(`Error fetching tasks for project ${projectId}:`, error);
            containerElement.innerHTML = `<div class="error-message p-4 text-center text-red-500">加载任务失败: ${error.message || '请稍后重试'}</div>`;
            containerElement.classList.add('error');
        }
    }

    // 渲染任务列表，按阶段分组
    function renderTasks(projectId, containerElement, tasks) {
        const tasksByStage = {};
        if (!Array.isArray(stagesData)) {
            console.error("stagesData is not an array:", stagesData);
            stagesData = [];
        }
        stagesData.forEach(stage => {
            if (stage && stage.stageId) {
                tasksByStage[stage.stageId] = {stageInfo: stage, tasks: []};
            } else {
                console.warn("Invalid stage data found:", stage);
            }
        });
        if (!Array.isArray(tasks)) {
            console.error("tasks is not an array:", tasks);
            tasks = [];
        }
        tasks.forEach(task => {
            if (task && task.stageId && tasksByStage[task.stageId]) {
                tasksByStage[task.stageId].tasks.push(task);
            } else {
                console.warn(`Task ${task?.taskId} has unknown or invalid stageId ${task?.stageId}`);
            }
        });
        let stagesHtml = '';
        stagesData.forEach(stage => {
            if (!stage || !stage.stageId) return;
            const stageId = stage.stageId;
            const stageGroup = tasksByStage[stageId];
            if (!stageGroup) return;
            stageGroup.tasks.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
            const taskItemsHtml = stageGroup.tasks.map(task => createTaskDisplayItemHtml(task)).join('');
            const completedCount = stageGroup.tasks.filter(t => t.taskStatus === '已完成').length;
            const totalCount = stageGroup.tasks.length;
            stagesHtml += `
                <div class="task-stage-group" data-stage-id="${stageId}">
                    <h5>${AppUtils.escapeHTML(stage.stageName)} (${completedCount}/${totalCount})</h5>
                    <ul class="task-list"> ${taskItemsHtml} <li class="add-task-placeholder" style="display: none;"></li> </ul>
                     <button class="btn btn-xs add-task-to-stage-btn" data-stage-id="${stageId}">+ 添加任务到此阶段</button>
                </div>`;
        });
        containerElement.innerHTML = `<h4>任务列表 <button class="btn btn-xs btn-primary add-task-btn" data-project-id="${projectId}" style="float: right; margin-top: -5px;">添加新任务</button></h4> <div class="task-stages-wrapper">${stagesHtml || '<p class="text-center text-gray-500 py-4">该项目下暂无任务。</p>'}</div>`;
    }

    // 创建单个任务的显示 HTML 字符串
    function createTaskDisplayItemHtml(task) {
        if (!task) return '';
        const statusClass = getTaskStatusClass(task.taskStatus);
        return `
            <li data-task-id="${task.taskId}" class="task-item">
                <div class="task-main"> <span class="task-name">${AppUtils.escapeHTML(task.taskName)}</span> <span class="task-priority priority-${(task.priority || 'medium').toLowerCase()}">${task.priority || 'Medium'}</span> </div>
                <div class="task-meta">
                    <span class="task-assignee"><i class="icon-user"></i> ${AppUtils.escapeHTML(task.assigneeEmployee || '未分配')}</span>
                    <span class="task-due-date"><i class="icon-calendar"></i> ${AppUtils.formatDateIfPresent(task.dueDate) || '无截止日期'}</span>
                    <span class="task-status ${statusClass}">${AppUtils.escapeHTML(task.taskStatus)}</span>
                    <span class="task-description-meta" title="${AppUtils.escapeHTML(task.taskDescription || '')}"> <i class="icon-info"></i> ${AppUtils.escapeHTML(task.taskDescription || '无描述')} </span>
                    <span class="task-actions-inline">
                        <button class="btn-icon edit-task-btn" title="编辑任务"> <i class="icon-edit"></i> <span class="btn-text">编辑</span> </button>
                        <button class="btn-icon delete-task-btn" title="删除任务"> <i class="icon-delete"></i> <span class="btn-text">删除</span> </button>
                    </span>
                </div>
            </li>`;
    }

    // 创建任务的编辑行 DOM 元素
    function createTaskEditableItem(task = {}, stageIdForNewTask = null) {
        const taskId = task.taskId;
        const li = document.createElement('li');
        li.classList.add('task-item', 'editing-row');
        if (taskId) {
            li.dataset.taskId = taskId;
        }
        const currentStageId = taskId ? task.stageId : stageIdForNewTask;
        if (!Array.isArray(employeesData)) {
            console.error("employeesData is not an array:", employeesData);
            employeesData = [];
        }
        const assigneeOptions = employeesData.map(emp => `<option value="${AppUtils.escapeHTML(emp.value)}" ${task.assigneeEmployee === emp.value ? 'selected' : ''}>${AppUtils.escapeHTML(emp.text)}</option>`).join('');
        if (!Array.isArray(stagesData)) {
            console.error("stagesData is not an array:", stagesData);
            stagesData = [];
        }
        const stageOptions = stagesData.map(stage => `<option value="${stage.stageId}" ${currentStageId === stage.stageId ? 'selected' : ''}>${AppUtils.escapeHTML(stage.stageName)}</option>`).join('');
        const startDateValue = AppUtils.formatDateIfPresent(task.startDate, 'Y-m-d');
        const dueDateValue = AppUtils.formatDateIfPresent(task.dueDate, 'Y-m-d');
        li.innerHTML = `<div class="task-form"> <div class="form-group task-name-group"> <label for="taskName-${taskId || 'new'}">名称*:</label> <input type="text" id="taskName-${taskId || 'new'}" name="taskName" value="${AppUtils.escapeHTML(task.taskName || '')}" required class="form-input"> </div> <div class="form-group task-priority-group"> <label for="priority-${taskId || 'new'}">优先级:</label> <select id="priority-${taskId || 'new'}" name="priority" class="form-select"> <option value="High" ${task.priority === 'High' ? 'selected' : ''}>High</option> <option value="Medium" ${!task.priority || task.priority === 'Medium' ? 'selected' : ''}>Medium</option> <option value="Low" ${task.priority === 'Low' ? 'selected' : ''}>Low</option> </select> </div> <div class="form-group task-stage-group"> <label for="stageId-${taskId || 'new'}">阶段*:</label> <select id="stageId-${taskId || 'new'}" name="stageId" required class="form-select"> ${stageOptions} </select> </div> <div class="form-group task-assignee-group"> <label for="assigneeEmployee-${taskId || 'new'}">负责人:</label> <select id="assigneeEmployee-${taskId || 'new'}" name="assigneeEmployee" class="form-select"> <option value="">未分配</option> ${assigneeOptions} </select> </div> <div class="form-group task-dates-group"> <label for="startDate-${taskId || 'new'}">起止日期:</label> <input type="text" id="startDate-${taskId || 'new'}" name="startDate" value="${startDateValue}" class="form-input flatpickr-input" placeholder="开始日期"> <input type="text" id="dueDate-${taskId || 'new'}" name="dueDate" value="${dueDateValue}" class="form-input flatpickr-input" placeholder="截止日期"> </div> <div class="form-group task-status-group"> <label for="taskStatus-${taskId || 'new'}">状态*:</label> <select id="taskStatus-${taskId || 'new'}" name="taskStatus" required class="form-select"> <option value="待办" ${!task.taskStatus || task.taskStatus === '待办' ? 'selected' : ''}>待办</option> <option value="进行中" ${task.taskStatus === '进行中' ? 'selected' : ''}>进行中</option> <option value="已完成" ${task.taskStatus === '已完成' ? 'selected' : ''}>已完成</option> <option value="已暂停" ${task.taskStatus === '已暂停' ? 'selected' : ''}>已暂停</option> <option value="已取消" ${task.taskStatus === '已取消' ? 'selected' : ''}>已取消</option> </select> </div> <div class="form-group task-description-group"> <label for="taskDescription-${taskId || 'new'}">描述:</label> <textarea id="taskDescription-${taskId || 'new'}" name="taskDescription" class="form-textarea">${AppUtils.escapeHTML(task.taskDescription || '')}</textarea> </div> <div class="task-actions form-actions"> <button class="btn btn-sm btn-save save-task-btn">保存</button> <button class="btn btn-sm btn-cancel cancel-task-btn">取消</button> </div> </div>`;
        initializeFlatpickrForRow(li);
        return li;
    }

    // 处理任务容器内的按钮点击
    function handleTaskContainerClick(event) {
        const target = event.target;
        const taskItem = target.closest('.task-item');
        const taskContainer = target.closest('.task-container');
        const stageGroup = target.closest('.task-stage-group');
        if (!taskContainer) return;
        const projectId = taskContainer.closest('tr.task-details-row')?.dataset.projectId;
        if (target.closest('.add-task-btn')) {
            const firstStageGroup = taskContainer.querySelector('.task-stage-group');
            if (firstStageGroup) {
                showAddTaskForm(firstStageGroup, projectId, firstStageGroup.dataset.stageId);
            } else {
                AppUtils.showMessage('没有可添加任务的阶段。', 'warning');
            }
        } else if (target.closest('.add-task-to-stage-btn')) {
            showAddTaskForm(stageGroup, projectId, target.dataset.stageId);
        } else if (taskItem) {
            const taskId = taskItem.dataset.taskId;
            if (target.closest('.edit-task-btn')) {
                handleEditTask(taskItem, taskId, projectId);
            } else if (target.closest('.delete-task-btn')) {
                handleDeleteTask(taskItem, taskId, projectId);
            } else if (target.closest('.save-task-btn')) {
                handleSaveTask(taskItem, taskId, projectId);
            } else if (target.closest('.cancel-task-btn')) {
                handleCancelTask(taskItem, taskId, projectId);
            }
        }
    }

    // 显示添加任务的表单
    function showAddTaskForm(stageGroupElement, projectId, stageId) {
        if (!stageGroupElement || !projectId || !stageId) return;
        const taskListElement = stageGroupElement.querySelector('.task-list');
        const container = stageGroupElement.closest('.task-container');
        if (taskListElement.querySelector('.editing-row') || container.querySelector('.task-item.editing-row')) {
            AppUtils.showMessage('请先完成当前正在编辑的任务。', 'warning');
            return;
        }
        const editableItem = createTaskEditableItem({projectId: projectId}, stageId);
        taskListElement.appendChild(editableItem);
    }

    // 处理编辑任务按钮点击
    async function handleEditTask(taskItem, taskId, projectId) {
        if (!taskId || !projectId) return;
        const container = taskItem.closest('.task-container');
        if (container.querySelector('.task-item.editing-row:not([data-task-id="' + taskId + '"])')) {
            AppUtils.showMessage('请先完成其他正在编辑的任务。', 'warning');
            return;
        }
        AppUtils.showLoading(taskItem);
        try {
            const taskData = await ProjectApiModule.getTaskById(taskId);
            if (taskData) {
                const editableItem = createTaskEditableItem(taskData);
                taskItem.parentNode.replaceChild(editableItem, taskItem);
            } else {
                AppUtils.showMessage('无法获取任务数据进行编辑。', 'error');
            }
        } catch (error) {
            console.error(`Error preparing task ${taskId} for editing:`, error);
            AppUtils.showMessage('准备编辑失败，请重试。', 'error');
        } finally {
            AppUtils.hideLoading(taskItem);
        }
    }

    // 处理删除任务按钮点击
    async function handleDeleteTask(taskItem, taskId, projectId) {
        if (!taskId || !projectId || !confirm(`确定要删除任务 ID 为 ${taskId} 吗？`)) return;
        AppUtils.showLoading(taskItem);
        try {
            await ProjectApiModule.deleteTask(taskId);
            AppUtils.showMessage('任务删除成功！', 'success');
            taskItem.remove();
            const container = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"] .task-container`);
            if (container) loadTasksForProject(projectId, container); // Reload tasks
        } catch (error) {
            console.error('Error deleting task:', error);
        } finally {
            AppUtils.hideLoading(taskItem);
        }
    }

    // 处理保存任务按钮点击 (修正新增逻辑)
    async function handleSaveTask(taskItem, taskId, projectId) {
        const taskData = getEditableTaskData(taskItem);
        taskData.projectId = projectId;
        if (!taskData.taskName || !taskData.stageId || !taskData.taskStatus) {
            AppUtils.showMessage('任务名称、阶段和状态不能为空。', 'error');
            return;
        }
        AppUtils.showLoading(taskItem);
        try {
            let savedTask;
            if (taskId) { // 更新逻辑
                savedTask = await ProjectApiModule.updateTask(taskId, taskData);
                AppUtils.showMessage('任务更新成功！', 'success');
                // 用保存后的数据替换编辑行
                const displayItemHtml = createTaskDisplayItemHtml(savedTask);
                taskItem.innerHTML = displayItemHtml;
                taskItem.className = 'task-item';
                taskItem.dataset.taskId = savedTask.taskId;
            } else { // 新增逻辑
                savedTask = await ProjectApiModule.createTask(taskData);
                AppUtils.showMessage('任务添加成功！', 'success');
                // *** 修正：直接移除编辑行 ***
                taskItem.remove();
            }

            destroyFlatpickrInstancesForRow(taskItem); // 销毁可能存在的 flatpickr
            // 重新加载该项目的任务列表以显示最新状态
            const container = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"] .task-container`);
            if (container) loadTasksForProject(projectId, container);
            // 刷新项目列表以更新项目状态/阶段显示
            ProjectListModule.refresh();
        } catch (error) {
            console.error('Error saving task:', error);
            // 错误消息应由 AppUtils.request 处理并显示
        } finally {
            // 确保在 taskItem 被移除前隐藏加载动画
            if (taskItem.parentNode) {
                AppUtils.hideLoading(taskItem);
            } else {
                // 如果 taskItem 已被移除，可能需要在父容器或其他地方隐藏加载
                const container = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"] .task-container`);
                if (container) AppUtils.hideLoading(container); // 尝试在容器上隐藏
                else AppUtils.hideLoading(); // 最坏情况，作用于 body
            }
        }
    }

    // 处理取消任务编辑/添加按钮点击
    async function handleCancelTask(taskItem, taskId, projectId) {
        destroyFlatpickrInstancesForRow(taskItem);
        if (taskId) { // 取消编辑
            AppUtils.showLoading(taskItem);
            try {
                const taskData = await ProjectApiModule.getTaskById(taskId);
                if (taskData) {
                    const displayItemHtml = createTaskDisplayItemHtml(taskData);
                    if (taskItem.parentNode) {
                        taskItem.innerHTML = displayItemHtml;
                        taskItem.className = 'task-item';
                        taskItem.dataset.taskId = taskId;
                    } else {
                        console.error("Parent node not found for task item:", taskItem);
                        AppUtils.showMessage('恢复任务显示失败。', 'error');
                    }
                } else {
                    AppUtils.showMessage('无法获取原始任务数据。', 'warning');
                    if (taskItem.parentNode) taskItem.remove();
                }
            } catch (error) {
                console.error('Error cancelling task edit:', error);
                AppUtils.showMessage('取消编辑失败，请重试。', 'error');
                if (taskItem.parentNode) taskItem.remove();
            } finally {
                if (taskItem.parentNode) AppUtils.hideLoading(taskItem); else AppUtils.hideLoading();
            }
        } else {
            taskItem.remove();
        } // 取消新增
    }


    // 从编辑行获取任务数据
    function getEditableTaskData(taskItem) {
        const data = {};
        data.taskName = taskItem.querySelector('input[name="taskName"]')?.value.trim();
        data.taskDescription = taskItem.querySelector('textarea[name="taskDescription"]')?.value.trim();
        data.priority = taskItem.querySelector('select[name="priority"]')?.value;
        data.assigneeEmployee = taskItem.querySelector('select[name="assigneeEmployee"]')?.value;
        data.startDate = taskItem.querySelector('input[name="startDate"]')?.value || null;
        data.dueDate = taskItem.querySelector('input[name="dueDate"]')?.value || null;
        data.stageId = parseInt(taskItem.querySelector('select[name="stageId"]')?.value);
        data.taskStatus = taskItem.querySelector('select[name="taskStatus"]')?.value;
        for (const key in data) {
            if (data[key] === '') {
                data[key] = null;
            }
        }
        if (isNaN(data.stageId)) data.stageId = null;
        return data;
    }

    // 根据任务状态获取 CSS 类
    function getTaskStatusClass(status) {
        switch (status) {
            case '待办':
                return 'status-todo';
            case '进行中':
                return 'status-inprogress';
            case '已完成':
                return 'status-completed';
            case '已暂停':
                return 'status-onhold';
            case '已取消':
                return 'status-cancelled';
            default:
                return 'status-unknown';
        }
    }

    // 初始化和销毁 Flatpickr
    function initializeFlatpickrForRow(rowElement) {
        const dateInputs = rowElement.querySelectorAll('.flatpickr-input');
        dateInputs.forEach(input => {
            if (input._flatpickr) return;
            const instance = flatpickr(input, {dateFormat: "Y-m-d", locale: "zh", allowInput: true});
            flatpickrTaskInstances.push({element: input, instance: instance});
        });
    }

    function destroyFlatpickrInstancesForRow(containerElement) {
        const inputs = containerElement.querySelectorAll('.flatpickr-input');
        inputs.forEach(input => {
            const instanceData = flatpickrTaskInstances.find(item => item.element === input);
            if (instanceData && instanceData.instance) {
                instanceData.instance.destroy();
                flatpickrTaskInstances = flatpickrTaskInstances.filter(item => item.element !== input);
            }
        });
    }

    return {init: init};
})();
