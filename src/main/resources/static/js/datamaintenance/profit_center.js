/**
 * 利润中心管理模块脚本
 * 文件路径: src/main/resources/static/js/datamaintenance/profit_center.js
 * 职责：处理利润中心数据的获取、显示、过滤、分页、新增、编辑、删除操作。
 * 修正：布尔值显示、允许更新同步字段、清空筛选按钮、保存日志和调试。
 * 依赖: common.js, pagination.js
 */

(function(window) {
    'use strict';

    // 创建或获取模块命名空间
    const ProfitCenterModule = window.ProfitCenterModule || {};

    // --- 配置和常量 ---
    const API_BASE_URL = '/api/profit-centers';
    const TABLE_BODY_ID = 'profit-centers-table-body';
    const PAGINATION_CONTAINER_ID = 'pc-pagination-container';
    const FILTER_FORM_ID = 'pc-filters';
    const ADD_BUTTON_ID = 'add-pc-button';
    const CLEAR_BUTTON_ID = 'pc-clear-filters'; // 清空按钮 ID
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
        console.debug("[ProfitCenter] Current Filters:", filters);
        return filters;
    }

    /** 创建表格行 (修正布尔值显示) */
    function renderRow(item) {
        const row = document.createElement('tr');
        row.setAttribute('data-id', item.zone); // 主键是 zone
        row.classList.add('hover:bg-gray-50');

        // 截断过长的 zone 显示 (可选)
        const displayZone = (item.zone && item.zone.length > 30) ? item.zone.substring(0, 27) + '...' : (item.zone || '-');

        row.innerHTML = `
            <td class="table-cell text-sm font-medium" title="${item.zone || ''}">${displayZone}</td>
            <td class="table-cell text-sm">${item.businessType || '-'}</td>
            <td class="table-cell text-sm">${item.regionCategory || '-'}</td>
            <td class="table-cell text-sm">${item.responsiblePerson || '-'}</td>
            <td class="table-cell text-sm">${item.workLocation || '-'}</td>
            <td class="table-cell text-sm">${item.customZoneRemark || '-'}</td>
            <td class="table-cell text-sm text-center">${item.enabled ? '<span class="text-green-600 font-semibold">启用</span>' : '<span class="text-red-600 font-semibold">停用</span>'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2 text-center">
                <button class="btn btn-secondary btn-sm edit-btn">修改</button>
                <button class="btn btn-danger btn-sm delete-btn">删除</button>
            </td>
        `;
        return row;
    }

    /** 使行可编辑 (修正同步字段可编辑) */
    function makeRowEditable(row, initialData = {}, isNew = false) {
        console.debug("[ProfitCenter] Making row editable. isNew:", isNew, "Data:", initialData);
        row.innerHTML = '';
        row.classList.add('editing-row');

        // 利润中心全名 (主键)
        const cellZone = row.insertCell(); cellZone.classList.add('table-cell text-sm');
        const inputZone = document.createElement('input'); inputZone.type = 'text'; inputZone.name = 'zone';
        inputZone.value = initialData.zone || ''; inputZone.placeholder = '输入利润中心全名 (主键)';
        inputZone.classList.add('input-field', 'editing-cell'); inputZone.required = true;
        if (!isNew) { inputZone.readOnly = true; inputZone.classList.add('bg-gray-100', 'cursor-not-allowed'); }
        cellZone.appendChild(inputZone);

        // 业务类型 (改为可编辑)
        const cellBusinessType = row.insertCell(); cellBusinessType.classList.add('table-cell text-sm');
        const inputBusinessType = document.createElement('input'); inputBusinessType.type = 'text'; inputBusinessType.name = 'businessType';
        inputBusinessType.value = initialData.businessType || ''; inputBusinessType.placeholder = '业务类型';
        inputBusinessType.classList.add('input-field', 'editing-cell', 'w-24');
        cellBusinessType.appendChild(inputBusinessType);

        // 区域分类 (改为可编辑)
        const cellRegionCat = row.insertCell(); cellRegionCat.classList.add('table-cell text-sm');
        const inputRegionCat = document.createElement('input'); inputRegionCat.type = 'text'; inputRegionCat.name = 'regionCategory';
        inputRegionCat.value = initialData.regionCategory || ''; inputRegionCat.placeholder = '区域分类';
        inputRegionCat.classList.add('input-field', 'editing-cell', 'w-24');
        cellRegionCat.appendChild(inputRegionCat);

        // --- 其他同步字段 (根据需要也改为可编辑输入框) ---
        // 例如: regionName, centerName, businessSubcategory, departmentName
        // 这里暂时省略，如果需要编辑，按照 businessType 的方式添加 input

        // 区域负责人
        const cellRespPerson = row.insertCell(); cellRespPerson.classList.add('table-cell text-sm');
        const inputRespPerson = document.createElement('input'); inputRespPerson.type = 'text'; inputRespPerson.name = 'responsiblePerson';
        inputRespPerson.value = initialData.responsiblePerson || ''; inputRespPerson.placeholder = '输入负责人姓名';
        inputRespPerson.classList.add('input-field', 'editing-cell');
        cellRespPerson.appendChild(inputRespPerson);

        // 工作地点
        const cellWorkLoc = row.insertCell(); cellWorkLoc.classList.add('table-cell text-sm');
        const inputWorkLoc = document.createElement('input'); inputWorkLoc.type = 'text'; inputWorkLoc.name = 'workLocation';
        inputWorkLoc.value = initialData.workLocation || ''; inputWorkLoc.placeholder = '输入工作地点';
        inputWorkLoc.classList.add('input-field', 'editing-cell');
        cellWorkLoc.appendChild(inputWorkLoc);

        // 备注 (自定义)
        const cellRemark = row.insertCell(); cellRemark.classList.add('table-cell text-sm');
        const inputRemark = document.createElement('textarea'); inputRemark.name = 'customZoneRemark';
        inputRemark.value = initialData.customZoneRemark || ''; inputRemark.placeholder = '输入自定义备注';
        inputRemark.classList.add('input-field', 'editing-cell', 'h-10', 'text-sm'); inputRemark.rows = 1;
        cellRemark.appendChild(inputRemark);

        // 启用状态
        const cellEnabled = row.insertCell(); cellEnabled.classList.add('table-cell text-sm');
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
            console.debug("[ProfitCenter] Cancel button clicked. isNew:", isNew);
            if (isNew) { row.remove(); }
            else { const originalRow = renderRow(initialData); row.parentNode.replaceChild(originalRow, row); }
        });
    }

    /** 从编辑行收集数据 */
    function collectRowData(row) {
        const data = {};
        let isValid = true;
        const inputs = row.querySelectorAll('input[name], select[name], textarea[name]');
        console.debug("[ProfitCenter] Collecting data from row:", row);

        inputs.forEach(input => {
            const name = input.name;
            let value = input.value; // trim for text/textarea

            if (input.type === 'text' || input.tagName === 'TEXTAREA') {
                value = value.trim();
            }
            console.debug(`[ProfitCenter] Input name: ${name}, value: "${value}", required: ${input.required}`);

            if (input.required && value === '') {
                isValid = false;
                input.classList.add('border-red-500');
                console.warn(`[ProfitCenter] Validation failed: Field ${name} is required.`);
            } else {
                input.classList.remove('border-red-500');
            }

            // 类型转换
            if (input.tagName === 'SELECT' && name === 'enabled') {
                value = value === 'true';
            }

            if (name) {
                // 对于可选字段，空字符串转为 null
                if (!input.required && value === '' && input.type !== 'hidden') {
                    data[name] = null;
                } else {
                    data[name] = value;
                }
            }
        });

        console.debug("[ProfitCenter] Collected data:", data, "isValid:", isValid);
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
            tableBody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-gray-500">暂无数据</td></tr>`;
            return;
        }
        data.forEach(item => {
            const row = renderRow(item);
            tableBody.appendChild(row);
        });
    }

    /** 获取数据并更新UI */
    async function fetchData(page = 1, filters = {}) {
        console.debug(`[ProfitCenter] Fetching data. Page: ${page}, Filters:`, filters);
        currentPage = page;
        currentFilters = filters;
        const loadingRow = `<tr><td colspan="8" class="text-center py-4 loading-row"><div class="loader"></div></td></tr>`;
        if(tableBody) tableBody.innerHTML = loadingRow;
        if (window.AppUtils && typeof window.AppUtils.setupPagination === 'function') {
            window.AppUtils.setupPagination({ containerId: PAGINATION_CONTAINER_ID, currentPage: 1, totalPages: 0, totalRecords: 0, onPageChange: ()=>{} });
        }

        const params = new URLSearchParams({ page: page, size: DEFAULT_PAGE_SIZE, ...filters });
        const url = `${API_BASE_URL}?${params.toString()}`;

        try {
            const response = await AppUtils.get(url);
            console.debug("[ProfitCenter] API response received:", response);
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
            console.error('[ProfitCenter] 获取利润中心数据失败:', error);
            if(tableBody) tableBody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-red-500">加载数据失败: ${error.message || '未知错误'}</td></tr>`;
            AppUtils.showMessage(`加载利润中心数据失败: ${error.message || '请稍后重试'}`, 'error');
        }
    }

    /** 处理新增 */
    function handleAddNew() {
        console.debug("[ProfitCenter] Add New button clicked.");
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
        makeRowEditable(newRow, {}, true); // 新增时 zone 需要用户输入
        newRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    /** 处理保存 */
    async function handleSave(row) {
        const isNew = row.classList.contains('new-editing-row');
        console.debug(`[ProfitCenter] Save button clicked. isNew: ${isNew}, Row:`, row);
        const data = collectRowData(row);

        if (!data) {
            console.warn("[ProfitCenter] Save aborted due to invalid data.");
            return;
        }

        const saveButton = row.querySelector('.save-btn');
        const cancelButton = row.querySelector('.cancel-btn');
        if(saveButton) saveButton.disabled = true; saveButton.textContent = '保存中...';
        if(cancelButton) cancelButton.disabled = true;

        try {
            if (isNew) {
                console.log("[ProfitCenter] Attempting to add profit center:", data);
                // 新增时发送所有收集到的数据，包括 zone 和其他可编辑字段
                await AppUtils.post(API_BASE_URL, data);
                AppUtils.showMessage('利润中心添加成功!', 'success');
            } else {
                const zone = row.getAttribute('data-id'); // 主键
                // 编辑时只发送允许修改的字段
                const updateData = {
                    businessType: data.businessType, // 允许更新
                    regionCategory: data.regionCategory, // 允许更新
                    // regionName: data.regionName, // 如果也设为可编辑
                    // centerName: data.centerName, // 如果也设为可编辑
                    // businessSubcategory: data.businessSubcategory, // 如果也设为可编辑
                    // departmentName: data.departmentName, // 如果也设为可编辑
                    responsiblePerson: data.responsiblePerson,
                    workLocation: data.workLocation,
                    customZoneRemark: data.customZoneRemark,
                    enabled: data.enabled
                };
                console.log(`[ProfitCenter] Attempting to update profit center ${zone}:`, updateData);
                await AppUtils.put(`${API_BASE_URL}/${encodeURIComponent(zone)}`, updateData);
                AppUtils.showMessage('利润中心更新成功!', 'success');
            }
            fetchData(currentPage, currentFilters); // 刷新当前页
        } catch (error) {
            console.error('[ProfitCenter] 保存利润中心失败:', error);
            AppUtils.showMessage(`保存失败: ${error.message || '请检查输入或联系管理员'}`, 'error');
            if(saveButton) saveButton.disabled = false; saveButton.textContent = '保存';
            if(cancelButton) cancelButton.disabled = false;
        }
    }

    /** 处理删除 */
    async function handleDelete(row) {
        const zone = row.getAttribute('data-id'); // 主键
        console.debug(`[ProfitCenter] Delete button clicked for ID: ${zone}`);
        if (!zone) return;
        if (!confirm(`确定要删除利润中心 "${zone}" 吗？`)) {
            console.debug("[ProfitCenter] Delete cancelled by user.");
            return;
        }

        const deleteButton = row.querySelector('.delete-btn');
        const editButton = row.querySelector('.edit-btn');
        if(deleteButton) deleteButton.disabled = true;
        if(editButton) editButton.disabled = true;

        try {
            console.log(`[ProfitCenter] Attempting to delete profit center ${zone}`);
            await AppUtils.delete(`${API_BASE_URL}/${encodeURIComponent(zone)}`);
            AppUtils.showMessage('利润中心删除成功!', 'success');
            row.remove();
            // 可选：检查是否需要加载前一页
            const currentRows = tableBody.querySelectorAll('tr:not(.editing-row)');
            if (currentRows.length === 0 && currentPage > 1) {
                console.log("[ProfitCenter] Current page empty after delete, fetching previous page.");
                fetchData(currentPage - 1, currentFilters);
            }
        } catch (error) {
            console.error('[ProfitCenter] 删除利润中心失败:', error);
            AppUtils.showMessage(`删除失败: ${error.message || '未知错误'}`, 'error');
            if(deleteButton) deleteButton.disabled = false;
            if(editButton) editButton.disabled = false;
        }
    }

    /** 设置事件监听器 */
    function setupEventListeners() {
        console.debug("[ProfitCenter] Setting up event listeners.");
        if (!tableBody || !addButton || !filterInputs || !clearButton) {
            console.error("[ProfitCenter] Cannot setup listeners, required elements missing.");
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
            console.debug("[ProfitCenter] Clear filters button clicked.");
            filterInputs.forEach(input => { input.value = ''; });
            fetchData(1, {});
            AppUtils.showMessage('筛选条件已清空', 'info');
        });

        // 表格事件委托
        tableBody.addEventListener('click', function(event) {
            const target = event.target;
            const row = target.closest('tr');
            if (!row) return;
            console.debug("[ProfitCenter] Click detected inside table body. Target:", target);

            if (target.classList.contains('edit-btn')) {
                console.debug("[ProfitCenter] Edit button clicked.");
                const existingEditRow = tableBody.querySelector('.editing-row');
                if (existingEditRow && existingEditRow !== row) {
                    AppUtils.showMessage('请先完成当前编辑或取消', 'warning');
                    existingEditRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    existingEditRow.classList.add('animate-pulse');
                    setTimeout(() => existingEditRow.classList.remove('animate-pulse'), 1500);
                    return;
                }
                if (row.classList.contains('editing-row')) return;

                const zone = row.getAttribute('data-id');
                console.log(`[ProfitCenter] Fetching data for edit, ID: ${zone}`);
                AppUtils.get(`${API_BASE_URL}/${encodeURIComponent(zone)}`).then(data => {
                    if(data) makeRowEditable(row, data, false);
                    else AppUtils.showMessage('无法获取利润中心数据进行编辑', 'error');
                }).catch(err => {
                    console.error("[ProfitCenter] 获取编辑数据失败:", err);
                    AppUtils.showMessage(`获取编辑数据失败: ${err.message}`, 'error');
                });
            } else if (target.classList.contains('delete-btn')) {
                console.debug("[ProfitCenter] Delete button clicked.");
                handleDelete(row);
            } else if (target.classList.contains('save-btn')) {
                console.debug("[ProfitCenter] Save button clicked.");
                if(row.classList.contains('editing-row') || row.classList.contains('new-editing-row')){
                    handleSave(row);
                } else {
                    console.warn("[ProfitCenter] Save button clicked on non-editing row.");
                }
            }
        });
    }

    /** 初始化模块 */
    ProfitCenterModule.init = function() {
        console.log("Initializing Profit Center Module...");
        tableBody = document.getElementById(TABLE_BODY_ID);
        filterInputs = document.querySelectorAll(`#${FILTER_FORM_ID} .filter-input`);
        addButton = document.getElementById(ADD_BUTTON_ID);
        clearButton = document.getElementById(CLEAR_BUTTON_ID);

        if (!tableBody || !filterInputs || !addButton || !clearButton) {
            console.error("[ProfitCenter] Module initialization failed: Could not find required elements.",
                {tableBody, filterInputs, addButton, clearButton});
            const section = document.getElementById('profit-centers-section');
            if(section) section.innerHTML = '<p class="text-red-500 p-4">利润中心管理模块加载失败，请联系管理员。</p>';
            return;
        }

        setupEventListeners();
        fetchData(1, getFilterValues()); // 加载初始数据
    };

    window.ProfitCenterModule = ProfitCenterModule;

})(window);

//
// * **说明:**
//     * 结构与前几个模块类似。
//     * `renderRow`: 显示利润中心数据，对过长的 `zone` 进行了截断显示（鼠标悬停显示完整）。
//     * `makeRowEditable`:
//         * 将 `zone` (主键) 在编辑时设为只读，在新增时设为可输入（因为它是用户定义的）。
//         * 将同步字段（`businessType`, `regionCategory` 等）设为只读显示。
//         * 将 `responsiblePerson`, `workLocation`, `customZoneRemark`, `enabled` 设为可编辑。
//     * `collectRowData`: 收集数据，进行非空验证。
//     * `handleSave`: 新增(POST)时发送用户输入的所有字段（包括 `zone`），编辑(PUT)时只发送可修改的字段。
//     * `handleDelete`: 按 `zone`