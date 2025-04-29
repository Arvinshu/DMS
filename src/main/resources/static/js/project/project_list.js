/**
 * 文件路径: src/main/resources/static/js/project/project_list.js
 * 开发时间: 2025-04-25 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 处理项目列表获取、渲染、搜索、分页、行点击展开/收起、序号生成。
 * 更新内容: 实现行点击展开/收起，添加序号，移除旧的 toggle 按钮逻辑。
 * 依赖: common.js (AppUtils), project_api.js (ProjectApiModule), pagination.js (via AppUtils), project_tasks.js (ProjectTasksModule)
 */
const ProjectListModule = (() => {
    // 常量定义
    const TABLE_BODY_ID = 'project-table-body';
    const PAGINATION_ID = 'project-pagination';
    // 搜索元素
    const SEARCH_NAME_ID = 'search-project-name';
    const SEARCH_TAGS_ID = 'search-project-tags';
    const SEARCH_BUSINESS_TYPE_ID = 'search-business-type';
    const SEARCH_PROFIT_CENTER_ID = 'search-profit-center';
    const SEARCH_STAGE_ID = 'search-project-stage';
    const SEARCH_STATUS_ID = 'search-project-status';
    const SEARCH_BTN_ID = 'search-project-btn';

    // DOM 元素引用
    let tableBody;
    let searchNameInput, searchTagsSelect, searchBusinessTypeSelect, searchProfitCenterSelect, searchStageSelect,
        searchStatusSelect, searchBtn;

    // 状态变量
    let currentPage = 1;
    const pageSize = 50;
    let currentFilters = {};
    let totalItems = 0;
    let totalPages = 1;
    let debounceTimer;
    let currentlyExpandedProjectId = null; // 跟踪当前展开的项目ID

    // 初始化函数
    function init() {
        console.log('Initializing ProjectListModule...');
        tableBody = document.getElementById(TABLE_BODY_ID);
        searchNameInput = document.getElementById(SEARCH_NAME_ID);
        searchTagsSelect = document.getElementById(SEARCH_TAGS_ID);
        searchBusinessTypeSelect = document.getElementById(SEARCH_BUSINESS_TYPE_ID);
        searchProfitCenterSelect = document.getElementById(SEARCH_PROFIT_CENTER_ID);
        searchStageSelect = document.getElementById(SEARCH_STAGE_ID);
        searchStatusSelect = document.getElementById(SEARCH_STATUS_ID);
        searchBtn = document.getElementById(SEARCH_BTN_ID);

        if (!tableBody || !searchNameInput /* ... other checks ... */ || !searchBtn) {
            console.error('ProjectListModule: Required DOM elements not found.');
            return;
        }

        // 绑定搜索事件
        searchBtn.addEventListener('click', handleSearch);
        [searchNameInput, searchTagsSelect, searchBusinessTypeSelect, searchProfitCenterSelect, searchStageSelect, searchStatusSelect].forEach(element => {
            const eventType = (element.multiple || element.type === 'text' || element.type === 'search') ? 'input' : 'change';
            element.addEventListener(eventType, () => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(handleSearch, 500);
            });
            if (element.multiple) {
                element.addEventListener('change', () => {
                    clearTimeout(debounceTimer);
                    debounceTimer = setTimeout(handleSearch, 500);
                });
            }
        });

        // *** 修改: 为表格体添加点击事件监听器，处理行点击展开/收起 ***
        tableBody.addEventListener('click', handleProjectRowClick);

        fetchProjectsList(); // 加载初始数据
        console.log('ProjectListModule initialized.');
    }

    // 处理搜索事件
    function handleSearch() {
        currentPage = 1;
        currentFilters = getFilters();
        fetchProjectsList();
    }

    // 从搜索栏获取过滤条件
    function getFilters() {
        const filters = {};
        filters.nameFilter = searchNameInput.value.trim();
        const selectedTagOptions = Array.from(searchTagsSelect.selectedOptions);
        filters.tagIds = selectedTagOptions.map(option => option.value).filter(value => value);
        filters.businessTypeName = searchBusinessTypeSelect.value;
        filters.profitCenterZone = searchProfitCenterSelect.value;
        filters.stageFilter = searchStageSelect.value; // 可能需要转换为 stageId
        filters.statusFilter = searchStatusSelect.value;
        console.debug("Current Filters:", filters);
        return filters;
    }

    // 获取并渲染项目列表
    async function fetchProjectsList() {
        // 关闭当前可能展开的行
        collapseProjectRow(currentlyExpandedProjectId);
        currentlyExpandedProjectId = null;

        AppUtils.showLoading(tableBody);
        const params = {pageNum: currentPage, pageSize: pageSize, ...currentFilters};
        try {
            const response = await ProjectApiModule.fetchProjects(params);
            totalItems = response.total;
            totalPages = Math.ceil(totalItems / pageSize);
            renderTable(response.data); // 传递数据用于渲染
            renderPagination();
        } catch (error) {
            console.error('Error fetching project list:', error);
            tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-red-500">加载项目列表失败: ${error.message || '请稍后重试'}</td></tr>`; // Colspan 更新为 10
            renderPagination(); // 即使出错也渲染分页（显示0页）
        } finally {
            AppUtils.hideLoading(tableBody);
        }
    }

    // 渲染表格数据 (添加序号)
    function renderTable(projects) {
        tableBody.innerHTML = '';
        if (!projects || projects.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="10" class="text-center py-4 text-gray-500">没有找到符合条件的项目。</td></tr>'; // Colspan 更新为 10
            return;
        }
        projects.forEach((project, index) => {
            const sequenceNumber = (currentPage - 1) * pageSize + index + 1; // 计算序号
            const row = createProjectRow(project, sequenceNumber); // 传递序号
            tableBody.appendChild(row);
            const taskRow = document.createElement('tr');
            taskRow.classList.add('task-details-row');
            taskRow.dataset.projectId = project.projectId;
            taskRow.style.display = 'none';
            taskRow.innerHTML = `<td colspan="10"><div class="task-container loading p-4">加载任务中...</div></td>`; // Colspan 更新为 10
            tableBody.appendChild(taskRow);
        });
    }

    // 创建项目行 HTML
    function createProjectRow(project, sequenceNumber) {
        const row = document.createElement('tr');
        row.dataset.projectId = project.projectId;
        row.classList.add('project-row'); // 添加类方便选择

        // const toggleCell = document.createElement('td');
        // toggleCell.classList.add('px-3', 'py-2', 'text-center'); // 添加 Tailwind 样式
        // const toggleButton = document.createElement('button');
        // toggleButton.classList.add('btn-icon', 'toggle-tasks-btn');
        // toggleButton.innerHTML = '<i class="icon-chevron-right"></i>';
        // toggleCell.appendChild(toggleButton);

        const tagsHtml = (project.tags && project.tags.length > 0)
            ? project.tags.map(tag => `<span class="tag">${AppUtils.escapeHTML(tag.tagName)}</span>`).join(' ') : '-';
        const statusClass = getStatusClass(project.projectStatus);
        const statusHtml = `<span class="status ${statusClass}">${AppUtils.escapeHTML(project.projectStatus ?? '未知')}</span>`;

        // TODO 查询当前项目中总任务数和已完成任务数，计算完成比例后赋值给progressValue。
        // 随机进度条仅为示例，应替换为真实数据
        const progressValue = Math.floor(Math.random() * 101); // 0-100
        const progressColorClass = progressValue === 100 ? 'bg-green-500' : (progressValue > 0 ? 'bg-blue-500' : 'bg-gray-500');
        const progressHtml = `<div class="progress-bar mx-auto"><div class="${progressColorClass}" style="width: ${progressValue}%;"></div></div>`;

        row.innerHTML = `
            <td class="px-3 py-2 text-center text-gray-500">${sequenceNumber}</td> <td class="px-3 py-2 whitespace-nowrap" data-field="projectName">${AppUtils.escapeHTML(project.projectName ?? '')}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="createdAt">${AppUtils.formatDateIfPresent(project.createdAt)}</td>
            <td class="px-3 py-2" data-field="tags">${tagsHtml}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="businessTypeName">${AppUtils.escapeHTML(project.businessTypeName ?? '-')}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="profitCenterZone">${AppUtils.escapeHTML(AppUtils.getShortName(project.profitCenterZone,'基础业务-') ?? '-')}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="currentStageName">${AppUtils.escapeHTML(project.currentStageName ?? '-')}</td>
            <td class="px-3 py-2 text-center" data-field="projectStatus">${statusHtml}</td>
            <td class="px-3 py-2 text-center" data-field="progress">${progressHtml}</td>
            <td class="px-3 py-2 text-center whitespace-nowrap action-cell"> <button class="btn btn-xs btn-edit edit-project-btn">编辑</button>
                <button class="btn btn-xs btn-delete delete-project-btn ml-1">删除</button>
            </td>
        `;
        return row;
    }

    // *** 新增: 处理项目行点击事件 ***
    function handleProjectRowClick(event) {
        const target = event.target;
        // 检查点击的是否在操作单元格内，或者是否是 input/select/textarea/button/a 等交互元素
        if (target.closest('.action-cell') || target.closest('input, select, textarea, button, a')) {
            console.debug('Clicked on interactive element or action cell, ignoring row toggle.');
            return; // 如果是交互元素或操作单元格，则不触发展开/收起
        }

        const projectRow = target.closest('tr.project-row'); // 确保是项目数据行
        if (!projectRow) return;

        const projectId = projectRow.dataset.projectId;
        console.debug(`Project row clicked, toggling project ID: ${projectId}`);
        toggleProjectRow(projectId);
    }

    // *** 新增: 切换项目行展开/收起状态 ***
    function toggleProjectRow(projectId) {
        if (!projectId) return;
        if (currentlyExpandedProjectId === projectId) { // 点击的是已展开行 -> 收起
            collapseProjectRow(projectId);
            currentlyExpandedProjectId = null;
        } else { // 点击的是未展开行 -> 展开
            collapseProjectRow(currentlyExpandedProjectId); // 先收起其他行
            expandProjectRow(projectId); // 再展开当前行
            currentlyExpandedProjectId = projectId;
        }
    }

    // *** 新增: 展开指定项目行 ***
    function expandProjectRow(projectId) {
        if (!projectId) return;
        const projectRow = tableBody.querySelector(`tr.project-row[data-project-id="${projectId}"]`);
        const taskDetailsRow = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"]`);
        if (projectRow && taskDetailsRow) {
            projectRow.classList.add('expanded-project-row'); // 添加展开样式
            taskDetailsRow.style.display = ''; // 显示任务行 (默认为 table-row)
            const taskContainer = taskDetailsRow.querySelector('.task-container');
            if (taskContainer && !taskContainer.classList.contains('loaded')) {
                // 确保 ProjectTasksModule 和其方法可用
                if (typeof ProjectTasksModule !== 'undefined' && typeof ProjectTasksModule.loadTasksForProject === 'function') {
                    ProjectTasksModule.loadTasksForProject(projectId, taskContainer);
                } else {
                    console.error("ProjectTasksModule.loadTasksForProject function is not available.");
                    taskContainer.innerHTML = '<div class="error-message">无法加载任务数据 (函数未找到)</div>';
                }
            }
        }
    }

    // *** 新增: 收起指定项目行 ***
    function collapseProjectRow(projectId) {
        if (!projectId) return;
        const projectRow = tableBody.querySelector(`tr.project-row[data-project-id="${projectId}"]`);
        const taskDetailsRow = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"]`);
        if (projectRow && taskDetailsRow) {
            projectRow.classList.remove('expanded-project-row'); // 移除展开样式
            taskDetailsRow.style.display = 'none'; // 隐藏任务行
            // 销毁 Flatpickr 实例
            if (typeof ProjectTasksModule !== 'undefined' && typeof ProjectTasksModule.destroyFlatpickrInstancesForRow === 'function') {
                ProjectTasksModule.destroyFlatpickrInstancesForRow(taskDetailsRow);
            }
        }
    }

    // 根据项目状态获取对应的 CSS 类
    function getStatusClass(status) {
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

    // 渲染分页控件 (修正调用方式)
    function renderPagination() {
        // *** 修正开始 ***
        // 准备传递给 setupPagination 的 options 对象
        const paginationOptions = {
            containerId: PAGINATION_ID, // 传递容器 ID 字符串
            currentPage: currentPage,
            totalPages: totalPages, // 传递计算好的总页数
            totalRecords: totalItems,
            // maxPagesToShow: 7, // 可以自定义显示的页码按钮数量
            onPageChange: (page) => { // 传递回调函数
                if (currentPage !== page) {
                    currentPage = page;
                    fetchProjectsList(); // 页面改变时重新获取数据
                }
            }
        };
        // 调用 AppUtils 上的 setupPagination 方法
        AppUtils.setupPagination(paginationOptions);
        // *** 修正结束 ***
    }

    // 暴露公共接口
    return {
        init: init,
        refresh: fetchProjectsList, // 提供一个刷新列表的方法给其他模块调用
        createProjectRow: createProjectRow, // 暴露给 CRUD 模块使用
        getStatusClass: getStatusClass    // 暴露给 CRUD 模块使用
    };
})();
