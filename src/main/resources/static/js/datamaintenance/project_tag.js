/**
 * 文件路径: src/main/resources/static/js/datamaintenance/project_tag.js
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 处理数据维护页面中“项目标签维护”部分的交互逻辑，包括数据显示、搜索、分页、增删改。
 */
const ProjectTagModule = (() => {
    // 常量定义
    const API_BASE_URL = '/api/project-tags';
    const TABLE_BODY_ID = 'project-tag-table-body';
    const PAGINATION_ID = 'project-tag-pagination';
    const SEARCH_INPUT_ID = 'search-project-tag-name';
    const SEARCH_BTN_ID = 'search-project-tag-btn';
    const ADD_BTN_ID = 'add-project-tag-btn';

    // DOM 元素引用
    let tableBody;
    let paginationContainer;
    let searchInput;
    let searchBtn;
    let addBtn;

    // 状态变量
    let currentPage = 1;
    const pageSize = 10; // 或者从配置读取
    let currentFilter = '';
    let totalItems = 0;

    // 初始化函数
    function init() {
        console.log('Initializing ProjectTagModule...');
        // 获取 DOM 元素
        tableBody = document.getElementById(TABLE_BODY_ID);
        paginationContainer = document.getElementById(PAGINATION_ID);
        searchInput = document.getElementById(SEARCH_INPUT_ID);
        searchBtn = document.getElementById(SEARCH_BTN_ID);
        addBtn = document.getElementById(ADD_BTN_ID);

        if (!tableBody || !paginationContainer || !searchInput || !searchBtn || !addBtn) {
            console.error('ProjectTagModule: Required DOM elements not found.');
            return;
        }

        // 绑定事件监听器
        searchBtn.addEventListener('click', handleSearch);
        searchInput.addEventListener('keyup', (event) => {
            if (event.key === 'Enter') {
                handleSearch();
            }
        });
        addBtn.addEventListener('click', handleAdd);
        // 使用事件委托处理表格行的编辑和删除按钮点击
        tableBody.addEventListener('click', handleTableButtonClick);

        // 加载初始数据
        fetchTags();
        console.log('ProjectTagModule initialized.');
    }

    // 处理搜索
    function handleSearch() {
        currentFilter = searchInput.value.trim();
        currentPage = 1; // 重置到第一页
        fetchTags();
    }

    // 处理添加按钮点击 (行内添加)
    function handleAdd() {
        // 检查是否已存在编辑行
        if (tableBody.querySelector('.editing-row')) {
            AppUtils.showMessage('请先完成当前编辑的行。', 'warning');
            return;
        }

        const newRow = createEditableRow({}); // 创建一个空的编辑行
        tableBody.insertBefore(newRow, tableBody.firstChild); // 插入到表格顶部
    }

    // 处理表格按钮点击 (编辑/删除/保存/取消)
    function handleTableButtonClick(event) {
        const target = event.target;
        const row = target.closest('tr');
        if (!row) return;

        const tagId = row.dataset.id; // ID 可能是字符串，需要转为 Long

        if (target.classList.contains('edit-btn')) {
            handleEdit(row, tagId);
        } else if (target.classList.contains('delete-btn')) {
            handleDelete(row, tagId);
        } else if (target.classList.contains('save-btn')) {
            handleSave(row, tagId); // tagId 可能为 undefined (新增时)
        } else if (target.classList.contains('cancel-btn')) {
            handleCancel(row, tagId); // tagId 可能为 undefined (新增时)
        }
    }

    // 处理编辑按钮点击
    function handleEdit(row, tagId) {
        if (tableBody.querySelector('.editing-row') && !row.classList.contains('editing-row')) {
            AppUtils.showMessage('请先完成当前编辑的行。', 'warning');
            return;
        }
        const tagData = getRowData(row);
        const editableRow = createEditableRow(tagData, tagId);
        row.parentNode.replaceChild(editableRow, row);
    }

    // 处理删除按钮点击
    async function handleDelete(row, tagId) {
        if (!tagId) return;

        if (!confirm(`确定要删除 ID 为 ${tagId} 的项目标签吗？删除后关联的项目将失去此标签。`)) {
            return;
        }

        AppUtils.showLoading();
        try {
            await AppUtils.delete(`${API_BASE_URL}/${tagId}`);
            AppUtils.showMessage('删除成功！', 'success');
            fetchTags(); // 重新加载数据
        } catch (error) {
            console.error('Error deleting project tag:', error);
        } finally {
            AppUtils.hideLoading();
        }
    }

    // 处理保存按钮点击 (新增或更新)
    async function handleSave(row, tagId) {
        const tagData = getEditableRowData(row);

        if (!tagData.tagName) {
            AppUtils.showMessage('标签名称不能为空。', 'error');
            return;
        }

        AppUtils.showLoading();
        try {
            let result;
            if (tagId) { // 更新
                result = await AppUtils.put(`${API_BASE_URL}/${tagId}`, tagData);
                AppUtils.showMessage('更新成功！', 'success');
            } else { // 新增
                result = await AppUtils.post(API_BASE_URL, tagData);
                AppUtils.showMessage('添加成功！', 'success');
            }
            fetchTags();
        } catch (error) {
            console.error('Error saving project tag:', error);
            // API Controller 层会处理冲突 (409)，AppUtils 会显示错误消息
        } finally {
            AppUtils.hideLoading();
        }
    }

    // 处理取消按钮点击
    function handleCancel(row, tagId) {
        if (tagId) { // 取消编辑
            fetchAndReplaceRow(row, tagId);
        } else { // 取消新增
            row.remove();
        }
    }

    // 重新获取单行数据并替换编辑行
    async function fetchAndReplaceRow(row, tagId) {
        AppUtils.showLoading();
        try {
            const tag = await AppUtils.get(`${API_BASE_URL}/${tagId}`);
            if (tag) {
                const displayRow = createDisplayRow(tag);
                row.parentNode.replaceChild(displayRow, row);
            } else {
                row.remove();
                AppUtils.showMessage(`标签 ID ${tagId} 可能已被删除。`, 'warning');
            }
        } catch (error) {
            console.error(`Error fetching tag ${tagId} for cancel:`, error);
            AppUtils.showMessage('无法恢复原始数据，请重试。', 'error');
        } finally {
            AppUtils.hideLoading();
        }
    }


    // 获取 API 数据
    async function fetchTags() {
        AppUtils.showLoading(tableBody);
        const params = {
            pageNum: currentPage,
            pageSize: pageSize,
            nameFilter: currentFilter
        };
        try {
            const response = await AppUtils.get(API_BASE_URL, params);
            totalItems = response.total;
            renderTable(response.data);
            renderPagination();
        } catch (error) {
            console.error('Error fetching project tags:', error);
            tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-red-500">加载数据失败，请稍后重试。</td></tr>';
            paginationContainer.innerHTML = '';
        } finally {
            AppUtils.hideLoading(tableBody);
        }
    }

    // 渲染表格数据
    function renderTable(tags) {
        tableBody.innerHTML = ''; // 清空现有内容
        if (!tags || tags.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" class="text-center">没有找到符合条件的项目标签。</td></tr>';
            return;
        }
        tags.forEach(tag => {
            const row = createDisplayRow(tag);
            tableBody.appendChild(row);
        });
    }

    // 创建显示行
    function createDisplayRow(tag) {
        const row = document.createElement('tr');
        row.dataset.id = tag.tagId;
        row.innerHTML = `
            <td>${tag.tagId}</td>
            <td>${AppUtils.escapeHTML(tag.tagName ?? '')}</td>
            <td>${AppUtils.formatDateIfPresent(tag.createdAt)}</td>
            <td>${AppUtils.formatDateIfPresent(tag.updatedAt)}</td>
            <td>
                <button class="btn btn-sm btn-edit edit-btn">编辑</button>
                <button class="btn btn-sm btn-delete delete-btn">删除</button>
            </td>
        `;
        return row;
    }

    // 创建编辑行
    function createEditableRow(tag, tagId = null) {
        const row = document.createElement('tr');
        row.classList.add('editing-row');
        if (tagId) {
            row.dataset.id = tagId;
        }
        row.innerHTML = `
            <td>${tagId ?? '新增'}</td>
            <td><input type="text" name="tagName" value="${AppUtils.escapeHTML(tag.tagName ?? '')}" required maxlength="100" class="form-input"></td>
            <td>${tagId ? AppUtils.formatDateIfPresent(tag.createdAt) : '-'}</td>
            <td>${tagId ? AppUtils.formatDateIfPresent(tag.updatedAt) : '-'}</td>
            <td>
                <button class="btn btn-sm btn-save save-btn">保存</button>
                <button class="btn btn-sm btn-cancel cancel-btn">取消</button>
            </td>
        `;
        return row;
    }

    // 从显示行提取数据 (简化)
    function getRowData(row) {
        const cells = row.querySelectorAll('td');
        return {
            tagId: parseInt(row.dataset.id),
            tagName: cells[1].textContent,
            createdAt: cells[2].textContent,
            updatedAt: cells[3].textContent
        };
    }

    // 从编辑行提取数据
    function getEditableRowData(row) {
        const nameInput = row.querySelector('input[name="tagName"]');
        return {
            tagName: nameInput ? nameInput.value.trim() : ''
        };
    }

    // 渲染分页控件
    function renderPagination() {
        AppUtils.setupPagination(PAGINATION_ID, currentPage, pageSize, totalItems, (page) => {
            currentPage = page;
            fetchTags();
        });
    }

    // 返回公共接口
    return {
        init: init
    };
})();
