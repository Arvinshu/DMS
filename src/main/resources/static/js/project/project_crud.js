/**
 * 文件路径: src/main/resources/static/js/project/project_crud.js
 * 开发时间: 2025-04-24 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 处理项目管理页面中项目的行内添加、编辑和删除操作。
 * 更新内容: 修正 populateAddRowDropdowns 中调用下拉框填充函数的方式。
 * 集成 Choices.js 用于标签多选下拉框，并管理其生命周期。
 * 依赖: common.js (AppUtils), project_api.js (ProjectApiModule), project_list.js (ProjectListModule), project_main.js (populateSelectFromMap), choices.js
 */
const ProjectCrudModule = (() => {
    // 常量定义
    const ADD_BTN_ID = 'add-project-btn';
    const ADD_ROW_ID = 'add-project-row';
    const TABLE_BODY_ID = 'project-table-body';

    // DOM 元素引用
    let addBtn;
    let addRowTemplate;
    let tableBody;

    // 存储下拉选项数据
    let lookupData = null;
    let flatpickrInstances = [];
    let activeChoicesInstances = new Map(); // 新增：用于存储 Choices.js 实例，键为 select 元素

    // Choices.js 的通用配置
    const choicesDefaultConfig = {
        removeItemButton: true,
        placeholder: true,
        placeholderValue: '选择或搜索标签...',
        allowHTML: false,
        searchResultLimit: 15, // 增加搜索结果显示数量
        itemSelectText: '', // 提示文字
        // 限制下拉列表的高度，以便更好地处理大量数据
        // 注意：这个配置项在 Choices.js v10+ 中可能不直接存在，需要通过 CSS 控制 .choices__list--dropdown
        // searchFloor: 3, // 至少输入多少字符才开始搜索
        // searchChoices: true,
    };

    // 初始化函数
    function init(lookups) { // 移除 choicesMap 参数，改为内部管理
        console.log('Initializing ProjectCrudModule...');
        lookupData = lookups;

        addBtn = document.getElementById(ADD_BTN_ID);
        addRowTemplate = document.getElementById(ADD_ROW_ID);
        tableBody = document.getElementById(TABLE_BODY_ID);

        if (!addBtn || !addRowTemplate || !tableBody) {
            console.error('ProjectCrudModule: Required DOM elements not found.');
            return;
        }

        addBtn.addEventListener('click', handleShowAddRow);
        tableBody.addEventListener('click', handleTableButtonClick);
        // addRowTemplate 上的点击事件现在由 handleTableButtonClick 统一处理，因为它也是 tableBody 的一部分（虽然是隐藏的）
        // addRowTemplate.addEventListener('click', handleAddRowButtonClick); // 可以移除或保留，但注意事件冒泡

        console.log('ProjectCrudModule initialized.');
    }

    /**
     * 初始化指定 select 元素的 Choices.js 实例
     * @param {HTMLSelectElement} selectElement
     * @param {Array<string|number>} [selectedValues=[]] - 预选中的值数组
     */
    function initializeChoices(selectElement, selectedValues = []) {
        if (!selectElement || typeof Choices === 'undefined') return;
        // 先销毁可能存在的旧实例
        destroyChoicesForElement(selectElement);

        const instance = new Choices(selectElement, {
            ...choicesDefaultConfig, // 使用默认配置
            // classNames: { // 可选：根据需要添加自定义类名
            //     containerOuter: `choices choices-crud-outer ${selectElement.classList.contains('text-sm') ? 'choices-sm' : ''}`,
            //     containerInner: 'choices__inner form-control',
            // }
        });
        // 设置预选值 (Choices.js 会在初始化时处理 select 元素的 selected options)
        // 如果 populateSelect 已经处理了 selected, Choices 初始化时会读取
        // 如果需要通过 API 设置，可以使用 instance.setValue(selectedValues);
        activeChoicesInstances.set(selectElement, instance);
        console.debug('Choices instance initialized for:', selectElement, 'with values:', selectedValues);
    }

    /**
     * 销毁指定 select 元素的 Choices.js 实例
     * @param {HTMLSelectElement} selectElement
     */
    function destroyChoicesForElement(selectElement) {
        if (selectElement && activeChoicesInstances.has(selectElement)) {
            try {
                activeChoicesInstances.get(selectElement).destroy();
            } catch (e) {
                console.warn('Error destroying Choices instance:', e, 'for element:', selectElement);
            }
            activeChoicesInstances.delete(selectElement);
            console.debug('Choices instance destroyed for:', selectElement);
        }
    }

    /**
     * 销毁指定行内所有 select[multiple] 元素的 Choices.js 实例
     * @param {HTMLElement} rowElement
     */
    function destroyChoicesInRow(rowElement) {
        if (!rowElement) return;
        const multiSelects = rowElement.querySelectorAll('select[multiple]');
        multiSelects.forEach(select => destroyChoicesForElement(select));
    }


    // 显示新增项目行
    function handleShowAddRow() {
        if (tableBody.querySelector('.editing-row') || addRowTemplate.style.display !== 'none') {
            AppUtils.showMessage('请先完成当前编辑或新增操作。', 'warning');
            return;
        }
        clearAndPrepareAddRow(); // 会销毁旧的 Choices 实例
        populateAddRowDropdowns(); // 会重新初始化 Choices 实例
        initializeFlatpickr(addRowTemplate.querySelector('input[name="createdAt"]'));
        addRowTemplate.style.display = ''; // 显示行
        // 确保新增行在表格的顶部（thead之后，第一个数据行之前）
        const firstDataRow = tableBody.querySelector('tr:not(#add-project-row):not(.task-details-row)');
        if (firstDataRow) {
            tableBody.insertBefore(addRowTemplate, firstDataRow);
        } else {
            tableBody.appendChild(addRowTemplate); // 如果没有数据行，则追加
        }
    }

    // 清理并准备新增行
    function clearAndPrepareAddRow() {
        // 新增：销毁新增行中所有多选下拉框的 Choices 实例
        destroyChoicesInRow(addRowTemplate);

        const inputs = addRowTemplate.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            if (input.type === 'checkbox' || input.type === 'radio') {
                input.checked = false;
            } else if (input.multiple) {
                // 对于原生 select, 清空选项
                Array.from(input.options).forEach(option => option.selected = false);
                // Choices.js 实例已销毁，重新填充时会新建
            } else {
                input.value = '';
            }
        });
        // Set default date for createdAt using flatpickr config if needed
    }


    // 填充新增行的下拉框
    function populateAddRowDropdowns() {
        if (!lookupData) {
            console.error("Lookup data not available for populating dropdowns.");
            return;
        }
        const tagSelect = addRowTemplate.querySelector('select[name="tagIds"]');
        populateSelect(tagSelect, lookupData.tags || [], 'tagId', 'tagName', true);
        initializeChoices(tagSelect); // 新增：初始化 Choices.js

        populateSelectFromMap(addRowTemplate.querySelector('select[name="businessTypeName"]'), lookupData.businessTypes || [], 'value', 'text');
        populateSelectFromMap(addRowTemplate.querySelector('select[name="profitCenterZone"]'), lookupData.profitCenters || [], 'value', 'text');
    }


    // 处理表格体内的按钮点击 (编辑、删除、保存编辑、取消编辑、保存新增、取消新增)
    function handleTableButtonClick(event) {
        const target = event.target;
        const row = target.closest('tr'); // 这可能是数据行，也可能是 add-project-row

        if (!row || row.classList.contains('task-details-row')) return; // 忽略任务详情行内的点击

        const projectId = row.dataset.projectId; // 对于数据行
        const isAddRow = row.id === ADD_ROW_ID;

        if (target.classList.contains('edit-project-btn') && !isAddRow) {
            handleEditProject(row, projectId);
        } else if (target.classList.contains('delete-project-btn') && !isAddRow) {
            handleDeleteProject(row, projectId);
        } else if (target.classList.contains('save-edit-btn') && !isAddRow) { // 保存编辑
            handleSaveEdit(row, projectId);
        } else if (target.classList.contains('cancel-edit-btn') && !isAddRow) { // 取消编辑
            handleCancelEdit(row, projectId);
        } else if (target.classList.contains('save-new-project-btn') && isAddRow) { // 保存新增
            handleSaveNewProject();
        } else if (target.classList.contains('cancel-add-project-btn') && isAddRow) { // 取消新增
            handleCancelAddProject();
        }
    }

    // 处理编辑项目按钮点击
    async function handleEditProject(row, projectId) {
        if (tableBody.querySelector('.editing-row:not([data-project-id="' + projectId + '"])') || addRowTemplate.style.display !== 'none') {
            AppUtils.showMessage('请先完成当前编辑或新增操作。', 'warning');
            return;
        }
        AppUtils.showLoading(row);
        try {
            // 新增：在转换为编辑模式前，销毁该行可能存在的 Choices 实例 (虽然理论上显示行没有)
            destroyChoicesInRow(row);

            const projectData = await ProjectApiModule.getProjectDetail(projectId);
            if(!projectData) {
                AppUtils.showMessage('无法获取项目数据进行编辑。', 'error');
                return;
            }
            const editableRow = createEditableProjectRow(projectData); // editableRow 内部会初始化 Choices
            replaceRow(row, editableRow);
            initializeFlatpickr(editableRow.querySelector('input[name="createdAt"]'));
        } catch (error) {
            console.error(`Error preparing project ${projectId} for editing:`, error);
            AppUtils.showMessage('准备编辑失败，请重试。', 'error');
            // 如果出错，可能需要恢复原始行或刷新列表
            ProjectListModule.refresh();
        } finally {
            AppUtils.hideLoading(row.parentNode ? row : tableBody); // 如果 row 被替换，在 tableBody 上隐藏
        }
    }

    // 处理删除项目按钮点击
    async function handleDeleteProject(row, projectId) {
        if (!projectId) return;
        if (!confirm(`确定要删除项目 ID 为 ${projectId} 吗？\n注意：只有当项目下没有任何任务时才能删除。`)) {
            return;
        }
        AppUtils.showLoading();
        try {
            // 新增：在删除行之前，销毁其 Choices 实例 (虽然显示行通常没有)
            destroyChoicesInRow(row);
            // 新增：销毁其关联的任务详情行中的 Choices 实例 (如果存在)
            const taskDetailsRow = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"]`);
            if (taskDetailsRow) {
                // ProjectTasksModule 应该负责其内部 Choices 实例的销毁，这里不直接操作
            }

            await ProjectApiModule.deleteProject(projectId);
            AppUtils.showMessage('项目删除成功！', 'success');
            if (taskDetailsRow) taskDetailsRow.remove();
            row.remove();
            ProjectListModule.refresh(); // 刷新列表
        } catch (error) {
            console.error('Error deleting project:', error);
            AppUtils.showMessage(`删除项目失败: ${error.message || '请重试'}`, 'error');
        } finally {
            AppUtils.hideLoading();
        }
    }

    // 处理保存新项目
    async function handleSaveNewProject() {
        const projectData = getEditableRowData(addRowTemplate);
        if (!projectData.projectName) {
            AppUtils.showMessage('项目名称不能为空。', 'error');
            addRowTemplate.querySelector('input[name="projectName"]')?.focus();
            return;
        }
        AppUtils.showLoading(addRowTemplate);
        try {
            await ProjectApiModule.createProject(projectData);
            AppUtils.showMessage('项目添加成功！', 'success');

            // 新增：销毁新增行的 Choices 和 Flatpickr 实例
            destroyChoicesInRow(addRowTemplate);
            destroyFlatpickrInstances(addRowTemplate); // 沿用之前的 Flatpickr 销毁逻辑

            addRowTemplate.style.display = 'none'; // 先隐藏
            clearAndPrepareAddRow(); // 清理内容（这也会再次调用 destroyChoicesInRow）
            ProjectListModule.refresh(); // 刷新列表
        } catch (error) {
            console.error('Error creating project:', error);
            // 错误信息已在 AppUtils.post 中处理，这里可以根据需要添加额外处理
        } finally {
            AppUtils.hideLoading(addRowTemplate);
        }
    }

    // 处理取消添加项目
    function handleCancelAddProject() {
        // 新增：销毁新增行的 Choices 和 Flatpickr 实例
        destroyChoicesInRow(addRowTemplate);
        destroyFlatpickrInstances(addRowTemplate); // 沿用之前的 Flatpickr 销毁逻辑

        addRowTemplate.style.display = 'none';
        clearAndPrepareAddRow(); // 清理内容
    }


    // 处理保存编辑
    async function handleSaveEdit(row, projectId) {
        const projectData = getEditableRowData(row);
        if (!projectData.projectName) {
            AppUtils.showMessage('项目名称不能为空。', 'error');
            row.querySelector('input[name="projectName"]')?.focus();
            return;
        }
        AppUtils.showLoading(row);
        try {
            // 新增：在更新前，销毁当前编辑行中的 Choices 和 Flatpickr 实例
            destroyChoicesInRow(row);
            destroyFlatpickrInstances(row);

            const updatedProject = await ProjectApiModule.updateProject(projectId, projectData);
            AppUtils.showMessage('项目更新成功！', 'success');
            const displayRow = ProjectListModule.createProjectRow(updatedProject, row.rowIndex); // 传递序号
            replaceRow(row, displayRow);
            // 新的 displayRow 是纯 HTML，不需要 Choices 初始化
        } catch (error) {
            console.error('Error updating project:', error);
            // 如果保存失败，理论上应该恢复编辑行并重新初始化 Choices/Flatpickr，或者刷新整个列表
            // 为简单起见，这里先刷新列表
            ProjectListModule.refresh();
        } finally {
            // 如果 row 仍然在 DOM 中 (例如保存失败)，则在其上隐藏 loading
            // 如果 row 被替换了，则在 tableBody 上隐藏 (或者 ProjectListModule.refresh() 会处理)
            AppUtils.hideLoading(document.body.contains(row) ? row : tableBody);
        }
    }

    // 处理取消编辑
    async function handleCancelEdit(row, projectId) {
        AppUtils.showLoading(row);
        try {
            // 新增：销毁当前编辑行中的 Choices 和 Flatpickr 实例
            destroyChoicesInRow(row);
            destroyFlatpickrInstances(row);

            const project = await ProjectApiModule.getProjectDetail(projectId);
            if(project) {
                const displayRow = ProjectListModule.createProjectRow(project, row.rowIndex); // 传递序号
                replaceRow(row, displayRow);
            } else {
                AppUtils.showMessage('无法获取原始项目数据。', 'warning');
                ProjectListModule.refresh(); // 刷新列表
            }
        } catch(error) {
            console.error('Error cancelling edit:', error);
            AppUtils.showMessage('取消编辑失败，请重试。', 'error');
            ProjectListModule.refresh(); // 出错时也刷新列表
        } finally {
            AppUtils.hideLoading(document.body.contains(row) ? row : tableBody);
        }
    }


    // 创建项目的编辑行 HTML
    function createEditableProjectRow(project) {
        const row = document.createElement('tr');
        row.classList.add('editing-row'); // 标记为编辑行
        row.dataset.projectId = project.projectId;
        const selectedTagIds = (project.tags || []).map(tag => String(tag.tagId)); // 确保是字符串数组
        const createdAtValue = AppUtils.formatDateIfPresent(project.createdAt, 'Y-m-d');

        const statusClass = ProjectListModule.getStatusClass ? ProjectListModule.getStatusClass(project.projectStatus) : '';
        const statusHtml = `<span class="status ${statusClass}">${AppUtils.escapeHTML(project.projectStatus ?? '未知')}</span>`;

        row.innerHTML = `
            <td class="text-center"><i class="icon-edit"></i></td>
            <td><input type="text" name="projectName" value="${AppUtils.escapeHTML(project.projectName ?? '')}" required class="form-input text-sm"></td>
            <td><input type="text" name="createdAt" value="${createdAtValue}" class="form-input text-sm flatpickr-input" placeholder="创建日期"></td>
            <td><select name="tagIds" multiple class="form-multiselect text-sm" placeholder="选择或搜索标签..."></select></td>
            <td><select name="businessTypeName" class="form-select text-sm"><option value="">选择业务类型</option></select></td>
            <td><select name="profitCenterZone" class="form-select text-sm"><option value="">选择利润中心</option></select></td>
            <td class="text-sm">${AppUtils.escapeHTML(project.currentStageName ?? '-')}</td>
            <td class="text-sm text-center">${statusHtml}</td>
            <td class="text-sm text-center">N/A</td> <td class="text-center">
                <button class="btn btn-xs btn-save save-edit-btn">保存</button>
                <button class="btn btn-xs btn-cancel cancel-edit-btn">取消</button>
            </td>
        `;

        if (lookupData) {
            const tagSelectElement = row.querySelector('select[name="tagIds"]');
            populateSelect(tagSelectElement, lookupData.tags || [], 'tagId', 'tagName', true, selectedTagIds);
            initializeChoices(tagSelectElement, selectedTagIds); // 新增：初始化 Choices.js 并传递选中值

            populateSelectFromMap(row.querySelector('select[name="businessTypeName"]'), lookupData.businessTypes || [], 'value', 'text', project.businessTypeName);
            populateSelectFromMap(row.querySelector('select[name="profitCenterZone"]'), lookupData.profitCenters || [], 'value', 'text', project.profitCenterZone);
        } else {
            console.warn("Lookup data not available for editable row dropdowns.");
        }
        return row;
    }

    // 从编辑行（包括新增行）获取数据
    // 这个函数是组装项目信息，并发送至后端进行写入数据库操作。
    function getEditableRowData(row) {
        const data = {};
        data.projectName = row.querySelector('input[name="projectName"]')?.value.trim();
        data.createdAt = row.querySelector('input[name="createdAt"]')?.value; // 获取日期字符串
        data.businessTypeName = row.querySelector('select[name="businessTypeName"]')?.value;
        data.profitCenterZone = row.querySelector('select[name="profitCenterZone"]')?.value;

        // 修改后（获取选中项的text）
        // const businessTypeSelect = row.querySelector('select[name="businessTypeName"]');
        // data.businessTypeName = businessTypeSelect?.selectedOptions[0]?.textContent.trim() || '';

        //写入t_project表中的值为自定义项目利润中心。但是这样做会违反外键关联的t_profit_center唯一约束
        // const profitCenterSelect = row.querySelector('select[name="profitCenterZone"]');
        // data.profitCenterZone = profitCenterSelect?.selectedOptions[0]?.textContent.trim() || '';


        const tagSelect = row.querySelector('select[name="tagIds"]');
        // 如果是 Choices.js 实例，需要从实例获取值
        if (activeChoicesInstances.has(tagSelect)) {
            const choicesInstance = activeChoicesInstances.get(tagSelect);
            data.tagIds = choicesInstance.getValue(true); // 获取纯值数组
        } else if (tagSelect) { // Fallback for native select
            data.tagIds = Array.from(tagSelect.selectedOptions).map(option => option.value).filter(val => val);
        } else {
            data.tagIds = [];
        }
        // 确保 tagIds 是数字数组 (如果后端需要)
        data.tagIds = data.tagIds.map(id => parseInt(id, 10)).filter(id => !isNaN(id));


        for (const key in data) {
            if (data[key] === '') {
                data[key] = null;
            }
        }
        // 如果 createdAt 是空字符串，确保发送 null 或不发送
        if (data.createdAt === '') data.createdAt = null;

        return data;
    }

    // 填充下拉框选项 (从 project_main.js 移入或共享)
    function populateSelect(selectElement, options, valueField, textField, isMultiple = false, selectedValue = null) {
        if (!selectElement || !Array.isArray(options)) return;
        const firstOptionHTML = selectElement.options[0] && selectElement.options[0].value === '' ? selectElement.options[0].outerHTML : '';
        selectElement.innerHTML = firstOptionHTML;

        options.forEach(optionData => {
            const option = document.createElement('option');
            option.value = optionData[valueField];
            option.textContent = optionData[textField];
            if (selectedValue) {
                const currentOptionValueStr = String(optionData[valueField]);
                if (isMultiple && Array.isArray(selectedValue) && selectedValue.map(String).includes(currentOptionValueStr)) {
                    option.selected = true;
                } else if (!isMultiple && String(selectedValue) === currentOptionValueStr) {
                    option.selected = true;
                }
            }
            selectElement.appendChild(option);
        });
    }
    // 从 project_main.js 移入或共享
    function populateSelectFromMap(selectElement, options, valueKey, textKey, selectedValue = null) {
        if (!selectElement || !Array.isArray(options)) return;
        const firstOptionHTML = selectElement.options[0] && selectElement.options[0].value === '' ? selectElement.options[0].outerHTML : '';
        selectElement.innerHTML = firstOptionHTML;

        options.forEach(optionData => {
            const option = document.createElement('option');
            option.value = optionData[valueKey];
            option.textContent = optionData[textKey];
            if (selectedValue && String(optionData[valueKey]) === String(selectedValue)) {
                option.selected = true;
            }
            selectElement.appendChild(option);
        });
    }


    // 替换表格行
    function replaceRow(oldRow, newRow) {
        oldRow.parentNode.replaceChild(newRow, oldRow);
    }

    // 初始化 Flatpickr
    function initializeFlatpickr(element) {
        if (element && typeof flatpickr !== 'undefined') {
            // 销毁可能存在的旧实例
            const existingInstanceData = flatpickrInstances.find(item => item.element === element);
            if (existingInstanceData && existingInstanceData.instance) {
                existingInstanceData.instance.destroy();
                flatpickrInstances = flatpickrInstances.filter(item => item.element !== element);
            }

            const instance = flatpickr(element, {
                dateFormat: "Y-m-d",
                locale: "zh",
                allowInput: true
            });
            flatpickrInstances.push({element: element, instance: instance});
        }
    }

    // 销毁指定行内的 Flatpickr 实例
    function destroyFlatpickrInstances(row) {
        const inputs = row.querySelectorAll('.flatpickr-input');
        inputs.forEach(input => {
            const instanceDataIndex = flatpickrInstances.findIndex(item => item.element === input);
            if (instanceDataIndex > -1) {
                const instanceData = flatpickrInstances[instanceDataIndex];
                if (instanceData.instance) {
                    try {
                        instanceData.instance.destroy();
                    } catch(e) { console.warn("Error destroying flatpickr:", e); }
                }
                flatpickrInstances.splice(instanceDataIndex, 1);
            }
        });
    }


    // 暴露公共接口
    return {
        init: init
    };
})();
