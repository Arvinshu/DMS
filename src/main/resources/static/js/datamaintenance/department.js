/**
 * 部门管理模块脚本
 * 文件路径: src/main/resources/static/js/datamaintenance/department.js
 * 职责：处理部门数据的获取、显示、过滤、分页、新增、编辑、删除操作。
 * 依赖: common.js, pagination.js
 */

// 创建模块命名空间，并挂载到 window 对象，以便 datamaintenance_main.js 调用 init
(function(window) {
    'use strict';

    // 创建或获取模块命名空间
    const DepartmentModule = window.DepartmentModule || {};

    // --- 配置和常量 ---
    const API_BASE_URL = '/api/departments';
    const EMPLOYEES_API_URL = '/api/employees'; // 获取员工列表 API
    const TABLE_BODY_ID = 'departments-table-body';
    const PAGINATION_CONTAINER_ID = 'dept-pagination-container';
    const FILTER_FORM_ID = 'dept-filters';
    const ADD_BUTTON_ID = 'add-dept-button';
    const CLEAR_BUTTON_ID = 'dept-clear-filters'; // 清空筛选按钮 ID
    const DEFAULT_PAGE_SIZE = 50;
    const DEBOUNCE_DELAY = 300;

    // --- DOM 元素引用 ---
    let tableBody = null;
    let filterInputs = null;
    let addButton = null;
    let clearButton = null; // 清空按钮引用

    // --- 状态变量 ---
    let currentPage = 1;
    let currentFilters = {};
    let employeeOptionsCache = null; // 缓存员工选项

    // --- 函数定义 ---

    /** 获取过滤器值 */
    function getFilterValues() {
        const filters = {};
        if (!filterInputs) return filters;
        filterInputs.forEach(input => {
            // 使用 input 的 name 属性作为 key
            const name = input.name;
            const value = input.value.trim();
            if (name && value !== '') {
                filters[name] = value;
            }
        });
        console.debug("[Department] Current Filters:", filters); // 调试日志
        return filters;
    }

    /**
     * 加载员工选项到 Select (用于负责人/副经理)
     * @param {HTMLSelectElement} selectElement
     * @param {string} [selectedValue] - 员工主键字符串 (工号-姓名)
     * @param {boolean} [addEmptyOption=true]
     * @param {string} [emptyOptionText='-- 无 --']
     */
    async function loadEmployeeOptions(selectElement, selectedValue, addEmptyOption = true, emptyOptionText = '-- 无 --') {
        console.debug(`[Department] Loading employee options for select, selected: ${selectedValue}`);
        if (!selectElement) return;

        // 优先使用缓存
        if (employeeOptionsCache) {
            populateEmployeeSelect(selectElement, employeeOptionsCache, selectedValue, addEmptyOption, emptyOptionText);
            return;
        }

        selectElement.innerHTML = `<option value="">加载中...</option>`;
        selectElement.disabled = true;

        try {
            // 获取员工列表（仅获取必要的字段和在职员工，需要后端支持或前端过滤）
            // 增加了 active=true 参数，假设后端支持此过滤
            const response = await AppUtils.get(`${EMPLOYEES_API_URL}?size=1000&active=true`);
            employeeOptionsCache = response.data || []; // 缓存结果
            populateEmployeeSelect(selectElement, employeeOptionsCache, selectedValue, addEmptyOption, emptyOptionText);
            console.debug(`[Department] Loaded ${employeeOptionsCache.length} active employees.`);
        } catch (error) {
            console.error("[Department] 加载员工选项失败:", error);
            selectElement.innerHTML = `<option value="">加载失败</option>`;
            AppUtils.showMessage(`加载员工选项失败: ${error.message || '请稍后重试'}`, 'error');
        } finally {
            selectElement.disabled = false;
        }
    }

    /** 填充员工 Select */
    function populateEmployeeSelect(selectElement, options, selectedValue, addEmptyOption, emptyOptionText) {
        selectElement.innerHTML = ''; // 清空
        if (addEmptyOption) {
            const emptyOpt = document.createElement('option');
            emptyOpt.value = ''; // 值为空字符串表示不选或清除
            emptyOpt.textContent = emptyOptionText;
            selectElement.appendChild(emptyOpt);
        }
        options.forEach(emp => {
            // 确保只添加活动员工（如果缓存中包含非活动员工）
            if (emp.active) {
                const option = document.createElement('option');
                option.value = emp.employee; // 使用 "工号-姓名" 作为值
                option.textContent = `${emp.employeeName} (${emp.employeeId})`; // 显示姓名和工号
                // 使用 == 进行比较，因为 selectedValue 可能是 null 或 undefined
                if (selectedValue != null && emp.employee == selectedValue) {
                    option.selected = true;
                }
                selectElement.appendChild(option);
            }
        });
    }


    /** 创建表格行 (修正布尔值显示) */
    function renderRow(item) {
        const row = document.createElement('tr');
        row.setAttribute('data-id', item.id);
        row.classList.add('hover:bg-gray-50');

        // 负责人和副经理姓名优先显示，如果不存在则显示 ID
        const managerDisplay = item.managerName ? `${item.managerName} (${item.managerId})` : (item.managerId || '-');
        const assistantManagerDisplay = item.assistantManagerName ? `${item.assistantManagerName} (${item.assistantManagerId})` : (item.assistantManagerId || '-');

        row.innerHTML = `
            <td class="table-cell text-sm font-medium text-center">${item.id}</td>
            <td class="table-cell text-sm">${item.depName || '-'}</td>
            <td class="table-cell text-sm text-center">${item.depLevel || '-'}</td>
            <td class="table-cell text-sm">${managerDisplay}</td>
            <td class="table-cell text-sm">${assistantManagerDisplay}</td>
            <td class="table-cell text-sm text-center">${item.active ? '<span class="text-green-600 font-semibold">启用</span>' : '<span class="text-red-600 font-semibold">停用</span>'}</td>
            <td class="table-cell text-sm text-center">${item.statistics ? '<span class="text-green-600 font-semibold">是</span>' : '<span class="text-red-600 font-semibold">否</span>'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2 text-center"> <button class="btn btn-secondary btn-sm edit-btn">修改</button>
                <button class="btn btn-danger btn-sm delete-btn">删除</button>
            </td>
        `;
        return row;
    }

    /** 使行可编辑 (修正下拉框和可编辑字段) */
    async function makeRowEditable(row, initialData = {}, isNew = false) {
        console.debug("[Department] Making row editable. isNew:", isNew, "Data:", initialData);
        row.innerHTML = '';
        row.classList.add('editing-row');

        // ID
        const cellId = row.insertCell(); cellId.classList.add('table-cell');
        const inputId = document.createElement('input'); inputId.type = 'number'; inputId.name = 'id';
        inputId.value = initialData.id || ''; inputId.classList.add('input-field', 'w-20', 'editing-cell');
        inputId.required = true;
        if (!isNew) { inputId.readOnly = true; inputId.classList.add('bg-gray-100', 'cursor-not-allowed'); }
        cellId.appendChild(inputId);

        // 部门名称 (变为可编辑)
        const cellDepName = row.insertCell(); cellDepName.classList.add('table-cell');
        const inputDepName = document.createElement('input'); inputDepName.type = 'text'; inputDepName.name = 'depName';
        inputDepName.value = initialData.depName || ''; inputDepName.placeholder = '输入部门名称';
        inputDepName.classList.add('input-field', 'editing-cell'); inputDepName.required = true;
        cellDepName.appendChild(inputDepName);

        // 部门层级 (改为下拉框)
        const cellLevel = row.insertCell(); cellLevel.classList.add('table-cell');
        const selectLevel = document.createElement('select'); selectLevel.name = 'depLevel';
        selectLevel.classList.add('select-field', 'editing-cell'); selectLevel.required = true;
        const levels = ['一级部门', '二级部门', '三级部门'];
        let levelFound = false;
        levels.forEach(level => {
            const option = document.createElement('option');
            option.value = level; option.textContent = level;
            if (initialData.depLevel === level) { option.selected = true; levelFound = true; }
            selectLevel.appendChild(option);
        });
        if (!levelFound && !isNew) { // 如果是编辑且值不在选项中，添加一个提示
            console.warn(`[Department] Initial depLevel "${initialData.depLevel}" not in standard options.`);
            // 可以选择添加一个禁用的选项显示原始值，或默认选中第一个
        }
        if (isNew || !levelFound) { // 新增或编辑时值无效，添加提示选项
            const defaultOption = document.createElement('option');
            defaultOption.value = ""; defaultOption.textContent = "--请选择层级--";
            defaultOption.selected = true; defaultOption.disabled = true;
            selectLevel.insertBefore(defaultOption, selectLevel.firstChild);
        }
        cellLevel.appendChild(selectLevel);

        // 负责人 ID (改为下拉框)
        const cellManager = row.insertCell(); cellManager.classList.add('table-cell');
        const selectManager = document.createElement('select'); selectManager.name = 'managerId';
        selectManager.classList.add('select-field', 'editing-cell'); // 允许为空，所以非 required
        cellManager.appendChild(selectManager);
        // 异步加载选项，传入当前值用于默认选中
        await loadEmployeeOptions(selectManager, initialData.managerId, true, '-- 无负责人 --');

        // 副经理 ID (改为下拉框)
        const cellAssistant = row.insertCell(); cellAssistant.classList.add('table-cell');
        const selectAssistant = document.createElement('select'); selectAssistant.name = 'assistantManagerId';
        selectAssistant.classList.add('select-field', 'editing-cell'); // 允许为空
        cellAssistant.appendChild(selectAssistant);
        await loadEmployeeOptions(selectAssistant, initialData.assistantManagerId, true, '-- 无副经理 --');

        // 状态 (Select)
        const cellActive = row.insertCell(); cellActive.classList.add('table-cell');
        const selectActive = document.createElement('select'); selectActive.name = 'active';
        selectActive.classList.add('select-field', 'editing-cell');
        selectActive.innerHTML = `<option value="true" ${initialData.active !== false ? 'selected' : ''}>启用</option><option value="false" ${initialData.active === false ? 'selected' : ''}>停用</option>`;
        cellActive.appendChild(selectActive);

        // 参与统计 (Select)
        const cellStats = row.insertCell(); cellStats.classList.add('table-cell');
        const selectStats = document.createElement('select'); selectStats.name = 'statistics';
        selectStats.classList.add('select-field', 'editing-cell');
        selectStats.innerHTML = `<option value="true" ${initialData.statistics !== false ? 'selected' : ''}>是</option><option value="false" ${initialData.statistics === false ? 'selected' : ''}>否</option>`;
        cellStats.appendChild(selectStats);

        // 操作按钮
        const cellActions = row.insertCell(); cellActions.classList.add('px-6', 'py-4', 'whitespace-nowrap', 'text-sm', 'font-medium', 'space-x-2', 'text-center');
        const saveButton = document.createElement('button'); saveButton.textContent = '保存'; saveButton.classList.add('btn', 'btn-primary', 'btn-sm', 'save-btn'); saveButton.type = 'button';
        const cancelButton = document.createElement('button'); cancelButton.textContent = '取消'; cancelButton.classList.add('btn', 'btn-secondary', 'btn-sm', 'cancel-btn'); cancelButton.type = 'button';
        cellActions.appendChild(saveButton); cellActions.appendChild(cancelButton);

        // 绑定取消事件
        cancelButton.addEventListener('click', () => {
            console.debug("[Department] Cancel button clicked. isNew:", isNew);
            if (isNew) { row.remove(); }
            else { const originalRow = renderRow(initialData); row.parentNode.replaceChild(originalRow, row); }
        });
        // 保存按钮事件在 setupEventListeners 中通过委托处理
    }

    /** 从编辑行收集数据 */
    function collectRowData(row) {
        const data = {};
        let isValid = true;
        const inputs = row.querySelectorAll('input[name], select[name]');
        console.debug("[Department] Collecting data from row:", row);

        inputs.forEach(input => {
            const name = input.name;
            let value = input.value; // trim() 会移除数字0，对 select 的 value 也要小心

            // 对 select 保留原始 value，对 text input 进行 trim
            if (input.type === 'text' || input.type === 'number') {
                value = value.trim();
            }

            console.debug(`[Department] Input name: ${name}, value: "${value}", required: ${input.required}`);

            // 基础验证
            if (input.required && value === '') {
                isValid = false;
                input.classList.add('border-red-500');
                console.warn(`[Department] Validation failed: Field ${name} is required.`);
            } else {
                input.classList.remove('border-red-500');
            }

            // 类型转换和特殊处理
            if (name === 'id') {
                value = value === '' ? null : parseInt(value, 10);
                if (isNaN(value)) value = null;
            } else if (input.tagName === 'SELECT' && (name === 'active' || name === 'statistics')) {
                value = value === 'true'; // 转换为布尔值
            } else if (input.tagName === 'SELECT' && (name === 'managerId' || name === 'assistantManagerId')) {
                // 如果选择的是空选项，值设为 null
                value = value === '' ? null : value;
            }

            if (name) {
                data[name] = value;
            }
        });

        console.debug("[Department] Collected data:", data, "isValid:", isValid);
        if (!isValid) {
            AppUtils.showMessage('请检查所有必填项', 'warning');
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
        console.debug(`[Department] Fetching data. Page: ${page}, Filters:`, filters);
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
            console.debug("[Department] API response received:", response);
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
            console.error('[Department] 获取部门数据失败:', error);
            if(tableBody) tableBody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-red-500">加载数据失败: ${error.message || '未知错误'}</td></tr>`;
            AppUtils.showMessage(`加载部门数据失败: ${error.message || '请稍后重试'}`, 'error');
        }
    }

    /** 处理新增按钮点击 */
    function handleAddNew() {
        console.debug("[Department] Add New button clicked.");
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
        makeRowEditable(newRow, {}, true); // 异步加载员工选项
        newRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    /** 处理保存 (包括新增和编辑) */
    async function handleSave(row) {
        const isNew = row.classList.contains('new-editing-row');
        console.debug(`[Department] Save button clicked. isNew: ${isNew}, Row:`, row);
        const data = collectRowData(row);

        if (!data) {
            console.warn("[Department] Save aborted due to invalid data.");
            return; // 验证失败
        }

        const saveButton = row.querySelector('.save-btn');
        const cancelButton = row.querySelector('.cancel-btn');
        if(saveButton) saveButton.disabled = true; saveButton.textContent = '保存中...';
        if(cancelButton) cancelButton.disabled = true;

        try {
            if (isNew) {
                console.log("[Department] Attempting to add department:", data);
                // 新增时，后端可能不需要 managerName, assistantManagerName
                const postData = { ...data };
                delete postData.managerName;
                delete postData.assistantManagerName;
                await AppUtils.post(API_BASE_URL, postData);
                AppUtils.showMessage('部门添加成功!', 'success');
            } else {
                const id = row.querySelector('input[name="id"]').value; // 获取 ID
                if (!id) throw new Error("无法获取部门 ID 进行更新");
                // 编辑时，后端可能不需要 managerName, assistantManagerName
                const putData = { ...data };
                delete putData.managerName;
                delete putData.assistantManagerName;
                console.log(`[Department] Attempting to update department ${id}:`, putData);
                await AppUtils.put(`${API_BASE_URL}/${id}`, putData);
                AppUtils.showMessage('部门更新成功!', 'success');
            }
            fetchData(currentPage, currentFilters); // 刷新当前页
        } catch (error) {
            console.error('[Department] 保存部门失败:', error);
            AppUtils.showMessage(`保存失败: ${error.message || '请检查输入或联系管理员'}`, 'error');
            if(saveButton) saveButton.disabled = false; saveButton.textContent = '保存';
            if(cancelButton) cancelButton.disabled = false;
        }
    }

    /** 处理删除 */
    async function handleDelete(row) {
        const id = row.getAttribute('data-id');
        const depName = row.cells[1].textContent;
        console.debug(`[Department] Delete button clicked for ID: ${id}, Name: ${depName}`);

        if (!id) return;
        if (!confirm(`确定要删除部门 "${depName}" (ID: ${id}) 吗？\n请确保该部门下没有员工！`)) {
            console.debug("[Department] Delete cancelled by user.");
            return;
        }

        const deleteButton = row.querySelector('.delete-btn');
        const editButton = row.querySelector('.edit-btn');
        if(deleteButton) deleteButton.disabled = true;
        if(editButton) editButton.disabled = true;

        try {
            console.log(`[Department] Attempting to delete department ${id}`);
            await AppUtils.delete(`${API_BASE_URL}/${id}`);
            AppUtils.showMessage('部门删除成功!', 'success');
            row.remove(); // 直接从 DOM 移除
            // TODO: 检查删除后当前页是否为空，如果为空且不是第一页，则加载前一页
            const currentRows = tableBody.querySelectorAll('tr:not(.editing-row)');
            if (currentRows.length === 0 && currentPage > 1) {
                console.log("[Department] Current page empty after delete, fetching previous page.");
                fetchData(currentPage - 1, currentFilters);
            } else {
                // 可选：如果只是移除，可以重新计数或保持当前页
                // fetchData(currentPage, currentFilters); // 或者不刷新
            }
        } catch (error) {
            console.error('[Department] 删除部门失败:', error);
            AppUtils.showMessage(`删除失败: ${error.message || '请检查该部门是否仍被使用'}`, 'error');
            if(deleteButton) deleteButton.disabled = false;
            if(editButton) editButton.disabled = false;
        }
    }

    /** 设置事件监听器 */
    function setupEventListeners() {
        console.debug("[Department] Setting up event listeners.");
        if (!tableBody || !addButton || !filterInputs || !clearButton) {
            console.error("[Department] Cannot setup listeners, required elements missing.");
            return;
        }

        // 过滤事件 (防抖)
        const debouncedFetch = AppUtils.debounce(() => {
            const filters = getFilterValues();
            fetchData(1, filters); // 过滤器改变，回到第一页
        }, DEBOUNCE_DELAY);

        filterInputs.forEach(input => {
            input.addEventListener('input', debouncedFetch);
            if (input.tagName === 'SELECT') {
                input.addEventListener('change', debouncedFetch);
            }
        });

        // 新增按钮事件
        addButton.addEventListener('click', handleAddNew);

        // 清空筛选按钮事件
        clearButton.addEventListener('click', () => {
            console.debug("[Department] Clear filters button clicked.");
            filterInputs.forEach(input => { input.value = ''; });
            // 手动触发一次查询以应用空过滤器并回到第一页
            fetchData(1, {});
            AppUtils.showMessage('筛选条件已清空', 'info');
        });


        // 表格体事件委托 (处理编辑、删除、保存按钮点击)
        tableBody.addEventListener('click', function(event) {
            const target = event.target;
            const row = target.closest('tr');
            if (!row) return; // 点击的不是行内的元素

            console.debug("[Department] Click detected inside table body. Target:", target);

            if (target.classList.contains('edit-btn')) {
                console.debug("[Department] Edit button clicked.");
                // 处理编辑按钮点击
                const existingEditRow = tableBody.querySelector('.editing-row');
                if (existingEditRow && existingEditRow !== row) {
                    AppUtils.showMessage('请先完成当前编辑或取消', 'warning');
                    existingEditRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    existingEditRow.classList.add('animate-pulse');
                    setTimeout(() => existingEditRow.classList.remove('animate-pulse'), 1500);
                    return;
                }
                if (row.classList.contains('editing-row')) return; // 防止重复点击

                const id = row.getAttribute('data-id');
                console.log(`[Department] Fetching data for edit, ID: ${id}`);
                AppUtils.get(`${API_BASE_URL}/${id}`).then(data => {
                    if(data) {
                        makeRowEditable(row, data, false); // 异步加载员工选项
                    } else {
                        AppUtils.showMessage('无法获取部门数据进行编辑', 'error');
                    }
                }).catch(err => {
                    console.error("[Department] 获取编辑数据失败:", err);
                    AppUtils.showMessage(`获取编辑数据失败: ${err.message}`, 'error');
                });

            } else if (target.classList.contains('delete-btn')) {
                console.debug("[Department] Delete button clicked.");
                // 处理删除按钮点击
                handleDelete(row);
            } else if (target.classList.contains('save-btn')) {
                console.debug("[Department] Save button clicked.");
                // 处理保存按钮点击 (必须是编辑行内的保存按钮)
                if(row.classList.contains('editing-row') || row.classList.contains('new-editing-row')){
                    handleSave(row);
                } else {
                    console.warn("[Department] Save button clicked on non-editing row.");
                }
            }
            // 取消按钮的事件在 makeRowEditable 中单独绑定，这里不用处理
        });
    }

    /** 初始化模块 */
    DepartmentModule.init = function() {
        console.log("Initializing Department Module...");
        // 获取 DOM 元素
        tableBody = document.getElementById(TABLE_BODY_ID);
        filterInputs = document.querySelectorAll(`#${FILTER_FORM_ID} .filter-input`);
        addButton = document.getElementById(ADD_BUTTON_ID);
        clearButton = document.getElementById(CLEAR_BUTTON_ID); // 获取清空按钮

        if (!tableBody || !filterInputs || !addButton || !clearButton) {
            console.error("[Department] Module initialization failed: Could not find required elements.",
                {tableBody, filterInputs, addButton, clearButton});
            const section = document.getElementById('departments-section');
            if(section) section.innerHTML = '<p class="text-red-500 p-4">部门管理模块加载失败，请联系管理员。</p>';
            return;
        }

        setupEventListeners(); // 设置事件监听
        fetchData(1, getFilterValues()); // 加载初始数据
        // 可以在这里预加载员工选项缓存，如果员工数量不多
        // loadEmployeeOptions(document.createElement('select'), null, false); // 预热缓存
    };

    // 将模块暴露到全局
    window.DepartmentModule = DepartmentModule;

})(window);


// * **说明:**
//     * 使用 IIFE 创建 `DepartmentModule` 命名空间。
//     * 定义了模块特定的常量（API URL, 元素 ID 等）。
//     * `init`: 模块入口函数，由 `datamaintenance_main.js` 调用。负责获取 DOM 元素引用、设置事件监听器和加载初始数据。
//     * `getFilterValues`: 获取当前模块的过滤器值。
//     * `renderRow`: 创建表格的显示行，包含数据和操作按钮。
//     * `makeRowEditable`: 将一个表格行转换为包含输入字段的可编辑状态，区分新增和编辑模式。**注意：负责人和副经理字段目前是文本框，需要后续开发替换为员工选择下拉框。**
//     * `collectRowData`: 从编辑行收集用户输入的数据，并进行简单的前端验证。
//     * `renderTable`: 更新表格内容。
//     * `fetchData`: 获取数据并驱动 UI 更新（表格和分页）。
//     * `handleAddNew`: 处理新增按钮点击，添加可编辑的新行。
//     * `handleSave`: 处理保存按钮点击（区分新增和编辑），调用相应的 API。
//     * `handleDelete`: 处理删除按钮点击，包含确认提示和 API 调用。
//     * `setupEventListeners`: 设置过滤器输入、新增按钮和表格体（事件委托）的事件