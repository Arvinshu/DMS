/**
 * 工时编码管理模块脚本
 * 文件路径: src/main/resources/static/js/datamaintenance/timesheet_code.js
 * 职责：处理工时编码数据的获取、显示、过滤、分页、新增、编辑、删除操作。
 * 修正：布尔值显示、业务类型下拉框、tsName/sTsBm 可编辑、清空筛选按钮、保存日志和调试。
 * 依赖: common.js, pagination.js
 */

(function(window) {
    'use strict';

    // 创建或获取模块命名空间
    const TimesheetCodeModule = window.TimesheetCodeModule || {};

    // --- 配置和常量 ---
    const API_BASE_URL = '/api/timesheet-codes';
    const BUSINESS_TYPES_API_URL = '/api/business-types'; // 获取业务类型的 API
    const TABLE_BODY_ID = 'timesheet-codes-table-body';
    const PAGINATION_CONTAINER_ID = 'tsc-pagination-container';
    const FILTER_FORM_ID = 'tsc-filters';
    const ADD_BUTTON_ID = 'add-tsc-button';
    const CLEAR_BUTTON_ID = 'tsc-clear-filters'; // 清空按钮 ID
    const DEFAULT_PAGE_SIZE = 50;
    const DEBOUNCE_DELAY = 300;

    // --- DOM 元素引用 ---
    let tableBody = null;
    let filterInputs = null;
    let addButton = null;
    let clearButton = null; // 清空按钮
    let businessTypeFilterSelect = null; // 业务类型过滤下拉框

    // --- 状态变量 ---
    let currentPage = 1;
    let currentFilters = {};
    let businessTypeOptionsCache = null; // 缓存业务类型选项

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
        console.debug("[TimesheetCode] Current Filters:", filters);
        return filters;
    }

    /**
     * 加载业务类型选项到指定的 select 元素
     * @param {HTMLSelectElement} selectElement
     * @param {string} [selectedValue] - 业务类型名称
     * @param {boolean} [addEmptyOption=true]
     * @param {string} [emptyOptionText='所有类型']
     */
    async function loadBusinessTypeOptions(selectElement, selectedValue, addEmptyOption = true, emptyOptionText = '所有类型') {
        console.debug(`[TimesheetCode] Loading business type options, selected: ${selectedValue}`);
        if (!selectElement) return;

        if (businessTypeOptionsCache) {
            populateBusinessTypeSelect(selectElement, businessTypeOptionsCache, selectedValue, addEmptyOption, emptyOptionText);
            return;
        }

        selectElement.innerHTML = `<option value="">加载中...</option>`;
        selectElement.disabled = true;

        try {
            const response = await AppUtils.get(`${BUSINESS_TYPES_API_URL}?size=1000&enabled=true`); // 只加载启用的业务类型
            businessTypeOptionsCache = response.data || [];
            populateBusinessTypeSelect(selectElement, businessTypeOptionsCache, selectedValue, addEmptyOption, emptyOptionText);
            console.debug(`[TimesheetCode] Loaded ${businessTypeOptionsCache.length} enabled business types.`);
        } catch (error) {
            console.error("[TimesheetCode] 加载业务类型选项失败:", error);
            selectElement.innerHTML = `<option value="">加载失败</option>`;
            AppUtils.showMessage(`加载业务类型选项失败: ${error.message || '请稍后重试'}`, 'error');
        } finally {
            selectElement.disabled = false;
        }
    }

    /** 填充业务类型 Select */
    function populateBusinessTypeSelect(selectElement, options, selectedValue, addEmptyOption, emptyOptionText) {
        selectElement.innerHTML = '';
        if (addEmptyOption) {
            const emptyOpt = document.createElement('option');
            emptyOpt.value = '';
            emptyOpt.textContent = emptyOptionText;
            selectElement.appendChild(emptyOpt);
        }
        options.forEach(bt => {
            // 确保只添加启用的类型（如果缓存包含未启用的）
            if (bt.enabled) {
                const option = document.createElement('option');
                option.value = bt.businessName; // 值是业务名称
                option.textContent = `${bt.businessName} (${bt.businessCategory})`; // 显示名称和类别
                if (selectedValue != null && bt.businessName == selectedValue) {
                    option.selected = true;
                }
                selectElement.appendChild(option);
            }
        });
    }


    /** 创建表格行 (修正布尔值显示) */
    function renderRow(item) {
        const row = document.createElement('tr');
        row.setAttribute('data-id', item.tsBm); // 主键是 tsBm
        row.classList.add('hover:bg-gray-50');

        row.innerHTML = `
            <td class="table-cell text-sm font-medium">${item.tsBm || '-'}</td>
            <td class="table-cell text-sm">${item.tsName || '-'}</td>
            <td class="table-cell text-sm">${item.sTsBm || '-'}</td>
            <td class="table-cell text-sm">${item.customProjectName || '-'}</td>
            <td class="table-cell text-sm text-center">${item.projectTimesheet ? '<span class="text-green-600 font-semibold">是</span>' : '<span class="text-red-600 font-semibold">否</span>'}</td>
            <td class="table-cell text-sm">${item.projectBusinessType || '-'}</td>
            <td class="table-cell text-sm text-center">${item.enabled ? '<span class="text-green-600 font-semibold">启用</span>' : '<span class="text-red-600 font-semibold">停用</span>'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2 text-center">
                <button class="btn btn-secondary btn-sm edit-btn">修改</button>
                <button class="btn btn-danger btn-sm delete-btn">删除</button>
            </td>
        `;
        return row;
    }

    /** 使行可编辑 (修正下拉框和可编辑字段) */
    async function makeRowEditable(row, initialData = {}, isNew = false) {
        console.debug("[TimesheetCode] Making row editable. isNew:", isNew, "Data:", initialData);
        row.innerHTML = '';
        row.classList.add('editing-row');

        // 工时编码 (主键)
        const cellTsBm = row.insertCell(); cellTsBm.classList.add('table-cell');
        const inputTsBm = document.createElement('input'); inputTsBm.type = 'text'; inputTsBm.name = 'tsBm';
        inputTsBm.value = initialData.tsBm || ''; inputTsBm.placeholder = '输入工时编码 (主键)';
        inputTsBm.classList.add('input-field', 'editing-cell'); inputTsBm.required = true;
        if (!isNew) { inputTsBm.readOnly = true; inputTsBm.classList.add('bg-gray-100', 'cursor-not-allowed'); }
        cellTsBm.appendChild(inputTsBm);

        // 工时名称 (改为可编辑)
        const cellTsName = row.insertCell(); cellTsName.classList.add('table-cell');
        const inputTsName = document.createElement('input'); inputTsName.type = 'text'; inputTsName.name = 'tsName';
        inputTsName.value = initialData.tsName || ''; inputTsName.placeholder = '输入工时名称';
        inputTsName.classList.add('input-field', 'editing-cell');
        // inputTsName.required = true; // 工时名称可能允许为空? 根据实际需求定
        cellTsName.appendChild(inputTsName);

        // 子工时编码 (改为可编辑)
        const cellSTsBm = row.insertCell(); cellSTsBm.classList.add('table-cell');
        const inputSTsBm = document.createElement('input'); inputSTsBm.type = 'text'; inputSTsBm.name = 'sTsBm';
        inputSTsBm.value = initialData.sTsBm || ''; inputSTsBm.placeholder = '输入子编码(可选)';
        inputSTsBm.classList.add('input-field', 'editing-cell');
        cellSTsBm.appendChild(inputSTsBm);

        // 工时信息 (自定义)
        const cellCustomName = row.insertCell(); cellCustomName.classList.add('table-cell');
        const inputCustomName = document.createElement('input'); inputCustomName.type = 'text'; inputCustomName.name = 'customProjectName';
        inputCustomName.value = initialData.customProjectName || ''; inputCustomName.placeholder = '部门维护的项目名称';
        inputCustomName.classList.add('input-field', 'editing-cell');
        cellCustomName.appendChild(inputCustomName);

        // 是否项目工时
        const cellIsProject = row.insertCell(); cellIsProject.classList.add('table-cell');
        const selectIsProject = document.createElement('select'); selectIsProject.name = 'projectTimesheet';
        selectIsProject.classList.add('select-field', 'editing-cell');
        selectIsProject.innerHTML = `<option value="true" ${initialData.projectTimesheet !== false ? 'selected' : ''}>是</option><option value="false" ${initialData.projectTimesheet === false ? 'selected' : ''}>否</option>`;
        cellIsProject.appendChild(selectIsProject);

        // 项目业务类型 (下拉)
        const cellBusinessType = row.insertCell(); cellBusinessType.classList.add('table-cell');
        const selectBusinessType = document.createElement('select'); selectBusinessType.name = 'projectBusinessType';
        selectBusinessType.classList.add('select-field', 'editing-cell');
        cellBusinessType.appendChild(selectBusinessType);
        // 异步加载选项，允许为空
        await loadBusinessTypeOptions(selectBusinessType, initialData.projectBusinessType, true, '-- 选择业务类型 (可选) --');

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
            console.debug("[TimesheetCode] Cancel button clicked. isNew:", isNew);
            if (isNew) { row.remove(); }
            else { const originalRow = renderRow(initialData); row.parentNode.replaceChild(originalRow, row); }
        });
    }

    /** 从编辑行收集数据 */
    function collectRowData(row) {
        const data = {};
        let isValid = true;
        const inputs = row.querySelectorAll('input[name], select[name]');
        console.debug("[TimesheetCode] Collecting data from row:", row);

        inputs.forEach(input => {
            const name = input.name;
            let value = input.value; // trim for text inputs only

            if (input.type === 'text') {
                value = value.trim();
            }

            console.debug(`[TimesheetCode] Input name: ${name}, value: "${value}", required: ${input.required}`);

            if (input.required && value === '') {
                isValid = false;
                input.classList.add('border-red-500');
                console.warn(`[TimesheetCode] Validation failed: Field ${name} is required.`);
            } else {
                input.classList.remove('border-red-500');
            }

            // 类型转换
            if (input.tagName === 'SELECT' && (name === 'projectTimesheet' || name === 'enabled')) {
                value = value === 'true';
            } else if (input.tagName === 'SELECT' && name === 'projectBusinessType') {
                value = value === '' ? null : value; // 允许为空
            }

            if (name) {
                // 对于可选字段，空字符串转为 null
                if (!input.required && value === '' && (name === 'sTsBm' || name === 'customProjectName' || name === 'tsName')) {
                    data[name] = null;
                } else {
                    data[name] = value;
                }
            }
        });

        console.debug("[TimesheetCode] Collected data:", data, "isValid:", isValid);
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
        console.debug(`[TimesheetCode] Fetching data. Page: ${page}, Filters:`, filters);
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
            console.debug("[TimesheetCode] API response received:", response);
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
            console.error('[TimesheetCode] 获取工时编码数据失败:', error);
            if(tableBody) tableBody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-red-500">加载数据失败: ${error.message || '未知错误'}</td></tr>`;
            AppUtils.showMessage(`加载工时编码数据失败: ${error.message || '请稍后重试'}`, 'error');
        }
    }

    /** 处理新增 */
    function handleAddNew() {
        console.debug("[TimesheetCode] Add New button clicked.");
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
        makeRowEditable(newRow, {}, true); // 异步加载业务类型选项
        newRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    /** 处理保存 */
    async function handleSave(row) {
        const isNew = row.classList.contains('new-editing-row');
        console.debug(`[TimesheetCode] Save button clicked. isNew: ${isNew}, Row:`, row);
        const data = collectRowData(row);

        if (!data) {
            console.warn("[TimesheetCode] Save aborted due to invalid data.");
            return;
        }

        const saveButton = row.querySelector('.save-btn');
        const cancelButton = row.querySelector('.cancel-btn');
        if(saveButton) saveButton.disabled = true; saveButton.textContent = '保存中...';
        if(cancelButton) cancelButton.disabled = true;

        try {
            if (isNew) {
                console.log("[TimesheetCode] Attempting to add timesheet code:", data);
                await AppUtils.post(API_BASE_URL, data);
                AppUtils.showMessage('工时编码添加成功!', 'success');
            } else {
                const tsBm = row.getAttribute('data-id'); // 主键
                // 编辑时发送所有可能修改的字段
                const updateData = {
                    tsName: data.tsName,
                    sTsBm: data.sTsBm,
                    customProjectName: data.customProjectName,
                    projectTimesheet: data.projectTimesheet,
                    projectBusinessType: data.projectBusinessType,
                    enabled: data.enabled
                };
                console.log(`[TimesheetCode] Attempting to update timesheet code ${tsBm}:`, updateData);
                await AppUtils.put(`${API_BASE_URL}/${encodeURIComponent(tsBm)}`, updateData);
                AppUtils.showMessage('工时编码更新成功!', 'success');
            }
            fetchData(currentPage, currentFilters); // 刷新当前页
        } catch (error) {
            console.error('[TimesheetCode] 保存工时编码失败:', error);
            AppUtils.showMessage(`保存失败: ${error.message || '请检查输入或联系管理员'}`, 'error');
            if(saveButton) saveButton.disabled = false; saveButton.textContent = '保存';
            if(cancelButton) cancelButton.disabled = false;
        }
    }

    /** 处理删除 */
    async function handleDelete(row) {
        const tsBm = row.getAttribute('data-id'); // 主键
        console.debug(`[TimesheetCode] Delete button clicked for ID: ${tsBm}`);
        if (!tsBm) return;
        if (!confirm(`确定要删除工时编码 "${tsBm}" 吗？`)) {
            console.debug("[TimesheetCode] Delete cancelled by user.");
            return;
        }

        const deleteButton = row.querySelector('.delete-btn');
        const editButton = row.querySelector('.edit-btn');
        if(deleteButton) deleteButton.disabled = true;
        if(editButton) editButton.disabled = true;

        try {
            console.log(`[TimesheetCode] Attempting to delete timesheet code ${tsBm}`);
            await AppUtils.delete(`${API_BASE_URL}/${encodeURIComponent(tsBm)}`);
            AppUtils.showMessage('工时编码删除成功!', 'success');
            row.remove();
            // 可选：检查是否需要加载前一页
            const currentRows = tableBody.querySelectorAll('tr:not(.editing-row)');
            if (currentRows.length === 0 && currentPage > 1) {
                console.log("[TimesheetCode] Current page empty after delete, fetching previous page.");
                fetchData(currentPage - 1, currentFilters);
            }
        } catch (error) {
            console.error('[TimesheetCode] 删除工时编码失败:', error);
            AppUtils.showMessage(`删除失败: ${error.message || '未知错误'}`, 'error');
            if(deleteButton) deleteButton.disabled = false;
            if(editButton) editButton.disabled = false;
        }
    }

    /** 设置事件监听器 */
    function setupEventListeners() {
        console.debug("[TimesheetCode] Setting up event listeners.");
        if (!tableBody || !addButton || !filterInputs || !clearButton || !businessTypeFilterSelect) {
            console.error("[TimesheetCode] Cannot setup listeners, required elements missing.");
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
            console.debug("[TimesheetCode] Clear filters button clicked.");
            filterInputs.forEach(input => { input.value = ''; });
            fetchData(1, {});
            AppUtils.showMessage('筛选条件已清空', 'info');
        });

        // 表格事件委托
        tableBody.addEventListener('click', function(event) {
            const target = event.target;
            const row = target.closest('tr');
            if (!row) return;
            console.debug("[TimesheetCode] Click detected inside table body. Target:", target);

            if (target.classList.contains('edit-btn')) {
                console.debug("[TimesheetCode] Edit button clicked.");
                const existingEditRow = tableBody.querySelector('.editing-row');
                if (existingEditRow && existingEditRow !== row) {
                    AppUtils.showMessage('请先完成当前编辑或取消', 'warning');
                    existingEditRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    existingEditRow.classList.add('animate-pulse');
                    setTimeout(() => existingEditRow.classList.remove('animate-pulse'), 1500);
                    return;
                }
                if (row.classList.contains('editing-row')) return;

                const tsBm = row.getAttribute('data-id');
                console.log(`[TimesheetCode] Fetching data for edit, ID: ${tsBm}`);
                AppUtils.get(`${API_BASE_URL}/${encodeURIComponent(tsBm)}`).then(data => {
                    if(data) makeRowEditable(row, data, false); // 异步加载业务类型选项
                    else AppUtils.showMessage('无法获取工时编码数据进行编辑', 'error');
                }).catch(err => {
                    console.error("[TimesheetCode] 获取编辑数据失败:", err);
                    AppUtils.showMessage(`获取编辑数据失败: ${err.message}`, 'error');
                });
            } else if (target.classList.contains('delete-btn')) {
                console.debug("[TimesheetCode] Delete button clicked.");
                handleDelete(row);
            } else if (target.classList.contains('save-btn')) {
                console.debug("[TimesheetCode] Save button clicked.");
                if(row.classList.contains('editing-row') || row.classList.contains('new-editing-row')){
                    handleSave(row);
                } else {
                    console.warn("[TimesheetCode] Save button clicked on non-editing row.");
                }
            }
        });
    }

    /** 初始化模块 */
    TimesheetCodeModule.init = function() {
        console.log("Initializing Timesheet Code Module...");
        tableBody = document.getElementById(TABLE_BODY_ID);
        filterInputs = document.querySelectorAll(`#${FILTER_FORM_ID} .filter-input`);
        addButton = document.getElementById(ADD_BUTTON_ID);
        clearButton = document.getElementById(CLEAR_BUTTON_ID);
        businessTypeFilterSelect = document.getElementById('tsc-filter-projectBusinessType');

        if (!tableBody || !filterInputs || !addButton || !clearButton || !businessTypeFilterSelect) {
            console.error("[TimesheetCode] Module initialization failed: Could not find required elements.",
                {tableBody, filterInputs, addButton, clearButton, businessTypeFilterSelect});
            const section = document.getElementById('timesheet-codes-section');
            if(section) section.innerHTML = '<p class="text-red-500 p-4">工时管理模块加载失败，请联系管理员。</p>';
            return;
        }

        setupEventListeners();
        loadBusinessTypeOptions(businessTypeFilterSelect, null, true, '所有类型'); // 加载业务类型过滤器选项
        fetchData(1, getFilterValues()); // 加载初始数据
    };

    window.TimesheetCodeModule = TimesheetCodeModule;

})(window);


// * **说明:**
//     * 结构与 `employee.js` 类似。
//     * `loadBusinessTypeOptions`: 新增函数，用于异步获取业务类型列表并填充下拉框。
//     * `renderRow`: 显示工时编码相关数据。
//     * `makeRowEditable`:
//         * 将 `projectBusinessType` 渲染为下拉框，并调用 `loadBusinessTypeOptions` 填充。
//         * 将 `tsBm`, `tsName`, `sTsBm` 在编辑模式设为只读。
//         * 新增模式下 `tsBm` 可输入，`tsName` 和 `sTsBm` 显示提示文字。
//     * `collectRowData`: 收集数据，将空的业务类型值设为 `null`。
//     * `handleSave`: 编辑(PUT)时只发送可修改的字段。新增(POST)时不发送同步字段。
//     * `init`: 调用 `loadBusinessTypeOptions` 填充过滤器的下拉