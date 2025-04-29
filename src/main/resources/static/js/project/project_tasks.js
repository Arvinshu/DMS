/**
 * 文件路径: src/main/resources/static/js/project/project_tasks.js
 * 开发时间: 2025-04-27 22:40 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 处理项目展开行中任务列表的显示、分组、添加(完整表单)、编辑(行内)、删除操作。
 * 更新内容:
 * 1. 修正问题1：为编辑状态下的保存/取消按钮添加日志，排查无反应问题。
 * 2. 修正问题2：确保“添加任务到此阶段”生成完整表单，并调整其插入位置到阶段分组末尾。
 * 3. 移除 show/hideLoading 调用以避免相关错误。
 * 依赖: common.js (AppUtils), project_api.js (ProjectApiModule), flatpickr, project_list.js (ProjectListModule)
 */
const ProjectTasksModule = (() => {
    // =========================================================================
    // 常量和变量定义
    // =========================================================================
    const TABLE_BODY_ID = 'project-table-body'; // 项目列表 tbody 的 ID

    // DOM 元素引用
    let tableBody; // 指向项目列表的 tbody 元素

    // 状态变量
    let stagesData = []; // 存储所有可用的项目阶段数据
    let employeesData = []; // 存储所有可用的员工数据 (用于负责人下拉框)
    let flatpickrTaskInstances = []; // 存储任务模块中 Flatpickr 的实例

    // =========================================================================
    // 初始化
    // =========================================================================

    /**
     * 初始化任务模块
     * @param {Array<object>} stages - 从后端获取的所有可用阶段数据
     * @param {Array<object>} employees - 从后端获取的所有员工数据
     */
    function init(stages, employees) {
        console.log('Initializing ProjectTasksModule...');
        stagesData = stages || [];
        employeesData = employees || [];
        console.log('ProjectTasksModule received stagesData:', stagesData.length);
        console.log('ProjectTasksModule received employeesData count:', employeesData.length);

        tableBody = document.getElementById(TABLE_BODY_ID);
        if (!tableBody) {
            console.error('ProjectTasksModule: Table body element not found.');
            return;
        }

        // 为 tbody 添加事件委托，处理任务容器内的所有点击事件
        tableBody.addEventListener('click', handleTaskContainerClick);

        console.log('ProjectTasksModule initialized.');
    }

    // =========================================================================
    // 任务加载与渲染
    // =========================================================================

    /**
     * 为指定项目加载并渲染任务列表
     * (由 project_list.js 的 expandProjectRow 调用)
     * @param {number|string} projectId - 项目 ID
     * @param {HTMLElement} containerElement - 用于显示任务列表的容器元素 (通常是 .task-container)
     */
    async function loadTasksForProject(projectId, containerElement) {
        if (!containerElement) {
            console.error(`Task container not found for project ${projectId}`);
            return;
        }
        // 在容器内部显示 Loading 占位符
        containerElement.innerHTML = '<div class="loading-placeholder p-4 text-center text-gray-500">加载任务中...</div>';
        containerElement.classList.remove('loaded', 'error'); // 重置状态

        try {
            const tasks = await ProjectApiModule.fetchTasks(projectId);
            // 在渲染前移除占位符 (确保只移除自己的占位符)
            const placeholder = containerElement.querySelector('.loading-placeholder');
            if (placeholder) placeholder.remove();
            renderTasks(projectId, containerElement, tasks); // 渲染任务
            containerElement.classList.add('loaded'); // 标记为已加载
        } catch (error) {
            console.error(`Error fetching tasks for project ${projectId}:`, error);
            // 渲染错误信息
            containerElement.innerHTML = `<div class="error-message p-4 text-center text-red-500">加载任务失败: ${error.message || '请稍后重试'}</div>`;
            containerElement.classList.add('error'); // 标记为错误状态
        }
        // 移除了 finally 中的 hideLoading 调用
    }

    /**
     * 渲染任务列表，按阶段分组显示
     * @param {number|string} projectId - 当前项目 ID
     * @param {HTMLElement} containerElement - 任务列表容器元素
     * @param {Array<object>} tasks - 从 API 获取的任务数据数组
     */
    function renderTasks(projectId, containerElement, tasks) {
        // 1. 按阶段 ID 将任务分组
        const tasksByStage = {};
        if (!Array.isArray(stagesData)) {
            console.error("stagesData is not an array:", stagesData);
            stagesData = [];
        }
        // 初始化分组
        stagesData.forEach(stage => {
            if (stage && stage.stageId !== undefined) {
                tasksByStage[stage.stageId] = {stageInfo: stage, tasks: []};
            } else {
                console.warn("Invalid stage data found during renderTasks:", stage);
            }
        });

        if (!Array.isArray(tasks)) {
            console.error("tasks is not an array:", tasks);
            tasks = [];
        }
        // 将任务放入对应阶段的分组
        tasks.forEach(task => {
            if (task && task.stageId !== undefined && tasksByStage[task.stageId]) {
                tasksByStage[task.stageId].tasks.push(task);
            } else {
                console.warn(`Task ${task?.taskId} has unknown or invalid stageId ${task?.stageId}. It will not be displayed.`);
            }
        });

        // 2. 构建 HTML
        let stagesHtml = '';
        let globalTaskIndex = 0; // 用于生成连续的任务序号

        // 任务列表的表头
        const taskListHeaderHtml = `
            <div class="task-list-header">
                <span class="task-header-seq">序号</span>
                <span class="task-header-name">任务名称</span>
                <span class="task-header-description">任务描述</span>
                <span class="task-header-priority">优先级</span>
                <span class="task-header-assignee">负责人</span>
                <span class="task-header-startdate">开始时间</span>
                <span class="task-header-duedate">截止日期</span>
                <span class="task-header-status">状态</span>
                <span class="task-header-actions">操作</span>
            </div>
        `;

        // 按照 stagesData 的顺序遍历阶段
        stagesData.forEach(stage => {
            if (!stage || stage.stageId === undefined) return; // 跳过无效阶段

            const stageId = stage.stageId;
            const stageGroup = tasksByStage[stageId];
            const currentTasks = stageGroup ? stageGroup.tasks : [];
            currentTasks.sort((a, b) => (a.taskId || 0) - (b.taskId || 0)); // 按 ID 排序示例

            // 为当前阶段的任务生成 HTML
            const taskItemsHtml = currentTasks.map(task => {
                globalTaskIndex++; // 递增全局序号
                return createTaskDisplayItemHtml(task, globalTaskIndex); // 生成显示行 HTML
            }).join('');

            const completedCount = currentTasks.filter(t => t.taskStatus === '已完成').length;
            const totalCount = currentTasks.length;

            // 构建当前阶段的 HTML 块
            stagesHtml += `
                <div class="task-stage-group" data-stage-id="${stageId}">
                    <h5>${AppUtils.escapeHTML(stage.stageName)} (${completedCount}/${totalCount})</h5>
                    <ul class="task-list">
                        ${taskItemsHtml}
                    </ul>
<!--                     {/* 添加任务到此阶段按钮 */}-->
                     <button class="btn btn-xs add-task-to-stage-btn mt-2" data-stage-id="${stageId}">+ 添加任务到此阶段</button>
<!--                     {/* 添加一个容器用于放置新增任务的表单 */}-->
                     <div class="add-task-form-container"></div>
                </div>`;
        });

        // 3. 填充容器
        containerElement.innerHTML = `
<!--            <h4>任务列表
                <button class="btn btn-xs btn-primary add-task-btn" data-project-id="${projectId}" style="float: right; margin-top: -5px;">添加新任务</button>
            </h4>-->
            ${taskListHeaderHtml}
            <div class="task-stages-wrapper">
                ${stagesHtml || '<p class="text-center text-gray-500 py-4">该项目下暂无任务或未定义阶段。</p>'}
<!--                 {/* 添加一个全局的添加任务表单容器 */}-->
                 <div class="add-task-form-container-global"></div>
            </div>`;
    }

    /**
     * 创建单个任务的显示状态 HTML 字符串
     * @param {object} task - 任务数据对象
     * @param {number} sequenceNumber - 任务的显示序号
     * @returns {string} - 代表任务显示行的 li 元素的 HTML 字符串
     */
    function createTaskDisplayItemHtml(task, sequenceNumber) {
        if (!task || task.taskId === undefined) return ''; // 无效任务数据则返回空
        const priorityClass = getPriorityClass(task.priority); // 获取优先级对应的 CSS 类
        const statusClass = getTaskStatusClass(task.taskStatus); // 获取状态对应的 CSS 类
        const taskId = task.taskId;

        // 返回 li 元素的完整 HTML 结构
        return `
            <li data-task-id="${taskId}" class="task-item">
                <span class="task-seq">${sequenceNumber}</span>
                <span class="task-name">${AppUtils.escapeHTML(task.taskName)}</span>
                <span class="task-description">${AppUtils.escapeHTML(task.taskDescription || '-')}</span>
                <!-- <span class="task-priority">${AppUtils.escapeHTML(task.priority || '-')}</span> -->
                <span class="task-priority-cell"><span class="priority ${priorityClass}">${AppUtils.escapeHTML(task.priority)}</span></span>                
                <span class="task-assignee">${AppUtils.escapeHTML(task.assigneeEmployee || '暂未分配')}</span>
                <span class="task-startdate">${AppUtils.formatDateIfPresent(task.startDate) || '-'}</span>
                <span class="task-duedate">${AppUtils.formatDateIfPresent(task.dueDate) || '-'}</span>                
                <span class="task-status-cell"><span class="status ${statusClass}">${AppUtils.escapeHTML(task.taskStatus)}</span></span>                
                <span class="task-actions-cell">
                    <span class="task-actions-inline">
                        <button class="btn-icon edit-task-btn" title="编辑任务"><i class="icon-edit"></i><span class="btn-text">编辑</span></button>
                        <button class="btn-icon delete-task-btn" title="删除任务"><i class="icon-delete"></i><span class="btn-text">删除</span></button>
                    </span>
                </span>
            </li>`;
    }

    // =========================================================================
    // 创建编辑/添加表单的 HTML
    // =========================================================================

    /**
     * 创建任务的完整编辑/添加表单 DOM 元素 (用于“添加新任务”或“添加任务到此阶段”)
     * @param {object} task - 任务数据对象 (如果是新增，则只包含 projectId)
     * @param {number|string|null} stageIdForNewTask - 新增任务时预选的阶段 ID (可能为 null)
     * @returns {HTMLElement} - 代表编辑/添加行的 li 元素
     */
    function createTaskEditableItem(task = {}, stageIdForNewTask = null) {
        const taskId = task.taskId; // 编辑时有值，新增时为 undefined
        const li = document.createElement('li');
        li.classList.add('task-item', 'editing-row'); // 使用 'editing-row' 表示完整表单模式
        if (taskId) {
            li.dataset.taskId = taskId;
        }
        // 确定当前应选中的阶段 ID
        const currentStageId = taskId ? task.stageId : stageIdForNewTask;

        // 准备负责人下拉选项
        if (!Array.isArray(employeesData)) {
            employeesData = [];
        }
        const assigneeOptions = employeesData.map(emp =>
            `<option value="${AppUtils.escapeHTML(emp.value)}" ${task.assigneeEmployee === emp.value ? 'selected' : ''}>${AppUtils.escapeHTML(emp.text)}</option>`
        ).join('');

        // 准备阶段下拉选项
        if (!Array.isArray(stagesData)) {
            stagesData = [];
        }
        const stageOptions = stagesData.map(stage => {
            if (!stage || stage.stageId === undefined) return '';
            // 确保比较时类型一致 (都转为字符串或数字)
            const isSelected = (currentStageId !== null && String(stage.stageId) === String(currentStageId));
            return `<option value="${stage.stageId}" ${isSelected ? 'selected' : ''}>${AppUtils.escapeHTML(stage.stageName)}</option>`;
        }).join('');

        // 格式化日期
        const startDateValue = AppUtils.formatDateIfPresent(task.startDate, 'Y-m-d');
        const dueDateValue = AppUtils.formatDateIfPresent(task.dueDate, 'Y-m-d');

        // 构建完整表单的 HTML
        li.innerHTML = `
            <div class="task-form">
<!--                {/* 添加一个隐藏的标记，用于区分是新增还是编辑 */}-->
                <input type="hidden" name="isNewTask" value="${taskId ? 'false' : 'true'}">
                <div class="form-group task-name-group">
                    <label for="taskName-${taskId || 'new'}">名称*:</label>
                    <input type="text" id="taskName-${taskId || 'new'}" name="taskName" value="${AppUtils.escapeHTML(task.taskName || '')}" required class="form-input">
                </div>
                <div class="form-group task-priority-group">
                    <label for="priority-${taskId || 'new'}">优先级:</label>
                    <select id="priority-${taskId || 'new'}" name="priority" class="form-select">
                        <option value="High" ${task.priority === 'High' ? 'selected' : ''}>High</option>
                        <option value="Medium" ${!task.priority || task.priority === 'Medium' ? 'selected' : ''}>Medium</option>
                        <option value="Low" ${task.priority === 'Low' ? 'selected' : ''}>Low</option>
                    </select>
                </div>
                <div class="form-group task-stage-group">
                    <label for="stageId-${taskId || 'new'}">阶段*:</label>
                    <select id="stageId-${taskId || 'new'}" name="stageId" required class="form-select">
                        <option value="">-- 选择阶段 --</option>
                        ${stageOptions}
                    </select>
                </div>
                <div class="form-group task-assignee-group">
                    <label for="assigneeEmployee-${taskId || 'new'}">负责人:</label>
                    <select id="assigneeEmployee-${taskId || 'new'}" name="assigneeEmployee" class="form-select">
                        <option value="">未分配</option>
                        ${assigneeOptions}
                    </select>
                </div>
                <div class="form-group task-dates-group">
                    <label for="startDate-${taskId || 'new'}">起止日期:</label>
                    <input type="text" id="startDate-${taskId || 'new'}" name="startDate" value="${startDateValue}" class="form-input flatpickr-input" placeholder="开始日期">
                    <input type="text" id="dueDate-${taskId || 'new'}" name="dueDate" value="${dueDateValue}" class="form-input flatpickr-input" placeholder="截止日期">
                </div>
                <div class="form-group task-status-group">
                    <label for="taskStatus-${taskId || 'new'}">状态*:</label>
                    <select id="taskStatus-${taskId || 'new'}" name="taskStatus" required class="form-select">
                        <option value="待办" ${!task.taskStatus || task.taskStatus === '待办' ? 'selected' : ''}>待办</option>
                        <option value="进行中" ${task.taskStatus === '进行中' ? 'selected' : ''}>进行中</option>
                        <option value="已完成" ${task.taskStatus === '已完成' ? 'selected' : ''}>已完成</option>
                        <option value="已暂停" ${task.taskStatus === '已暂停' ? 'selected' : ''}>已暂停</option>
                        <option value="已取消" ${task.taskStatus === '已取消' ? 'selected' : ''}>已取消</option>
                    </select>
                </div>
                <div class="form-group task-description-group">
                    <label for="taskDescription-${taskId || 'new'}">描述:</label>
                    <textarea id="taskDescription-${taskId || 'new'}" name="taskDescription" class="form-textarea">${AppUtils.escapeHTML(task.taskDescription || '')}</textarea>
                </div>
                <div class="task-actions form-actions">
                    <button class="btn btn-sm btn-save save-task-btn">保存</button>
                    <button class="btn btn-sm btn-cancel cancel-task-btn">取消</button>
                </div>
            </div>`;

        // 初始化日期选择器
        initializeFlatpickrForRow(li);
        return li;
    }

    // =========================================================================
    // 事件处理
    // =========================================================================

    /**
     * 处理任务容器内的点击事件 (事件委托)
     * @param {Event} event - 点击事件对象
     */
    function handleTaskContainerClick(event) {
        const target = event.target; // 获取被点击的元素
        console.log("Task container clicked:", target); // 添加日志

        const taskItem = target.closest('.task-item'); // 获取最近的任务项 li 元素
        const taskContainer = target.closest('.task-container'); // 获取任务容器 div
        const stageGroup = target.closest('.task-stage-group'); // 获取阶段分组 div

        // 如果点击事件不是发生在任务容器内，则忽略
        if (!taskContainer) return;

        // 获取当前项目的 ID
        const projectId = taskContainer.closest('tr.task-details-row')?.dataset.projectId;
        if (!projectId) {
            console.warn("Could not determine projectId from task container.");
            return;
        }

        // --- 检查是否有正在编辑或添加的任务 ---
        const editingOrAddingItem = taskContainer.querySelector('.task-item.editing-row, .task-item.editing-inline');

        // --- 处理全局“添加新任务”按钮 ---
        if (target.closest('.add-task-btn')) {
            console.log("Add New Task button clicked");
            if (editingOrAddingItem) { // 检查是否有正在编辑的项
                AppUtils.showMessage('请先完成当前编辑或添加的任务。', 'warning');
                return;
            }
            const globalFormContainer = taskContainer.querySelector('.add-task-form-container-global');
            if (!globalFormContainer) {
                AppUtils.showMessage('无法找到全局添加任务表单的容器。', 'error');
                return;
            }
            // 显示完整表单用于添加新任务，阶段需要用户选择
            showAddTaskForm(globalFormContainer, projectId, null);
        }
        // --- 处理“添加任务到此阶段”按钮 (改为使用完整表单) ---
        else if (target.closest('.add-task-to-stage-btn')) {
            console.log("Add Task to Stage button clicked");
            if (editingOrAddingItem) {
                AppUtils.showMessage('请先完成当前编辑或添加的任务。', 'warning');
                return;
            }
            if (!stageGroup) return; // 必须在阶段分组内
            const stageId = target.dataset.stageId;
            const stageFormContainer = stageGroup.querySelector('.add-task-form-container');
            if (!stageFormContainer) {
                AppUtils.showMessage('无法找到阶段内添加任务表单的容器。', 'error');
                return;
            }
            // 显示完整表单，并预选当前阶段
            showAddTaskForm(stageFormContainer, projectId, stageId);
        }
        // --- 处理现有任务项 (taskItem) 上的按钮 ---
        else if (taskItem && !taskItem.classList.contains('editing-row') && !taskItem.classList.contains('editing-inline')) { // 确保是显示状态的任务项
            const taskId = taskItem.dataset.taskId; // 获取任务 ID

            // 点击了编辑按钮 (进入行内编辑)
            if (target.closest('.edit-task-btn')) {
                console.log(`Edit button clicked for task ${taskId}`);
                if (editingOrAddingItem) { // 检查是否有其他编辑项
                    AppUtils.showMessage('请先完成当前编辑或添加的任务。', 'warning');
                    return;
                }
                if (taskId) handleEditTask(taskItem, taskId, projectId);
            }
            // 点击了删除按钮
            else if (target.closest('.delete-task-btn')) {
                console.log(`Delete button clicked for task ${taskId}`);
                if (editingOrAddingItem) {
                    AppUtils.showMessage('请先完成当前编辑或添加的任务。', 'warning');
                    return;
                }
                if (taskId) handleDeleteTask(taskItem, taskId, projectId);
            }
        }
        // --- 处理编辑状态下的按钮 (包括完整表单和行内编辑) ---
        else if (taskItem && (taskItem.classList.contains('editing-row') || taskItem.classList.contains('editing-inline'))) {
            const taskId = taskItem.dataset.taskId; // taskId 可能为 undefined (新增情况)
            // 点击了保存按钮
            if (target.closest('.save-task-btn')) {
                console.log(`Save button clicked for task ${taskId || 'new'}`);
                handleSaveTask(taskItem, taskId, projectId); // 调用保存处理函数
            }
            // 点击了取消按钮
            else if (target.closest('.cancel-task-btn')) {
                console.log(`Cancel button clicked for task ${taskId || 'new'}`);
                handleCancelTask(taskItem, taskId, projectId); // 调用取消处理函数
            }
        } else {
            console.log("Clicked elsewhere in task container, no action taken.");
        }
    }

    // =========================================================================
    // CRUD 操作处理函数
    // =========================================================================

    /**
     * 显示完整添加任务表单
     * @param {HTMLElement} container - 表单应添加到的容器元素 (.add-task-form-container 或 .add-task-form-container-global)
     * @param {number|string} projectId - 项目 ID
     * @param {number|string|null} stageId - 预选的阶段 ID (可能为 null)
     */
    function showAddTaskForm(container, projectId, stageId) {
        // 再次检查整个 taskContainer 是否有编辑/添加行
        const taskContainerRoot = container.closest('.task-container');
        if (taskContainerRoot && taskContainerRoot.querySelector('.editing-row, .editing-inline')) {
            AppUtils.showMessage('请先完成当前编辑或添加的任务', 'warning');
            return;
        }
        // 清空目标容器，确保只有一个添加表单
        container.innerHTML = '';
        // 创建完整表单元素
        const editableItem = createTaskEditableItem({projectId}, stageId);
        // 添加到容器
        container.appendChild(editableItem);
        // 聚焦
        editableItem.querySelector('input[name="taskName"]')?.focus();
        // 滚动到新添加的表单位置 (可选)
        editableItem.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }

    /**
     * 处理编辑任务按钮点击 (切换到行内编辑状态)
     * @param {HTMLElement} taskItem - 被点击编辑的任务项 li 元素
     * @param {number|string} taskId - 任务 ID
     * @param {number|string} projectId - 项目 ID
     */
    async function handleEditTask(taskItem, taskId, projectId) {
        console.log(`Entering handleEditTask for task ${taskId}`);
        const container = taskItem.closest('.task-container');
        // 检查同容器内是否有其他编辑或添加行
        if (container.querySelector('.task-item.editing-inline:not([data-task-id="' + taskId + '"]), .task-item.editing-row')) {
            AppUtils.showMessage('请先完成其他正在编辑或添加的任务。', 'warning');
            return;
        }

        AppUtils.showLoading(taskItem); // 显示加载指示器
        try {
            const taskData = await ProjectApiModule.getTaskById(taskId); // 获取最新任务数据
            if (taskData) {
                console.log(`Task data fetched for ${taskId}, converting to inline edit.`);
                // --- 切换到行内编辑状态 ---
                taskItem.classList.add('editing-inline'); // 添加标识类
                taskItem.dataset.originalStageId = taskData.stageId; // 保存原始阶段ID

                // 准备负责人下拉选项 HTML
                if (!Array.isArray(employeesData)) {
                    employeesData = [];
                }
                const assigneeOptionsHtml = `<option value="">未分配</option>` + employeesData.map(emp =>
                    `<option value="${AppUtils.escapeHTML(emp.value)}" ${taskData.assigneeEmployee === emp.value ? 'selected' : ''}>${AppUtils.escapeHTML(emp.text)}</option>`
                ).join('');

                // 替换各单元格内容为输入控件
                taskItem.querySelector('.task-name').innerHTML = `<input type="text" name="taskName" value="${AppUtils.escapeHTML(taskData.taskName || '')}" required class="form-input form-input-sm w-full">`;
                taskItem.querySelector('.task-description').innerHTML = `<textarea name="taskDescription" class="form-textarea form-textarea-sm w-full">${AppUtils.escapeHTML(taskData.taskDescription || '')}</textarea>`;
                taskItem.querySelector('.task-assignee').innerHTML = `<select name="assigneeEmployee" class="form-select form-select-sm w-full">${assigneeOptionsHtml}</select>`;
                taskItem.querySelector('.task-startdate').innerHTML = `<input type="text" name="startDate" value="${AppUtils.formatDateIfPresent(taskData.startDate, 'Y-m-d')}" class="form-input form-input-sm flatpickr-input w-full" placeholder="开始日期">`;
                taskItem.querySelector('.task-duedate').innerHTML = `<input type="text" name="dueDate" value="${AppUtils.formatDateIfPresent(taskData.dueDate, 'Y-m-d')}" class="form-input form-input-sm flatpickr-input w-full" placeholder="截止日期">`;
                taskItem.querySelector('.task-status-cell').innerHTML = `
                    <select name="taskStatus" required class="form-select form-select-sm w-full">
                        <option value="待办" ${taskData.taskStatus === '待办' ? 'selected' : ''}>待办</option>
                        <option value="进行中" ${taskData.taskStatus === '进行中' ? 'selected' : ''}>进行中</option>
                        <option value="已完成" ${taskData.taskStatus === '已完成' ? 'selected' : ''}>已完成</option>
                        <option value="已暂停" ${taskData.taskStatus === '已暂停' ? 'selected' : ''}>已暂停</option>
                        <option value="已取消" ${taskData.taskStatus === '已取消' ? 'selected' : ''}>已取消</option>
                    </select>`;

                // 替换操作按钮为保存/取消
                taskItem.querySelector('.task-actions-cell').innerHTML = `
                    <span class="task-actions-inline">
                        <button class="btn btn-xs btn-save save-task-btn">保存</button>
                        <button class="btn btn-xs btn-cancel cancel-task-btn">取消</button>
                    </span>`;

                // 初始化日期选择器
                initializeFlatpickrForRow(taskItem);
                // 聚焦到名称输入框
                const nameInput = taskItem.querySelector('input[name="taskName"]');
                if (nameInput) nameInput.focus();
                console.log(`Task ${taskId} converted to inline edit mode.`);

            } else {
                AppUtils.showMessage('无法获取任务数据进行编辑。', 'error');
            }
        } catch (error) {
            console.error(`Error preparing task ${taskId} for inline editing:`, error);
            AppUtils.showMessage('准备编辑失败，请重试。', 'error');
        } finally {
            AppUtils.hideLoading(taskItem); // 隐藏加载指示器
        }
    }

    /**
     * 处理删除任务按钮点击
     * @param {HTMLElement} taskItem - 被点击删除的任务项 li 元素
     * @param {number|string} taskId - 任务 ID
     * @param {number|string} projectId - 项目 ID
     */
    async function handleDeleteTask(taskItem, taskId, projectId) {
        console.log(`Entering handleDeleteTask for task ${taskId}`);
        // 弹出确认框
        if (!confirm(`确定要删除任务 "${taskItem.querySelector('.task-name')?.textContent || taskId}" 吗？`)) return;

        AppUtils.showLoading(taskItem); // 显示加载指示器
        let parentContainer = taskItem.closest('.task-container'); // 获取父容器
        try {
            await ProjectApiModule.deleteTask(taskId); // 调用 API 删除
            AppUtils.showMessage('任务删除成功！', 'success');
            taskItem.remove(); // 从 DOM 中移除任务项
            // 重新加载任务以更新计数和序号
            if (parentContainer) {
                console.log(`Reloading tasks for project ${projectId} after deletion.`);
                loadTasksForProject(projectId, parentContainer);
            }
            ProjectListModule.refresh(); // 刷新项目列表以更新进度等
        } catch (error) {
            console.error('Error deleting task:', error);
            AppUtils.showMessage(`删除任务失败: ${error.message || '请重试'}`, 'error');
            // 出错时也要确保隐藏 loading
            if (document.body.contains(taskItem)) {
                AppUtils.hideLoading(taskItem);
            } else if (parentContainer) {
                AppUtils.hideLoading(parentContainer);
            } else {
                AppUtils.hideLoading(tableBody || document.body);
            }
        }
        // finally 块不再需要，成功时元素已移除，失败时在 catch 中处理 loading
    }

    /**
     * 处理保存任务按钮点击 (包括新增、行内编辑、完整表单编辑)
     * @param {HTMLElement} taskItem - 当前任务项 li 元素 (可能是编辑行或行内编辑状态)
     * @param {number|string|undefined} taskId - 任务 ID (编辑时有值，新增时为 undefined)
     * @param {number|string} projectId - 项目 ID
     */
    async function handleSaveTask(taskItem, taskId, projectId) {
        console.log(`Entering handleSaveTask for task ${taskId || 'new'}`);
        const isInlineEditing = taskItem.classList.contains('editing-inline');
        const isFullFormAddOrEdit = taskItem.classList.contains('editing-row');

        // 从当前编辑状态获取数据
        const taskData = getEditableTaskData(taskItem);
        taskData.projectId = projectId; // 确保 projectId 已设置
        console.log("Data collected for save:", taskData);

        // 基本验证
        if (!taskData.taskName || !taskData.stageId || !taskData.taskStatus) {
            AppUtils.showMessage('任务名称、阶段和状态不能为空。', 'error');
            // 聚焦到第一个必填项 (示例)
            const nameInput = taskItem.querySelector('[name="taskName"]');
            const stageSelect = taskItem.querySelector('[name="stageId"]');
            if (nameInput && !taskData.taskName) nameInput.focus();
            else if (stageSelect && !taskData.stageId) stageSelect.focus();
            return;
        }

        AppUtils.showLoading(taskItem); // 显示加载指示器
        let parentContainer = taskItem.closest('.task-container'); // 获取父容器
        let savedSuccessfully = false; // 标记保存是否成功

        try {
            let savedTask;
            if (taskId) { // --- 更新现有任务 ---
                console.log(`Updating task ${taskId}`);
                savedTask = await ProjectApiModule.updateTask(taskId, taskData);
                AppUtils.showMessage('任务更新成功！', 'success');
                savedSuccessfully = true; // 标记成功

                // --- 恢复到显示状态 ---
                destroyFlatpickrInstancesForRow(taskItem); // 销毁日期选择器

                if (isInlineEditing) {
                    console.log(`Restoring display state for inline edited task ${taskId}`);
                    taskItem.classList.remove('editing-inline'); // 移除行内编辑类
                    const sequenceNumber = taskItem.querySelector('.task-seq')?.textContent || '?'; // 保留序号
                    // 使用更新后的数据重新渲染内部 HTML
                    const displayItemHtml = createTaskDisplayItemHtml(savedTask, sequenceNumber);
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = displayItemHtml;
                    if (tempDiv.firstChild) taskItem.innerHTML = tempDiv.firstChild.innerHTML; // 替换内部 HTML
                } else if (isFullFormAddOrEdit) { // 完整表单编辑 (理论上不应进入此分支，因为编辑是行内的)
                    console.warn("Saving task from full form edit state - unexpected.");
                    taskItem.remove();
                    if (parentContainer) loadTasksForProject(projectId, parentContainer);
                }

            } else { // --- 新增任务 (来自完整表单) ---
                console.log("Creating new task");
                savedTask = await ProjectApiModule.createTask(taskData);
                AppUtils.showMessage('任务添加成功！', 'success');
                savedSuccessfully = true; // 标记成功
                taskItem.remove(); // 移除添加表单行
                // 重新加载任务列表以显示新任务和正确序号
                if (parentContainer) loadTasksForProject(projectId, parentContainer);
            }

            // 刷新项目列表以更新可能的进度条等信息
            ProjectListModule.refresh();

        } catch (error) {
            console.error('Error saving task:', error);
            AppUtils.showMessage(`保存任务失败: ${error.message || '请重试'}`, 'error');
            // 保留编辑状态，让用户可以修正错误
        } finally {
            // 只有在元素仍然存在时才尝试隐藏 loading
            if (document.body.contains(taskItem)) {
                AppUtils.hideLoading(taskItem);
            } else if (parentContainer && savedSuccessfully) { // 如果元素被移除且保存成功，在父容器隐藏
                AppUtils.hideLoading(parentContainer);
            } else if (parentContainer) { // 如果元素被移除但保存失败，也在父容器隐藏
                AppUtils.hideLoading(parentContainer);
            } else {
                // Fallback
                AppUtils.hideLoading(tableBody || document.body);
            }
        }
    }

    /**
     * 处理取消任务编辑/添加按钮点击
     * @param {HTMLElement} taskItem - 当前任务项 li 元素 (可能是编辑行或行内编辑状态)
     * @param {number|string|undefined} taskId - 任务 ID (编辑时有值，新增时为 undefined)
     * @param {number|string} projectId - 项目 ID
     */
    async function handleCancelTask(taskItem, taskId, projectId) {
        console.log(`Entering handleCancelTask for task ${taskId || 'new'}`);
        destroyFlatpickrInstancesForRow(taskItem); // 销毁可能存在的日期选择器

        if (taskId) { // --- 取消编辑现有任务 ---
            const isInlineEditing = taskItem.classList.contains('editing-inline');
            AppUtils.showLoading(taskItem); // 显示加载指示器
            let elementToRemoveOnError = taskItem; // 记录元素，以防出错时移除
            let parentContainer = taskItem.closest('.task-container'); // 获取父容器

            try {
                const originalTaskData = await ProjectApiModule.getTaskById(taskId); // 获取原始数据
                if (originalTaskData) {
                    if (isInlineEditing) {
                        console.log(`Restoring display state for inline edited task ${taskId}`);
                        // --- 恢复行内编辑状态到显示状态 ---
                        taskItem.classList.remove('editing-inline'); // 移除编辑类
                        const sequenceNumber = taskItem.querySelector('.task-seq')?.textContent || '?'; // 保留序号
                        // 使用原始数据生成显示状态的内部 HTML
                        const displayItemHtml = createTaskDisplayItemHtml(originalTaskData, sequenceNumber);
                        const tempDiv = document.createElement('div');
                        tempDiv.innerHTML = displayItemHtml;
                        // 替换内部 HTML
                        if (tempDiv.firstChild) {
                            taskItem.innerHTML = tempDiv.firstChild.innerHTML;
                        } else {
                            console.error("Failed to parse display HTML for task:", taskId);
                            throw new Error("HTML parsing failed"); // 抛出错误
                        }
                        elementToRemoveOnError = null; // 恢复成功，标记为不移除

                    } else { // 取消完整表单编辑 (理论上不应进入此分支)
                        console.warn("Cancelling task from full form edit state - unexpected.");
                        taskItem.remove();
                        elementToRemoveOnError = null; // 元素已移除
                        if (parentContainer) loadTasksForProject(projectId, parentContainer);
                    }
                } else { // 获取原始数据失败
                    AppUtils.showMessage('无法获取原始任务数据，将移除该行。', 'warning');
                    if (elementToRemoveOnError && elementToRemoveOnError.parentNode) elementToRemoveOnError.remove();
                    elementToRemoveOnError = null;
                }
            } catch (error) { // API 调用或 HTML 解析出错
                console.error('Error cancelling task edit:', error);
                AppUtils.showMessage('取消编辑失败，请重试或刷新。', 'error');
                // 尝试移除可能处于损坏状态的编辑行
                if (elementToRemoveOnError && elementToRemoveOnError.parentNode) elementToRemoveOnError.remove();
                elementToRemoveOnError = null;
            } finally {
                // --- 健壮的 Loading 处理 ---
                if (elementToRemoveOnError && document.body.contains(elementToRemoveOnError)) {
                    AppUtils.hideLoading(elementToRemoveOnError); // 在原元素上隐藏
                } else if (parentContainer && document.body.contains(parentContainer)) {
                    // 如果原元素不在了，尝试在父容器隐藏
                    AppUtils.hideLoading(parentContainer);
                } else {
                    // Fallback: 尝试在 tableBody 或 body 上隐藏
                    AppUtils.hideLoading(tableBody || document.body);
                }
            }
        } else { // --- 取消新增任务 (来自完整表单) ---
            console.log("Cancelling new task form.");
            // 仅当它是完整表单添加行时移除
            if (taskItem.classList.contains('editing-row')) {
                taskItem.remove();
            }
        }
    }


    // =========================================================================
    // 辅助函数
    // =========================================================================

    /**
     * 从编辑行 (行内或完整表单) 获取任务数据
     * @param {HTMLElement} taskItem - 任务项 li 元素 (处于编辑状态)
     * @returns {object} - 包含任务数据的对象
     */
    function getEditableTaskData(taskItem) {
        const data = {};
        const isInlineEditing = taskItem.classList.contains('editing-inline');
        // 根据编辑模式确定查找输入的源元素
        const sourceElement = isInlineEditing ? taskItem : taskItem.querySelector('.task-form');

        if (!sourceElement) {
            console.error("Could not find source element for getting editable data in:", taskItem);
            return {}; // 返回空对象表示失败
        }

        // 从对应的输入控件获取值
        data.taskName = sourceElement.querySelector('input[name="taskName"]')?.value.trim();
        data.taskDescription = sourceElement.querySelector('textarea[name="taskDescription"]')?.value.trim();
        data.priority = sourceElement.querySelector('select[name="priority"]')?.value; // 仅完整表单有
        data.assigneeEmployee = sourceElement.querySelector('select[name="assigneeEmployee"]')?.value;
        data.startDate = sourceElement.querySelector('input[name="startDate"]')?.value || null;
        data.dueDate = sourceElement.querySelector('input[name="dueDate"]')?.value || null;
        data.taskStatus = sourceElement.querySelector('select[name="taskStatus"]')?.value;

        // 获取阶段 ID
        if (isInlineEditing) {
            // 行内编辑时，阶段通常不修改，从 data 属性获取原始值
            data.stageId = parseInt(taskItem.dataset.originalStageId);
        } else {
            // 完整表单时，从下拉框获取
            data.stageId = parseInt(sourceElement.querySelector('select[name="stageId"]')?.value);
        }

        // 清理空字符串值为 null
        for (const key in data) {
            if (data[key] === '') {
                data[key] = null;
            }
        }
        // 确保 stageId 是有效的数字或 null
        if (isNaN(data.stageId)) {
            data.stageId = null;
        }

        return data;
    }


    /**
     * 根据任务优先级字符串返回对应的 CSS 类名
     * @param {string} priority - 任务优先级 (如 'High', 'Medium','Low')
     * @returns {string} - 对应的 CSS 类名
     */
    function getPriorityClass(priority) {
        switch (priority) {
            case 'High':
                return 'priority-High';
            case 'Medium':
                return 'priority-Medium';
            case 'Low':
                return 'priority-Low';
            default:
                return 'priority-unknown';
        }
    }

    /**
     * 根据任务状态字符串返回对应的 CSS 类名
     * @param {string} status - 任务状态 (如 '待办', '进行中')
     * @returns {string} - 对应的 CSS 类名
     */
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

    // =========================================================================
    // Flatpickr 相关函数
    // =========================================================================

    /**
     * 初始化指定行内的 Flatpickr 日期选择器实例
     * @param {HTMLElement} rowElement - 需要初始化日期选择器的行元素 (li)
     */
    function initializeFlatpickrForRow(rowElement) {
        const dateInputs = rowElement.querySelectorAll('.flatpickr-input');
        dateInputs.forEach(input => {
            // 检查是否已初始化，避免重复
            if (input._flatpickr) return;
            // 初始化 Flatpickr
            const instance = flatpickr(input, {
                dateFormat: "Y-m-d", // 日期格式
                locale: "zh", // 使用中文语言包 (需引入对应的 locale 文件)
                allowInput: true // 允许手动输入日期
            });
            // 存储实例以便后续销毁
            flatpickrTaskInstances.push({element: input, instance: instance});
        });
    }

    /**
     * 销毁指定容器内的所有 Flatpickr 实例
     * @param {HTMLElement} containerElement - 包含日期选择器的容器元素
     */
    function destroyFlatpickrInstancesForRow(containerElement) {
        const inputs = containerElement.querySelectorAll('.flatpickr-input');
        inputs.forEach(input => {
            // 查找对应的实例数据
            const instanceDataIndex = flatpickrTaskInstances.findIndex(item => item.element === input);
            if (instanceDataIndex > -1) {
                const instanceData = flatpickrTaskInstances[instanceDataIndex];
                if (instanceData.instance) {
                    try {
                        instanceData.instance.destroy(); // 销毁实例
                    } catch (e) {
                        console.warn("Error destroying flatpickr instance:", e);
                    }
                }
                // 从数组中移除该实例数据
                flatpickrTaskInstances.splice(instanceDataIndex, 1);
            }
            // 确保移除 Flatpickr 可能添加的额外属性或类
            if (input._flatpickr) {
                try {
                    delete input._flatpickr;
                } catch (e) {
                    console.warn("Error deleting _flatpickr property:", e);
                }
            }
        });
        // console.debug("Flatpickr instances remaining:", flatpickrTaskInstances.length);
    }

    // =========================================================================
    // 暴露公共接口
    // =========================================================================
    return {
        init: init, // 初始化模块
        loadTasksForProject: loadTasksForProject, // 加载并渲染指定项目的任务
        destroyFlatpickrInstancesForRow: destroyFlatpickrInstancesForRow // 销毁指定容器内的 Flatpickr 实例 (供外部调用，例如 project_list 收起行时)
    };
})();
