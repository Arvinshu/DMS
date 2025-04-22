/**
 * 员工管理模块脚本
 * 文件路径: src/main/resources/static/js/datamaintenance/employee.js
 * 职责：处理员工数据的获取、显示、过滤、分页、新增、编辑、删除操作。
 * 修正：布尔值显示、清空筛选按钮、添加保存日志。
 * 依赖: common.js, pagination.js
 */

(function(window) {
    'use strict';

    // 创建或获取模块命名空间
    const EmployeeModule = window.EmployeeModule || {};

    // --- 配置和常量 ---
    const API_BASE_URL = '/api/employees';
    const DEPARTMENTS_API_URL = '/api/departments'; // 需要部门列表
    const TABLE_BODY_ID = 'employees-table-body';
    const PAGINATION_CONTAINER_ID = 'emp-pagination-container';
    const FILTER_FORM_ID = 'emp-filters';
    const ADD_BUTTON_ID = 'add-emp-button';
    const CLEAR_BUTTON_ID = 'emp-clear-filters'; // 清空按钮 ID
    const DEFAULT_PAGE_SIZE = 50;
    const DEBOUNCE_DELAY = 300;

    // --- DOM 元素引用 ---
    let tableBody = null;
    let filterInputs = null;
    let addButton = null;
    let clearButton = null; // 清空按钮
    let departmentFilterSelect = null; // 部门过滤下拉框

    // --- 状态变量 ---
    let currentPage = 1;
    let currentFilters = {};
    let departmentOptionsCache = null; // 缓存部门选项

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
        console.debug("[Employee] Current Filters:", filters);
        return filters;
    }

    /**
     * 加载部门选项到指定的 select 元素 (复用 department.js 的逻辑，稍作调整)
     * @param {HTMLSelectElement} selectElement
     * @param {string|number} [selectedValue]
     * @param {boolean} [addEmptyOption=true]
     * @param {string} [emptyOptionText='所有部门']
     */
    async function loadDepartmentOptions(selectElement, selectedValue, addEmptyOption = true, emptyOptionText = '所有部门') {
        console.debug(`[Employee] Loading department options for select, selected: ${selectedValue}`);
        if (!selectElement) return;

        if (departmentOptionsCache) {
            populateDepartmentSelect(selectElement, departmentOptionsCache, selectedValue, addEmptyOption, emptyOptionText);
            return;
        }

        selectElement.innerHTML = `<option value="">加载中...</option>`;
        selectElement.disabled = true;

        try {
            const response = await AppUtils.get(`${DEPARTMENTS_API_URL}?size=1000`);
            departmentOptionsCache = response.data || [];
            populateDepartmentSelect(selectElement, departmentOptionsCache, selectedValue, addEmptyOption, emptyOptionText);
            console.debug(`[Employee] Loaded ${departmentOptionsCache.length} departments.`);
        } catch (error) {
            console.error("[Employee] 加载部门选项失败:", error);
            selectElement.innerHTML = `<option value="">加载失败</option>`;
            AppUtils.showMessage(`加载部门选项失败: ${error.message || '请稍后重试'}`, 'error');
        } finally {
            selectElement.disabled = false;
        }
    }

    /** 填充部门 Select */
    function populateDepartmentSelect(selectElement, options, selectedValue, addEmptyOption, emptyOptionText) {
        selectElement.innerHTML = '';
        if (addEmptyOption) {
            const emptyOpt = document.createElement('option');
            emptyOpt.value = '';
            emptyOpt.textContent = emptyOptionText;
            selectElement.appendChild(emptyOpt);
        }
        options.forEach(dept => {
            // 只添加活动的部门作为选项 (根据需要调整)
            if (dept.active) {
                const option = document.createElement('option');
                option.value = dept.id;
                option.textContent = `${dept.depName} (ID: ${dept.id})`;
                if (selectedValue != null && dept.id == selectedValue) {
                    option.selected = true;
                }
                selectElement.appendChild(option);
            }
        });
    }


    /** 创建表格行 (修正布尔值显示) */
    function renderRow(item) {
        const row = document.createElement('tr');
        row.setAttribute('data-id', item.employee); // 主键是 employee 字符串
        row.classList.add('hover:bg-gray-50');

        const departmentDisplay = item.departmentName ? `${item.departmentName} (ID: ${item.depId})` : (item.depId || '-');

        row.innerHTML = `
            <td class="table-cell text-sm font-medium">${item.employee || '-'}</td>
            <td class="table-cell text-sm">${item.employeeId || '-'}</td>
            <td class="table-cell text-sm">${item.employeeName || '-'}</td>
            <td class="table-cell text-sm">${departmentDisplay}</td>
            <td class="table-cell text-sm text-center">${item.active ? '<span class="text-green-600 font-semibold">在职</span>' : '<span class="text-red-600 font-semibold">离职</span>'}</td>
            <td class="table-cell text-sm text-center">${item.statistics ? '<span class="text-green-600 font-semibold">是</span>' : '<span class="text-red-600 font-semibold">否</span>'}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2 text-center">
                <button class="btn btn-secondary btn-sm edit-btn">修改</button>
                <button class="btn btn-danger btn-sm delete-btn">删除</button>
            </td>
        `;
        return row;
    }

    /** 使行可编辑 (修正部门下拉框) */
    async function makeRowEditable(row, initialData = {}, isNew = false) {
        console.debug("[Employee] Making row editable. isNew:", isNew, "Data:", initialData);
        row.innerHTML = '';
        row.classList.add('editing-row');

        // 员工信息 (主键)
        const cellEmployee = row.insertCell(); cellEmployee.classList.add('table-cell text-sm');
        const inputEmployee = document.createElement('input'); inputEmployee.type = 'text'; inputEmployee.name = 'employee';
        inputEmployee.value = initialData.employee || ''; inputEmployee.placeholder = '工号-姓名 (主键)';
        inputEmployee.classList.add('input-field', 'editing-cell'); inputEmployee.required = true;
        if (!isNew) { inputEmployee.readOnly = true; inputEmployee.classList.add('bg-gray-100', 'cursor-not-allowed'); }
        cellEmployee.appendChild(inputEmployee);

        // 工号 (只读或新增时输入)
        const cellEmpId = row.insertCell(); cellEmpId.classList.add('table-cell text-sm');
        const inputEmpId = document.createElement('input'); inputEmpId.type = 'text'; inputEmpId.name = 'employeeId';
        inputEmpId.value = initialData.employeeId || ''; inputEmpId.placeholder = '输入工号';
        inputEmpId.classList.add('input-field', 'w-24', 'editing-cell'); inputEmpId.required = true;
        if (!isNew) { inputEmpId.readOnly = true; inputEmpId.classList.add('bg-gray-100', 'cursor-not-allowed'); }
        cellEmpId.appendChild(inputEmpId);

        // 姓名 (只读或新增时输入)
        const cellEmpName = row.insertCell(); cellEmpName.classList.add('table-cell text-sm');
        const inputEmpName = document.createElement('input'); inputEmpName.type = 'text'; inputEmpName.name = 'employeeName';
        inputEmpName.value = initialData.employeeName || ''; inputEmpName.placeholder = '输入姓名';
        inputEmpName.classList.add('input-field', 'editing-cell'); inputEmpName.required = true;
        if (!isNew) { inputEmpName.readOnly = true; inputEmpName.classList.add('bg-gray-100', 'cursor-not-allowed'); }
        cellEmpName.appendChild(inputEmpName);

        // 所属部门 (下拉选择)
        const cellDep = row.insertCell(); cellDep.classList.add('table-cell text-sm');
        const selectDep = document.createElement('select'); selectDep.name = 'depId';
        selectDep.classList.add('select-field', 'editing-cell'); selectDep.required = true; // 部门必选
        cellDep.appendChild(selectDep);
        await loadDepartmentOptions(selectDep, initialData.depId, false, '请选择部门'); // 不允许空选项

        // 状态 (Select)
        const cellActive = row.insertCell(); cellActive.classList.add('table-cell text-sm');
        const selectActive = document.createElement('select'); selectActive.name = 'active';
        selectActive.classList.add('select-field', 'editing-cell');
        selectActive.innerHTML = `<option value="true" ${initialData.active !== false ? 'selected' : ''}>在职</option><option value="false" ${initialData.active === false ? 'selected' : ''}>离职</option>`;
        cellActive.appendChild(selectActive);

        // 参与统计 (Select)
        const cellStats = row.insertCell(); cellStats.classList.add('table-cell text-sm');
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
            console.debug("[Employee] Cancel button clicked. isNew:", isNew);
            if (isNew) { row.remove(); }
            else { const originalRow = renderRow(initialData); row.parentNode.replaceChild(originalRow, row); }
        });
    }

    /** 从编辑行收集数据 */
    function collectRowData(row) {
        const data = {};
        let isValid = true;
        const inputs = row.querySelectorAll('input[name], select[name]');
        console.debug("[Employee] Collecting data from row:", row);

        inputs.forEach(input => {
            const name = input.name;
            let value = input.value; // trim() 会移除数字0

            if (input.type === 'text' || input.type === 'number') {
                value = value.trim();
            }

            console.debug(`[Employee] Input name: ${name}, value: "${value}", required: ${input.required}`);

            if (input.required && value === '') {
                isValid = false;
                input.classList.add('border-red-500');
                console.warn(`[Employee] Validation failed: Field ${name} is required.`);
            } else {
                input.classList.remove('border-red-500');
            }

            // 类型转换
            if (name === 'depId') {
                value = value === '' ? null : parseInt(value, 10);
                if (value === null && input.required) { // 再次检查部门是否必选
                    isValid = false; input.classList.add('border-red-500');
                    console.warn(`[Employee] Validation failed: Field ${name} is required.`);
                }
            } else if (input.tagName === 'SELECT' && (name === 'active' || name === 'statistics')) {
                value = value === 'true';
            }

            if (name) {
                data[name] = value;
            }
        });

        // 特殊验证：employee 格式 "工号-姓名"
        if (data.employee && !/^[a-zA-Z0-9]+-.+$/.test(data.employee)) {
            isValid = false;
            const empInput = row.querySelector('input[name="employee"]');
            if(empInput) empInput.classList.add('border-red-500');
            AppUtils.showMessage('员工信息格式应为 "工号-姓名"', 'warning');
            console.warn("[Employee] Validation failed: employee format incorrect.");
        } else {
            const empInput = row.querySelector('input[name="employee"]');
            if(empInput) empInput.classList.remove('border-red-500');
        }

        console.debug("[Employee] Collected data:", data, "isValid:", isValid);
        if (!isValid) {
            AppUtils.showMessage('请检查所有必填项和格式', 'warning');
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
            tableBody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-gray-500">暂无数据</td></tr>`;
            return;
        }
        data.forEach(item => {
            const row = renderRow(item);
            tableBody.appendChild(row);
        });
    }

    /** 获取数据并更新UI */
    async function fetchData(page = 1, filters = {}) {
        console.debug(`[Employee] Fetching data. Page: ${page}, Filters:`, filters);
        currentPage = page;
        currentFilters = filters;
        const loadingRow = `<tr><td colspan="7" class="text-center py-4 loading-row"><div class="loader"></div></td></tr>`;
        if(tableBody) tableBody.innerHTML = loadingRow;
        if (window.AppUtils && typeof window.AppUtils.setupPagination === 'function') {
            window.AppUtils.setupPagination({ containerId: PAGINATION_CONTAINER_ID, currentPage: 1, totalPages: 0, totalRecords: 0, onPageChange: ()=>{} });
        }

        const params = new URLSearchParams({ page: page, size: DEFAULT_PAGE_SIZE, ...filters });
        const url = `${API_BASE_URL}?${params.toString()}`;

        try {
            const response = await AppUtils.get(url);
            console.debug("[Employee] API response received:", response);
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
            console.error('[Employee] 获取员工数据失败:', error);
            if(tableBody) tableBody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-red-500">加载数据失败: ${error.message || '未知错误'}</td></tr>`;
            AppUtils.showMessage(`加载员工数据失败: ${error.message || '请稍后重试'}`, 'error');
        }
    }

    /** 处理新增 */
    function handleAddNew() {
        console.debug("[Employee] Add New button clicked.");
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
        makeRowEditable(newRow, {}, true); // 异步加载部门选项
        newRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    /** 处理保存 */
    async function handleSave(row) {
        const isNew = row.classList.contains('new-editing-row');
        console.debug(`[Employee] Save button clicked. isNew: ${isNew}, Row:`, row);
        const data = collectRowData(row);

        if (!data) {
            console.warn("[Employee] Save aborted due to invalid data.");
            return;
        }

        const saveButton = row.querySelector('.save-btn');
        const cancelButton = row.querySelector('.cancel-btn');
        if(saveButton) saveButton.disabled = true; saveButton.textContent = '保存中...';
        if(cancelButton) cancelButton.disabled = true;

        try {
            if (isNew) {
                console.log("[Employee] Attempting to add employee:", data);
                await AppUtils.post(API_BASE_URL, data);
                AppUtils.showMessage('员工添加成功!', 'success');
            } else {
                const employeeId = row.getAttribute('data-id'); // 主键
                // 编辑时只发送可修改的字段
                const updateData = {
                    depId: data.depId,
                    active: data.active,
                    statistics: data.statistics
                    // employeeId 和 employeeName 不应由用户修改
                };
                console.log(`[Employee] Attempting to update employee ${employeeId}:`, updateData);
                await AppUtils.put(`${API_BASE_URL}/${encodeURIComponent(employeeId)}`, updateData);
                AppUtils.showMessage('员工更新成功!', 'success');
            }
            fetchData(currentPage, currentFilters); // 刷新当前页
        } catch (error) {
            console.error('[Employee] 保存员工失败:', error);
            AppUtils.showMessage(`保存失败: ${error.message || '请检查输入或联系管理员'}`, 'error');
            if(saveButton) saveButton.disabled = false; saveButton.textContent = '保存';
            if(cancelButton) cancelButton.disabled = false;
        }
    }

    /** 处理删除 */
    async function handleDelete(row) {
        const employeeId = row.getAttribute('data-id'); // 主键
        const empName = row.cells[2].textContent;
        console.debug(`[Employee] Delete button clicked for ID: ${employeeId}, Name: ${empName}`);

        if (!employeeId) return;
        if (!confirm(`确定要删除员工 "${empName}" (${employeeId}) 吗？`)) {
            console.debug("[Employee] Delete cancelled by user.");
            return;
        }

        const deleteButton = row.querySelector('.delete-btn');
        const editButton = row.querySelector('.edit-btn');
        if(deleteButton) deleteButton.disabled = true;
        if(editButton) editButton.disabled = true;

        try {
            console.log(`[Employee] Attempting to delete employee ${employeeId}`);
            await AppUtils.delete(`${API_BASE_URL}/${encodeURIComponent(employeeId)}`);
            AppUtils.showMessage('员工删除成功!', 'success');
            row.remove();
            // 可选：检查是否需要加载前一页
            const currentRows = tableBody.querySelectorAll('tr:not(.editing-row)');
            if (currentRows.length === 0 && currentPage > 1) {
                console.log("[Employee] Current page empty after delete, fetching previous page.");
                fetchData(currentPage - 1, currentFilters);
            }
        } catch (error) {
            console.error('[Employee] 删除员工失败:', error);
            AppUtils.showMessage(`删除失败: ${error.message || '该员工可能被引用'}`, 'error');
            if(deleteButton) deleteButton.disabled = false;
            if(editButton) editButton.disabled = false;
        }
    }

    /** 设置事件监听器 */
    function setupEventListeners() {
        console.debug("[Employee] Setting up event listeners.");
        if (!tableBody || !addButton || !filterInputs || !clearButton || !departmentFilterSelect) {
            console.error("[Employee] Cannot setup listeners, required elements missing.");
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
            console.debug("[Employee] Clear filters button clicked.");
            filterInputs.forEach(input => { input.value = ''; });
            fetchData(1, {});
            AppUtils.showMessage('筛选条件已清空', 'info');
        });

        // 表格事件委托
        tableBody.addEventListener('click', function(event) {
            const target = event.target;
            const row = target.closest('tr');
            if (!row) return;
            console.debug("[Employee] Click detected inside table body. Target:", target);

            if (target.classList.contains('edit-btn')) {
                console.debug("[Employee] Edit button clicked.");
                const existingEditRow = tableBody.querySelector('.editing-row');
                if (existingEditRow && existingEditRow !== row) {
                    AppUtils.showMessage('请先完成当前编辑或取消', 'warning');
                    existingEditRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    existingEditRow.classList.add('animate-pulse');
                    setTimeout(() => existingEditRow.classList.remove('animate-pulse'), 1500);
                    return;
                }
                if (row.classList.contains('editing-row')) return;

                const employeeId = row.getAttribute('data-id');
                console.log(`[Employee] Fetching data for edit, ID: ${employeeId}`);
                AppUtils.get(`${API_BASE_URL}/${encodeURIComponent(employeeId)}`).then(data => {
                    if(data) makeRowEditable(row, data, false); // 异步加载部门选项
                    else AppUtils.showMessage('无法获取员工数据进行编辑', 'error');
                }).catch(err => {
                    console.error("[Employee] 获取编辑数据失败:", err);
                    AppUtils.showMessage(`获取编辑数据失败: ${err.message}`, 'error');
                });
            } else if (target.classList.contains('delete-btn')) {
                console.debug("[Employee] Delete button clicked.");
                handleDelete(row);
            } else if (target.classList.contains('save-btn')) {
                console.debug("[Employee] Save button clicked.");
                if(row.classList.contains('editing-row') || row.classList.contains('new-editing-row')){
                    handleSave(row);
                } else {
                    console.warn("[Employee] Save button clicked on non-editing row.");
                }
            }
        });
    }

    /** 初始化模块 */
    EmployeeModule.init = function() {
        console.log("Initializing Employee Module...");
        tableBody = document.getElementById(TABLE_BODY_ID);
        filterInputs = document.querySelectorAll(`#${FILTER_FORM_ID} .filter-input`);
        addButton = document.getElementById(ADD_BUTTON_ID);
        clearButton = document.getElementById(CLEAR_BUTTON_ID);
        departmentFilterSelect = document.getElementById('emp-filter-depId');

        if (!tableBody || !filterInputs || !addButton || !clearButton || !departmentFilterSelect) {
            console.error("[Employee] Module initialization failed: Could not find required elements.",
                {tableBody, filterInputs, addButton, clearButton, departmentFilterSelect});
            const section = document.getElementById('employees-section');
            if(section) section.innerHTML = '<p class="text-red-500 p-4">员工管理模块加载失败，请联系管理员。</p>';
            return;
        }

        setupEventListeners();
        loadDepartmentOptions(departmentFilterSelect, null, true, '所有部门'); // 加载部门过滤器选项
        fetchData(1, getFilterValues()); // 加载初始数据
    };

    window.EmployeeModule = EmployeeModule;

})(window);

//
// * **说明:**
//     * 遵循与 `department.js` 类似的结构。
//     * `loadDepartmentOptions`: 新增函数，用于异步获取部门列表并填充到 `<select>` 元素中，同时处理缓存。
//     * `renderRow`: 显示员工信息，包括从后端获取的 `departmentName`。
//     * `makeRowEditable`:
//         * 将 `depId` 字段渲染为 `<select>`，并调用 `loadDepartmentOptions` 填充。
//         * 将 `employee`, `employeeId`, `employeeName` 在编辑模式下设为只读。
//         * 新增模式下这些字段变为可输入。
//     * `collectRowData`: 收集数据时，获取 `depId` 的选中值，并添加了对 `employee` 字段格式的简单前端验证。
//     * `handleSave`: 在编辑(PUT)时，只发送可修改的字段 (`depId`, `active`, `statistics`)。
//     * `init`: 在初始化时调用 `loadDepartmentOptions` 来填充过滤器的部门下拉