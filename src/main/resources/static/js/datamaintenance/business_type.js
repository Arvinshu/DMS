/**
 * 业务类型管理模块脚本
 * 文件路径: src/main/resources/static/js/datamaintenance/business_type.js
 * 职责：处理业务类型数据的获取、显示、过滤、分页、新增、编辑、删除操作。
 * 修正：布尔值显示、清空筛选按钮、保存日志和调试。
 * 依赖: common.js, pagination.js
 */

(function(window) {
    'use strict';

    // 创建或获取模块命名空间
    const BusinessTypeModule = window.BusinessTypeModule || {};

    // --- 配置和常量 ---
    const API_BASE_URL = '/api/business-types';
    const TABLE_BODY_ID = 'business-types-table-body';
    const PAGINATION_CONTAINER_ID = 'bt-pagination-container';
    const FILTER_FORM_ID = 'bt-filters';
    const ADD_BUTTON_ID = 'add-bt-button';
    const CLEAR_BUTTON_ID = 'bt-clear-filters'; // 清空按钮 ID
    const DEFAULT_PAGE_SIZE = 50;
    const DEBOUNCE_DELAY = 300;

    // --- DOM 元素引用 ---
    let tableBody = null;
    let filterInputs = null;
    let addButton = null;
    let clearButton = null; // 清空按钮

    // --- 状态变量 ---
    let currentPage = 1;
    let currentFilters = {};

    // --- 函数定义 ---

    /** 获取过滤器值 */
    function getFilterValues() {
        const filters = {};
        if (!filterInputs) return filters;
        filterInputs.forEach(input => {
            const name = input.name;
            const value = input.value.trim();
            if (name && value !== '') {
                filters[name] = value;
            }
        });
        console.debug("[BusinessType] Current Filters:", filters);
        return filters;
    }

    /** 创建表格行 (修正布尔值显示) */
    function renderRow(item) {
        const row = document.createElement('tr');
        row.setAttribute('data-id', item.id); // 主键是 id
        row.classList.add('hover:bg-gray-50');

        row.innerHTML = `
            <td class="table-cell text-sm font-medium text-center">${item.id}</td>
            <td class="table-cell text-sm">${item.businessCategory || '-'}</td>
            <td class="table-cell text-sm">${item.businessName || '-'}</td>
            <td class="table-cell text-sm">${item.businessDescription || '-'}</td>
            <td class="table-cell text-sm text-center">${item.enabled ? '<span class="text-green-600 font-semibold">启用</span>' : '<span class="text-red-600 font-semibold">停用</span>'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2 text-center">
                <button class="btn btn-secondary btn-sm edit-btn">修改</button>
                <button class="btn btn-danger btn-sm delete-btn">删除</button>
            </td>
        `;
        return row;
    }

    /** 使行可编辑 */
    function makeRowEditable(row, initialData = {}, isNew = false) {
        console.debug("[BusinessType] Making row editable. isNew:", isNew, "Data:", initialData);
        row.innerHTML = '';
        row.classList.add('editing-row');

        // ID (只读)
        const cellId = row.insertCell(); cellId.classList.add('table-cell', 'font-medium');
        cellId.textContent = isNew ? '(自动生成)' : initialData.id;
        const hiddenId = document.createElement('input'); hiddenId.type = 'hidden'; hiddenId.name = 'id';
        hiddenId.value = initialData.id || ''; cellId.appendChild(hiddenId);

        // 业务类别
        const cellCategory = row.insertCell(); cellCategory.classList.add('table-cell');
        const inputCategory = document.createElement('input'); inputCategory.type = 'text'; inputCategory.name = 'businessCategory';
        inputCategory.value = initialData.businessCategory || ''; inputCategory.placeholder = '输入业务类别';
        inputCategory.classList.add('input-field', 'editing-cell'); inputCategory.required = true;
        cellCategory.appendChild(inputCategory);

        // 业务名称
        const cellName = row.insertCell(); cellName.classList.add('table-cell');
        const inputName = document.createElement('input'); inputName.type = 'text'; inputName.name = 'businessName';
        inputName.value = initialData.businessName || ''; inputName.placeholder = '输入业务名称';
        inputName.classList.add('input-field', 'editing-cell'); inputName.required = true;
        cellName.appendChild(inputName);

        // 业务描述
        const cellDesc = row.insertCell(); cellDesc.classList.add('table-cell');
        const inputDesc = document.createElement('textarea'); inputDesc.name = 'businessDescription';
        inputDesc.value = initialData.businessDescription || ''; inputDesc.placeholder = '输入详细描述';
        inputDesc.classList.add('input-field', 'editing-cell', 'h-10', 'text-sm'); inputDesc.rows = 1;
        cellDesc.appendChild(inputDesc);

        // 启用状态
        const cellEnabled = row.insertCell(); cellEnabled.classList.add('table-cell');
        const selectEnabled = document.createElement('select'); selectEnabled.name = 'enabled';
        selectEnabled.classList.add('select-field', 'editing-cell');
        selectEnabled.innerHTML = `<option value="true" ${initialData.enabled !== false ? 'selected' : ''}>启用</option><option value="false" ${initialData.enabled === false ? 'selected' : ''}>停用</option>`;
        cellEnabled.appendChild(selectEnabled);

        // 操作按钮
        const cellActions = row.insertCell(); cellActions.classList.add('px-6', 'py-4', 'whitespace-nowrap', 'text-sm', 'font-medium', 'space-x-2', 'text-center');
        const saveButton = document.createElement('button'); saveButton.textContent = '保存'; saveButton.classList.add('btn', 'btn-primary', 'btn-sm', 'save-btn'); saveButton.type = 'button';
        const cancelButton = document.createElement('button'); cancelButton.textContent = '取消'; cancelButton.classList.add('btn', 'btn-secondary', 'btn-sm', 'cancel-btn'); cancelButton.type = 'button';
        cellActions.appendChild(saveButton); cellActions.appendChild(cancelButton);

        // 绑定取消事件
        cancelButton.addEventListener('click', () => {
            console.debug("[BusinessType] Cancel button clicked. isNew:", isNew);
            if (isNew) { row.remove(); }
            else { const originalRow = renderRow(initialData); row.parentNode.replaceChild(originalRow, row); }
        });
    }

    /** 从编辑行收集数据 */
    function collectRowData(row) {
        const data = {};
        let isValid = true;
        const inputs = row.querySelectorAll('input[name], select[name], textarea[name]');
        console.debug("[BusinessType] Collecting data from row:", row);

        inputs.forEach(input => {
            const name = input.name;
            let value = input.value; // trim for text/textarea

            if (input.type === 'text' || input.tagName === 'TEXTAREA') {
                value = value.trim();
            }
            console.debug(`[BusinessType] Input name: ${name}, value: "${value}", required: ${input.required}`);

            if (input.required && value === '') {
                isValid = false;
                input.classList.add('border-red-500');
                console.warn(`[BusinessType] Validation failed: Field ${name} is required.`);
            } else {
                input.classList.remove('border-red-500');
            }

            // 类型转换
            if (input.tagName === 'SELECT' && name === 'enabled') {
                value = value === 'true';
            } else if (name === 'id' && value === '') {
                value = null; // ID 在新增时为空
            }

            if (name) {
                // 不收集空的 ID
                if(name === 'id' && value === null) { /* do nothing */ }
                else { data[name] = value; }
            }
        });

        console.debug("[BusinessType] Collected data:", data, "isValid:", isValid);
        if (!isValid) {
            AppUtils.showMessage('请填写所有必填项', 'warning');
            return null;
        }
        return data;
    }

    /** 渲染表格 */
    function renderTable(data) {
        if (!tableBody) return;
        tableBody.innerHTML = '';
        const loadingRow = tableBody.querySelector('.loading-row');
        if(loadingRow) loadingRow.remove();

        if (!data || data.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-gray-500">暂无数据</td></tr>`;
            return;
        }
        data.forEach(item => {
            const row = renderRow(item);
            tableBody.appendChild(row);
        });
    }

    /** 获取数据并更新UI */
    async function fetchData(page = 1, filters = {}) {
        console.debug(`[BusinessType] Fetching data. Page: ${page}, Filters:`, filters);
        currentPage = page;
        currentFilters = filters;
        const loadingRow = `<tr><td colspan="6" class="text-center py-4 loading-row"><div class="loader"></div></td></tr>`;
        if(tableBody) tableBody.innerHTML = loadingRow;
        if (window.AppUtils && typeof window.AppUtils.setupPagination === 'function') {
            window.AppUtils.setupPagination({ containerId: PAGINATION_CONTAINER_ID, currentPage: 1, totalPages: 0, totalRecords: 0, onPageChange: ()=>{} });
        }

        const params = new URLSearchParams({ page: page, size: DEFAULT_PAGE_SIZE, ...filters });
        const url = `${API_BASE_URL}?${params.toString()}`;

        try {
            const response = await AppUtils.get(url);
            console.debug("[BusinessType] API response received:", response);
            renderTable(response.data || []);

            // --- 修正：明确传递 containerId ---
            if (window.AppUtils && typeof window.AppUtils.setupPagination === 'function') {
                window.AppUtils.setupPagination({
                    containerId: PAGINATION_CONTAINER_ID, // 使用本模块定义的常量 'dept-pagination-container'
                    currentPage: response.currentPage || 1,
                    totalPages: response.totalPages || 1,
                    totalRecords: response.totalRecords || 0,
                    onPageChange: (newPage) => fetchData(newPage, currentFilters) // 回调函数不变
                });
                console.debug(`[Department] Pagination setup for container #${PAGINATION_CONTAINER_ID}`);
            } else {
                console.error("[Department] AppUtils.setupPagination function not found!");
            }
        } catch (error) {
            console.error('[BusinessType] 获取业务类型数据失败:', error);
            if(tableBody) tableBody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-red-500">加载数据失败: ${error.message || '未知错误'}</td></tr>`;
            AppUtils.showMessage(`加载业务类型数据失败: ${error.message || '请稍后重试'}`, 'error');
        }
    }

    /** 处理新增 */
    function handleAddNew() {
        console.debug("[BusinessType] Add New button clicked.");
        if (!tableBody) return;
        const existingEditRow = tableBody.querySelector('.editing-row');
        if (existingEditRow) {
            AppUtils.showMessage('请先完成当前编辑或取消', 'warning');
            existingEditRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
            existingEditRow.classList.add('animate-pulse');
            setTimeout(() => existingEditRow.classList.remove('animate-pulse'), 1500);
            return;
        }
        const newRow = tableBody.insertRow(0);
        newRow.classList.add('new-editing-row');
        makeRowEditable(newRow, {}, true);
        newRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    /** 处理保存 */
    async function handleSave(row) {
        const isNew = row.classList.contains('new-editing-row');
        console.debug(`[BusinessType] Save button clicked. isNew: ${isNew}, Row:`, row);
        const data = collectRowData(row);

        if (!data) {
            console.warn("[BusinessType] Save aborted due to invalid data.");
            return;
        }

        const saveButton = row.querySelector('.save-btn');
        const cancelButton = row.querySelector('.cancel-btn');
        if(saveButton) saveButton.disabled = true; saveButton.textContent = '保存中...';
        if(cancelButton) cancelButton.disabled = true;

        try {
            if (isNew) {
                // 新增时不需要发送 ID
                const postData = { ...data };
                delete postData.id;
                console.log("[BusinessType] Attempting to add business type:", postData);
                await AppUtils.post(API_BASE_URL, postData);
                AppUtils.showMessage('业务类型添加成功!', 'success');
            } else {
                const id = row.querySelector('input[name="id"]').value;
                if (!id) throw new Error("无法获取业务类型 ID 进行更新");
                console.log(`[BusinessType] Attempting to update business type ${id}:`, data);
                await AppUtils.put(`${API_BASE_URL}/${id}`, data);
                AppUtils.showMessage('业务类型更新成功!', 'success');
            }
            fetchData(currentPage, currentFilters); // 刷新当前页
        } catch (error) {
            console.error('[BusinessType] 保存业务类型失败:', error);
            AppUtils.showMessage(`保存失败: ${error.message || '请检查输入或联系管理员'}`, 'error');
            if(saveButton) saveButton.disabled = false; saveButton.textContent = '保存';
            if(cancelButton) cancelButton.disabled = false;
        }
    }

    /** 处理删除 */
    async function handleDelete(row) {
        const id = row.getAttribute('data-id');
        const btName = row.cells[2].textContent;
        console.debug(`[BusinessType] Delete button clicked for ID: ${id}, Name: ${btName}`);
        if (!id) return;
        if (!confirm(`确定要删除业务类型 "${btName}" (ID: ${id}) 吗？\n注意：如果该类型已被工时编码引用，将无法删除。`)) {
            console.debug("[BusinessType] Delete cancelled by user.");
            return;
        }

        const deleteButton = row.querySelector('.delete-btn');
        const editButton = row.querySelector('.edit-btn');
        if(deleteButton) deleteButton.disabled = true;
        if(editButton) editButton.disabled = true;

        try {
            console.log(`[BusinessType] Attempting to delete business type ${id}`);
            await AppUtils.delete(`${API_BASE_URL}/${id}`);
            AppUtils.showMessage('业务类型删除成功!', 'success');
            row.remove();
            // 可选：检查是否需要加载前一页
            const currentRows = tableBody.querySelectorAll('tr:not(.editing-row)');
            if (currentRows.length === 0 && currentPage > 1) {
                console.log("[BusinessType] Current page empty after delete, fetching previous page.");
                fetchData(currentPage - 1, currentFilters);
            }
        } catch (error) {
            console.error('[BusinessType] 删除业务类型失败:', error);
            AppUtils.showMessage(`删除失败: ${error.message || '请检查该类型是否仍被引用'}`, 'error');
            if(deleteButton) deleteButton.disabled = false;
            if(editButton) editButton.disabled = false;
        }
    }

    /** 设置事件监听器 */
    function setupEventListeners() {
        console.debug("[BusinessType] Setting up event listeners.");
        if (!tableBody || !addButton || !filterInputs || !clearButton) {
            console.error("[BusinessType] Cannot setup listeners, required elements missing.");
            return;
        }

        // 过滤事件
        const debouncedFetch = AppUtils.debounce(() => {
            const filters = getFilterValues();
            fetchData(1, filters);
        }, DEBOUNCE_DELAY);

        filterInputs.forEach(input => {
            input.addEventListener('input', debouncedFetch);
            if (input.tagName === 'SELECT') {
                input.addEventListener('change', debouncedFetch);
            }
        });

        // 新增按钮
        addButton.addEventListener('click', handleAddNew);

        // 清空筛选按钮
        clearButton.addEventListener('click', () => {
            console.debug("[BusinessType] Clear filters button clicked.");
            filterInputs.forEach(input => { input.value = ''; });
            fetchData(1, {});
            AppUtils.showMessage('筛选条件已清空', 'info');
        });

        // 表格事件委托
        tableBody.addEventListener('click', function(event) {
            const target = event.target;
            const row = target.closest('tr');
            if (!row) return;
            console.debug("[BusinessType] Click detected inside table body. Target:", target);

            if (target.classList.contains('edit-btn')) {
                console.debug("[BusinessType] Edit button clicked.");
                const existingEditRow = tableBody.querySelector('.editing-row');
                if (existingEditRow && existingEditRow !== row) {
                    AppUtils.showMessage('请先完成当前编辑或取消', 'warning');
                    existingEditRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    existingEditRow.classList.add('animate-pulse');
                    setTimeout(() => existingEditRow.classList.remove('animate-pulse'), 1500);
                    return;
                }
                if (row.classList.contains('editing-row')) return;

                const id = row.getAttribute('data-id');
                console.log(`[BusinessType] Fetching data for edit, ID: ${id}`);
                AppUtils.get(`${API_BASE_URL}/${id}`).then(data => {
                    if(data) makeRowEditable(row, data, false);
                    else AppUtils.showMessage('无法获取业务类型数据进行编辑', 'error');
                }).catch(err => {
                    console.error("[BusinessType] 获取编辑数据失败:", err);
                    AppUtils.showMessage(`获取编辑数据失败: ${err.message}`, 'error');
                });
            } else if (target.classList.contains('delete-btn')) {
                console.debug("[BusinessType] Delete button clicked.");
                handleDelete(row);
            } else if (target.classList.contains('save-btn')) {
                console.debug("[BusinessType] Save button clicked.");
                if(row.classList.contains('editing-row') || row.classList.contains('new-editing-row')){
                    handleSave(row);
                } else {
                    console.warn("[BusinessType] Save button clicked on non-editing row.");
                }
            }
        });
    }

    /** 初始化模块 */
    BusinessTypeModule.init = function() {
        console.log("Initializing Business Type Module...");
        tableBody = document.getElementById(TABLE_BODY_ID);
        filterInputs = document.querySelectorAll(`#${FILTER_FORM_ID} .filter-input`);
        addButton = document.getElementById(ADD_BUTTON_ID);
        clearButton = document.getElementById(CLEAR_BUTTON_ID);

        if (!tableBody || !filterInputs || !addButton || !clearButton) {
            console.error("[BusinessType] Module initialization failed: Could not find required elements.",
                {tableBody, filterInputs, addButton, clearButton});
            const section = document.getElementById('business-types-section');
            if(section) section.innerHTML = '<p class="text-red-500 p-4">业务管理模块加载失败，请联系管理员。</p>';
            return;
        }

        setupEventListeners();
        fetchData(1, getFilterValues()); // 加载初始数据
    };

    window.BusinessTypeModule = BusinessTypeModule;

})(window);

// * **说明:**
//     * 结构与前几个模块类似。
//     * `makeRowEditable`: ID 字段在编辑时只读，新增时不显示输入框（显示“自动生成”）。其他字段如类别、名称、描述、状态均可编辑。
//     * `collectRowData`: 收集数据，进行非空验证。
//     * `handleSave`: 新增(POST)时不发送 ID，编辑(PUT)时从隐藏字段获取 ID 并包含在 URL 中。
//     * `handleDelete`: 删除前检查关联（Service 层实