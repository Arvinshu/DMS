/**
 * 文件路径: src/main/resources/static/js/project/project_list.js
 * 开发时间: 2025-04-24 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 处理项目管理页面中项目列表的获取、渲染、搜索和分页逻辑。
 * 更新内容: 修正 renderPagination 中调用 AppUtils.setupPagination 的方式，传递 options 对象。
 * 依赖: common.js (AppUtils), project_api.js (ProjectApiModule), pagination.js (via AppUtils)
 */
const ProjectListModule = (() => {
    // 常量定义
    const TABLE_BODY_ID = 'project-table-body';
    const PAGINATION_ID = 'project-pagination'; // 分页控件容器的 ID
    // 搜索栏元素 ID
    const SEARCH_NAME_ID = 'search-project-name';
    const SEARCH_TAGS_ID = 'search-project-tags';
    const SEARCH_BUSINESS_TYPE_ID = 'search-business-type';
    const SEARCH_PROFIT_CENTER_ID = 'search-profit-center';
    const SEARCH_STAGE_ID = 'search-project-stage';
    const SEARCH_STATUS_ID = 'search-project-status';
    const SEARCH_BTN_ID = 'search-project-btn';

    // DOM 元素引用
    let tableBody;
    let paginationContainer; // 这个变量现在可能不需要全局存储了，因为 ID 在调用时传入
    let searchNameInput;
    let searchTagsSelect;
    let searchBusinessTypeSelect;
    let searchProfitCenterSelect;
    let searchStageSelect;
    let searchStatusSelect;
    let searchBtn;

    // 状态变量
    let currentPage = 1;
    const pageSize = 50;
    let currentFilters = {};
    let totalItems = 0;
    let totalPages = 1; // 新增，用于传递给分页组件
    let debounceTimer;

    // 初始化函数
    function init() {
        console.log('Initializing ProjectListModule...');
        tableBody = document.getElementById(TABLE_BODY_ID);
        // paginationContainer = document.getElementById(PAGINATION_ID); // 获取容器元素本身可能不再需要
        searchNameInput = document.getElementById(SEARCH_NAME_ID);
        searchTagsSelect = document.getElementById(SEARCH_TAGS_ID);
        searchBusinessTypeSelect = document.getElementById(SEARCH_BUSINESS_TYPE_ID);
        searchProfitCenterSelect = document.getElementById(SEARCH_PROFIT_CENTER_ID);
        searchStageSelect = document.getElementById(SEARCH_STAGE_ID);
        searchStatusSelect = document.getElementById(SEARCH_STATUS_ID);
        searchBtn = document.getElementById(SEARCH_BTN_ID);

        if (!tableBody || !searchNameInput || !searchTagsSelect ||
            !searchBusinessTypeSelect || !searchProfitCenterSelect || !searchStageSelect ||
            !searchStatusSelect || !searchBtn) {
            console.error('ProjectListModule: Required DOM elements not found.');
            return;
        }

        searchBtn.addEventListener('click', handleSearch);
        [searchNameInput, searchTagsSelect, searchBusinessTypeSelect, searchProfitCenterSelect, searchStageSelect, searchStatusSelect].forEach(element => {
            const eventType = (element.multiple || element.type === 'text' || element.type === 'search') ? 'input' : 'change';
            element.addEventListener(eventType, () => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(handleSearch, 500);
            });
            if(element.multiple) { // Re-add change listener for multi-select if needed by specific libraries
                element.addEventListener('change', () => {
                    clearTimeout(debounceTimer);
                    debounceTimer = setTimeout(handleSearch, 500);
                });
            }
        });

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
        AppUtils.showLoading(tableBody);
        const params = {
            pageNum: currentPage,
            pageSize: pageSize,
            ...currentFilters
        };

        try {
            const response = await ProjectApiModule.fetchProjects(params);
            totalItems = response.total;
            totalPages = Math.ceil(totalItems / pageSize); // 计算总页数
            renderTable(response.data);
            renderPagination(); // 调用更新后的分页渲染
        } catch (error) {
            console.error('Error fetching project list:', error);
            tableBody.innerHTML = `<tr><td colspan="10" class="text-center text-red-500">加载项目列表失败: ${error.message || '请稍后重试'}</td></tr>`;
            // 即使出错，也尝试渲染一个空的分页（或错误提示）
            renderPagination(); // 传递当前状态（可能是0页）
        } finally {
            AppUtils.hideLoading(tableBody);
        }
    }

    // 渲染表格数据
    function renderTable(projects) {
        tableBody.innerHTML = '';
        if (!projects || projects.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="10" class="text-center py-4 text-gray-500">没有找到符合条件的项目。</td></tr>'; // 添加内边距和颜色
            return;
        }
        projects.forEach(project => {
            const row = createProjectRow(project);
            tableBody.appendChild(row);
            const taskRow = document.createElement('tr');
            taskRow.classList.add('task-details-row');
            taskRow.dataset.projectId = project.projectId;
            taskRow.style.display = 'none';
            taskRow.innerHTML = `<td colspan="10"><div class="task-container loading p-4">加载任务中...</div></td>`; // 添加内边距
            tableBody.appendChild(taskRow);
        });
    }

    // 创建项目行 HTML
    function createProjectRow(project) {
        const row = document.createElement('tr');
        row.dataset.projectId = project.projectId;
        row.classList.add('hover:bg-gray-50'); // 添加悬停效果

        const toggleCell = document.createElement('td');
        toggleCell.classList.add('px-3', 'py-2', 'text-center'); // 添加 Tailwind 样式
        const toggleButton = document.createElement('button');
        toggleButton.classList.add('btn-icon', 'toggle-tasks-btn');
        toggleButton.innerHTML = '<i class="icon-chevron-right"></i>';
        toggleCell.appendChild(toggleButton);

        const tagsHtml = (project.tags && project.tags.length > 0)
            ? project.tags.map(tag => `<span class="tag">${AppUtils.escapeHTML(tag.tagName)}</span>`).join(' ')
            : '-';

        const statusClass = getStatusClass(project.projectStatus);
        const statusHtml = `<span class="status ${statusClass}">${AppUtils.escapeHTML(project.projectStatus ?? '未知')}</span>`;

        // TODO 查询当前项目中总任务数和已完成任务数，计算完成比例后赋值给progressValue。
        // 随机进度条仅为示例，应替换为真实数据
        const progressValue = Math.floor(Math.random() * 101); // 0-100
        const progressColorClass = progressValue === 100 ? 'bg-green-500' : (progressValue > 0 ? 'bg-blue-500' : 'bg-gray-500');
        const progressHtml = `<div class="progress-bar mx-auto"><div class="${progressColorClass}" style="width: ${progressValue}%;"></div></div>`;


        row.innerHTML = `
            ${toggleCell.outerHTML}
            <td class="px-3 py-2 whitespace-nowrap" data-field="projectName">${AppUtils.escapeHTML(project.projectName ?? '')}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="createdAt">${AppUtils.formatDateIfPresent(project.createdAt)}</td>
            <td class="px-3 py-2" data-field="tags">${tagsHtml}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="businessTypeName">${AppUtils.escapeHTML(project.businessTypeName ?? '-')}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="profitCenterZone">${AppUtils.escapeHTML(project.profitCenterZone ?? '-')}</td>
            <td class="px-3 py-2 whitespace-nowrap" data-field="currentStageName">${AppUtils.escapeHTML(project.currentStageName ?? '-')}</td>
            <td class="px-3 py-2 text-center" data-field="projectStatus">${statusHtml}</td>
            <td class="px-3 py-2 text-center" data-field="progress">${progressHtml}</td>
            <td class="px-3 py-2 text-center whitespace-nowrap">
                <button class="btn btn-xs btn-edit edit-project-btn">编辑</button>
                <button class="btn btn-xs btn-delete delete-project-btn ml-1">删除</button> </td>
        `;
        return row;
    }

    // 根据项目状态获取对应的 CSS 类
    function getStatusClass(status) {
        switch (status) {
            case '待办': return 'status-todo';
            case '进行中': return 'status-inprogress';
            case '已完成': return 'status-completed';
            case '已暂停': return 'status-onhold';
            case '已取消': return 'status-cancelled';
            default: return 'status-unknown';
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
