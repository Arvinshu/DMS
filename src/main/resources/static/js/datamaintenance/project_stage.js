/**
 * 文件路径: src/main/resources/static/js/datamaintenance/project_stage.js
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 处理数据维护页面中“项目阶段维护”部分的交互逻辑，包括数据显示、搜索、分页、增删改。
 */
const ProjectStageModule = (() => {
    // 常量定义
    const API_BASE_URL = '/api/project-stages';
    const TABLE_BODY_ID = 'project-stage-table-body';
    const PAGINATION_ID = 'project-stage-pagination';
    const SEARCH_INPUT_ID = 'search-project-stage-name';
    const SEARCH_BTN_ID = 'search-project-stage-btn';
    const ADD_BTN_ID = 'add-project-stage-btn';

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
        console.log('Initializing ProjectStageModule...');
        // 获取 DOM 元素
        tableBody = document.getElementById(TABLE_BODY_ID);
        paginationContainer = document.getElementById(PAGINATION_ID);
        searchInput = document.getElementById(SEARCH_INPUT_ID);
        searchBtn = document.getElementById(SEARCH_BTN_ID);
        addBtn = document.getElementById(ADD_BTN_ID);

        if (!tableBody || !paginationContainer || !searchInput || !searchBtn || !addBtn) {
            console.error('ProjectStageModule: Required DOM elements not found.');
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
        fetchStages();
        console.log('ProjectStageModule initialized.');
    }

    // 处理搜索
    function handleSearch() {
        currentFilter = searchInput.value.trim();
        currentPage = 1; // 重置到第一页
        fetchStages();
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

        const stageId = row.dataset.id;

        if (target.classList.contains('edit-btn')) {
            handleEdit(row, stageId);
        } else if (target.classList.contains('delete-btn')) {
            handleDelete(row, stageId);
        } else if (target.classList.contains('save-btn')) {
            handleSave(row, stageId); // stageId 可能为 undefined (新增时)
        } else if (target.classList.contains('cancel-btn')) {
            handleCancel(row, stageId); // stageId 可能为 undefined (新增时)
        }
    }

    // 处理编辑按钮点击
    function handleEdit(row, stageId) {
        // 检查是否已存在编辑行
        if (tableBody.querySelector('.editing-row') && !row.classList.contains('editing-row')) {
            AppUtils.showMessage('请先完成当前编辑的行。', 'warning');
            return;
        }
        // 将显示行转换为编辑行
        const stageData = getRowData(row); // 从当前行获取数据
        const editableRow = createEditableRow(stageData, stageId);
        row.parentNode.replaceChild(editableRow, row);
    }

    // 处理删除按钮点击
    async function handleDelete(row, stageId) {
        if (!stageId) return; // 不处理新增未保存的行

        if (!confirm(`确定要删除 ID 为 ${stageId} 的项目阶段吗？`)) {
            return;
        }

        AppUtils.showLoading();
        try {
            await AppUtils.delete(`${API_BASE_URL}/${stageId}`);
            AppUtils.showMessage('删除成功！', 'success');
            fetchStages(); // 重新加载数据
        } catch (error) {
            console.error('Error deleting project stage:', error);
            // AppUtils.showMessage 在 AppUtils.delete 中通常会处理
        } finally {
            AppUtils.hideLoading();
        }
    }

    // 处理保存按钮点击 (新增或更新)
    async function handleSave(row, stageId) {
        const stageData = getEditableRowData(row);

        // 基本前端验证 (更严格的验证应在后端完成)
        if (!stageData.stageName || !stageData.stageOrder) {
            AppUtils.showMessage('阶段名称和排序号不能为空。', 'error');
            return;
        }
        if (isNaN(parseInt(stageData.stageOrder))) {
            AppUtils.showMessage('排序号必须是数字。', 'error');
            return;
        }

        AppUtils.showLoading();
        try {
            let result;
            if (stageId) { // 更新
                result = await AppUtils.put(`${API_BASE_URL}/${stageId}`, stageData);
                AppUtils.showMessage('更新成功！', 'success');
            } else { // 新增
                result = await AppUtils.post(API_BASE_URL, stageData);
                AppUtils.showMessage('添加成功！', 'success');
            }
            // 刷新列表，保留当前页和过滤器
            fetchStages();
            // 注意：如果添加成功，可能需要跳转到第一页或保持当前页，取决于需求
            // 这里简单刷新当前页
        } catch (error) {
            console.error('Error saving project stage:', error);
            // AppUtils.showMessage 通常已在 AppUtils 中处理
        } finally {
            AppUtils.hideLoading();
        }
    }

    // 处理取消按钮点击
    function handleCancel(row, stageId) {
        if (stageId) { // 取消编辑
            // 重新获取原始数据并渲染回显示行
            fetchAndReplaceRow(row, stageId);
        } else { // 取消新增
            row.remove(); // 直接移除新增的编辑行
        }
    }

    // 重新获取单行数据并替换编辑行
    async function fetchAndReplaceRow(row, stageId) {
        AppUtils.showLoading();
        try {
            const stage = await AppUtils.get(`${API_BASE_URL}/${stageId}`);
            if (stage) {
                const displayRow = createDisplayRow(stage);
                row.parentNode.replaceChild(displayRow, row);
            } else {
                // 如果获取失败（例如已被删除），则直接移除行
                row.remove();
                AppUtils.showMessage(`阶段 ID ${stageId} 可能已被删除。`, 'warning');
            }
        } catch (error) {
            console.error(`Error fetching stage ${stageId} for cancel:`, error);
            // 保留编辑行，让用户重试或手动刷新
            AppUtils.showMessage('无法恢复原始数据，请重试。', 'error');
        } finally {
            AppUtils.hideLoading();
        }
    }


    // 获取 API 数据
    async function fetchStages() {
        AppUtils.showLoading(tableBody); // 在表格区域显示加载指示
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
            console.error('Error fetching project stages:', error);
            tableBody.innerHTML = '<tr><td colspan="8" class="text-center text-red-500">加载数据失败，请稍后重试。</td></tr>';
            paginationContainer.innerHTML = ''; // 清空分页
        } finally {
            AppUtils.hideLoading(tableBody);
        }
    }

    // 渲染表格数据
    function renderTable(stages) {
        tableBody.innerHTML = ''; // 清空现有内容
        if (!stages || stages.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="8" class="text-center">没有找到符合条件的项目阶段。</td></tr>';
            return;
        }
        stages.forEach(stage => {
            const row = createDisplayRow(stage);
            tableBody.appendChild(row);
        });
    }

    // 创建显示行 (Read-only Row)
    function createDisplayRow(stage) {
        const row = document.createElement('tr');
        row.dataset.id = stage.stageId; // 存储 ID
        row.innerHTML = `
            <td>${stage.stageId}</td>
            <td>${stage.stageOrder ?? ''}</td>
            <td>${AppUtils.escapeHTML(stage.stageName ?? '')}</td>
            <td>${AppUtils.escapeHTML(stage.stageDescription ?? '')}</td>
            <td>${stage.isEnabled ? '是' : '否'}</td>
            <td>${AppUtils.formatDateIfPresent(stage.createdAt)}</td>
            <td>${AppUtils.formatDateIfPresent(stage.updatedAt)}</td>
            <td>
                <button class="btn btn-sm btn-edit edit-btn">编辑</button>
                <button class="btn btn-sm btn-delete delete-btn">删除</button>
            </td>
        `;
        return row;
    }

    // 创建编辑行 (Editable Row)
    function createEditableRow(stage, stageId = null) {
        const row = document.createElement('tr');
        row.classList.add('editing-row'); // 标记为编辑行
        if (stageId) {
            row.dataset.id = stageId; // 存储 ID (如果是编辑)
        }
        row.innerHTML = `
            <td>${stageId ?? '新增'}</td>
            <td><input type="number" name="stageOrder" value="${stage.stageOrder ?? ''}" required class="form-input"></td>
            <td><input type="text" name="stageName" value="${AppUtils.escapeHTML(stage.stageName ?? '')}" required maxlength="100" class="form-input"></td>
            <td><textarea name="stageDescription" class="form-textarea">${AppUtils.escapeHTML(stage.stageDescription ?? '')}</textarea></td>
            <td>
                <select name="isEnabled" class="form-select">
                    <option value="true" ${stage.isEnabled === true ? 'selected' : ''}>是</option>
                    <option value="false" ${stage.isEnabled === false ? 'selected' : ''}>否</option>
                </select>
            </td>
            <td>${stageId ? AppUtils.formatDateIfPresent(stage.createdAt) : '-'}</td>
            <td>${stageId ? AppUtils.formatDateIfPresent(stage.updatedAt) : '-'}</td>
            <td>
                <button class="btn btn-sm btn-save save-btn">保存</button>
                <button class="btn btn-sm btn-cancel cancel-btn">取消</button>
            </td>
        `;
        return row;
    }

    // 从显示行提取数据
    function getRowData(row) {
        // 注意：需要根据 createDisplayRow 的结构来解析，或者最好是从 API 重新获取最新数据
        // 这里简化处理，假设可以从单元格文本获取（不推荐用于复杂数据）
        const cells = row.querySelectorAll('td');
        return {
            stageId: parseInt(row.dataset.id),
            stageOrder: parseInt(cells[1].textContent),
            stageName: cells[2].textContent,
            stageDescription: cells[3].textContent,
            isEnabled: cells[4].textContent === '是',
            createdAt: cells[5].textContent, // 保持字符串格式或 null
            updatedAt: cells[6].textContent
        };
    }


    // 从编辑行提取数据
    function getEditableRowData(row) {
        const orderInput = row.querySelector('input[name="stageOrder"]');
        const nameInput = row.querySelector('input[name="stageName"]');
        const descInput = row.querySelector('textarea[name="stageDescription"]');
        const enabledSelect = row.querySelector('select[name="isEnabled"]');
        return {
            stageOrder: orderInput ? parseInt(orderInput.value) : null,
            stageName: nameInput ? nameInput.value.trim() : '',
            stageDescription: descInput ? descInput.value.trim() : '',
            isEnabled: enabledSelect ? enabledSelect.value === 'true' : null
        };
    }


    // 渲染分页控件
    function renderPagination() {
        AppUtils.setupPagination(PAGINATION_ID, currentPage, pageSize, totalItems, (page) => {
            currentPage = page;
            fetchStages();
        });
    }

    // 返回公共接口
    return {
        init: init
    };
})();
