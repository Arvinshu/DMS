/**
 * 文件路径: src/main/resources/static/js/project/project_crud.js
 * 开发时间: 2025-04-24 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 处理项目管理页面中项目的行内添加、编辑和删除操作。
 * 更新内容: 修正 populateAddRowDropdowns 中调用下拉框填充函数的方式。
 * 依赖: common.js (AppUtils), project_api.js (ProjectApiModule), project_list.js (ProjectListModule), project_main.js (populateSelectFromMap)
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

    // 初始化函数
    function init(lookups) {
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
        addRowTemplate.addEventListener('click', handleAddRowButtonClick);

        console.log('ProjectCrudModule initialized.');
    }

    // 显示新增项目行
    function handleShowAddRow() {
        if (tableBody.querySelector('.editing-row') || addRowTemplate.style.display !== 'none') {
            AppUtils.showMessage('请先完成当前编辑或新增操作。', 'warning');
            return;
        }
        clearAndPrepareAddRow();
        populateAddRowDropdowns(); // 调用修正后的函数
        initializeFlatpickr(addRowTemplate.querySelector('input[name="createdAt"]'));
        addRowTemplate.style.display = '';
    }

    // 清理并准备新增行
    function clearAndPrepareAddRow() {
        const inputs = addRowTemplate.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            if (input.type === 'checkbox' || input.type === 'radio') {
                input.checked = false;
            } else if (input.multiple) {
                Array.from(input.options).forEach(option => option.selected = false);
                // TODO: Re-initialize multi-select plugin if used
            } else {
                input.value = '';
            }
        });
        // Set default date for createdAt using flatpickr config if needed
    }


    // 填充新增行的下拉框 (修正调用)
    function populateAddRowDropdowns() {
        if (!lookupData) {
            console.error("Lookup data not available for populating dropdowns.");
            return;
        }
        // 填充标签 (使用 populateSelect，因为 lookupData.tags 是 List<ProjectTagDto>)
        populateSelect(addRowTemplate.querySelector('select[name="tagIds"]'), lookupData.tags || [], 'tagId', 'tagName', true);

        // *** 修正开始: 调用 populateSelectFromMap ***
        // 填充业务类型 (lookupData.businessTypes 是 List<Map<String, Object>>)
        populateSelectFromMap(addRowTemplate.querySelector('select[name="businessTypeName"]'), lookupData.businessTypes || [], 'value', 'text');
        // 填充利润中心 (lookupData.profitCenters 是 List<Map<String, Object>>)
        populateSelectFromMap(addRowTemplate.querySelector('select[name="profitCenterZone"]'), lookupData.profitCenters || [], 'value', 'text');
        // *** 修正结束 ***

        // 填充其他下拉框 (如果添加了)
        // populateSelectFromMap(addRowTemplate.querySelector('select[name="projectManagerEmployee"]'), lookupData.employees || [], 'value', 'text');
        // populateSelectFromMap(addRowTemplate.querySelector('select[name="tsBm"]'), lookupData.timesheetCodes || [], 'value', 'text');

        // TODO: Initialize multi-select plugin if necessary for tags
    }


    // 处理表格体内的按钮点击 (编辑、删除、保存编辑、取消编辑)
    function handleTableButtonClick(event) {
        const target = event.target;
        const row = target.closest('tr');
        if (!row || row.id === ADD_ROW_ID || row.classList.contains('task-details-row')) return;

        const projectId = row.dataset.projectId;

        if (target.classList.contains('edit-project-btn')) {
            handleEditProject(row, projectId);
        } else if (target.classList.contains('delete-project-btn')) {
            handleDeleteProject(row, projectId);
        } else if (target.classList.contains('save-edit-btn')) {
            handleSaveEdit(row, projectId);
        } else if (target.classList.contains('cancel-edit-btn')) {
            handleCancelEdit(row, projectId);
        }
    }

    // 处理新增行的按钮点击 (保存、取消)
    function handleAddRowButtonClick(event) {
        const target = event.target;
        if (target.classList.contains('save-new-project-btn')) {
            handleSaveNewProject();
        } else if (target.classList.contains('cancel-add-project-btn')) {
            handleCancelAddProject();
        }
    }


    // 处理编辑项目按钮点击
    async function handleEditProject(row, projectId) {
        if (tableBody.querySelector('.editing-row') || addRowTemplate.style.display !== 'none') {
            AppUtils.showMessage('请先完成当前编辑或新增操作。', 'warning');
            return;
        }
        AppUtils.showLoading(row);
        try {
            const projectData = await ProjectApiModule.getProjectDetail(projectId);
            if(!projectData) {
                AppUtils.showMessage('无法获取项目数据进行编辑。', 'error');
                return;
            }
            const editableRow = createEditableProjectRow(projectData);
            replaceRow(row, editableRow);
            initializeFlatpickr(editableRow.querySelector('input[name="createdAt"]'));
            // TODO: Initialize multi-select plugin if necessary for tags
        } catch (error) {
            console.error(`Error preparing project ${projectId} for editing:`, error);
            AppUtils.showMessage('准备编辑失败，请重试。', 'error');
        } finally {
            AppUtils.hideLoading(row);
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
            await ProjectApiModule.deleteProject(projectId);
            AppUtils.showMessage('项目删除成功！', 'success');
            const taskRow = tableBody.querySelector(`tr.task-details-row[data-project-id="${projectId}"]`);
            if (taskRow) taskRow.remove();
            row.remove();
            ProjectListModule.refresh(); // 刷新列表
        } catch (error) {
            console.error('Error deleting project:', error);
        } finally {
            AppUtils.hideLoading();
        }
    }

    // 处理保存新项目
    async function handleSaveNewProject() {
        const projectData = getEditableRowData(addRowTemplate);
        if (!projectData.projectName) {
            AppUtils.showMessage('项目名称不能为空。', 'error');
            return;
        }
        AppUtils.showLoading(addRowTemplate);
        try {
            await ProjectApiModule.createProject(projectData);
            AppUtils.showMessage('项目添加成功！', 'success');
            addRowTemplate.style.display = 'none';
            destroyFlatpickrInstances(addRowTemplate);
            ProjectListModule.refresh(); // 刷新列表
        } catch (error) {
            console.error('Error creating project:', error);
        } finally {
            AppUtils.hideLoading(addRowTemplate);
        }
    }

    // 处理取消添加项目
    function handleCancelAddProject() {
        addRowTemplate.style.display = 'none';
        destroyFlatpickrInstances(addRowTemplate);
        clearAndPrepareAddRow();
    }


    // 处理保存编辑
    async function handleSaveEdit(row, projectId) {
        const projectData = getEditableRowData(row);
        if (!projectData.projectName) {
            AppUtils.showMessage('项目名称不能为空。', 'error');
            return;
        }
        AppUtils.showLoading(row);
        try {
            const updatedProject = await ProjectApiModule.updateProject(projectId, projectData);
            AppUtils.showMessage('项目更新成功！', 'success');
            // 使用 ProjectListModule 提供的函数重新渲染行
            const displayRow = ProjectListModule.createProjectRow(updatedProject);
            replaceRow(row, displayRow);
            destroyFlatpickrInstances(row);
        } catch (error) {
            console.error('Error updating project:', error);
        } finally {
            AppUtils.hideLoading(row);
        }
    }

    // 处理取消编辑
    async function handleCancelEdit(row, projectId) {
        AppUtils.showLoading(row);
        try {
            const project = await ProjectApiModule.getProjectDetail(projectId);
            if(project) {
                // 使用 ProjectListModule 提供的函数重新渲染行
                const displayRow = ProjectListModule.createProjectRow(project);
                replaceRow(row, displayRow);
                destroyFlatpickrInstances(row);
            } else {
                AppUtils.showMessage('无法获取原始项目数据。', 'warning');
                ProjectListModule.refresh(); // 刷新列表
            }
        } catch(error) {
            console.error('Error cancelling edit:', error);
            AppUtils.showMessage('取消编辑失败，请重试。', 'error');
        } finally {
            AppUtils.hideLoading(row);
        }
    }


    // 创建项目的编辑行 HTML
    function createEditableProjectRow(project) {
        const row = document.createElement('tr');
        row.classList.add('editing-row');
        row.dataset.projectId = project.projectId;
        const selectedTagIds = (project.tags || []).map(tag => tag.tagId);
        const createdAtValue = AppUtils.formatDateIfPresent(project.createdAt, 'Y-m-d');

        // 获取状态的 CSS 类
        const statusClass = ProjectListModule.getStatusClass ? ProjectListModule.getStatusClass(project.projectStatus) : '';
        const statusHtml = `<span class="status ${statusClass}">${AppUtils.escapeHTML(project.projectStatus ?? '未知')}</span>`;


        row.innerHTML = `
            <td class="text-center"><i class="icon-edit"></i></td>
            <td><input type="text" name="projectName" value="${AppUtils.escapeHTML(project.projectName ?? '')}" required class="form-input text-sm"></td>
            <td><input type="text" name="createdAt" value="${createdAtValue}" class="form-input text-sm flatpickr-input" placeholder="创建日期"></td>
            <td><select name="tagIds" multiple class="form-multiselect text-sm" placeholder="选择标签..."></select></td>
            <td><select name="businessTypeName" class="form-select text-sm"><option value="">选择业务类型</option></select></td>
            <td><select name="profitCenterZone" class="form-select text-sm"><option value="">选择利润中心</option></select></td>
            <td class="text-sm">${AppUtils.escapeHTML(project.currentStageName ?? '-')}</td>
            <td class="text-sm text-center">${statusHtml}</td>
            <td class="text-sm text-center"></td>
            <td class="text-center">
                <button class="btn btn-xs btn-save save-edit-btn">保存</button>
                <button class="btn btn-xs btn-cancel cancel-edit-btn">取消</button>
            </td>
        `;

        // 填充下拉选项并设置选中状态
        if (lookupData) {
            // 标签使用 populateSelect
            populateSelect(row.querySelector('select[name="tagIds"]'), lookupData.tags || [], 'tagId', 'tagName', true, selectedTagIds);
            // 业务类型和利润中心使用 populateSelectFromMap
            populateSelectFromMap(row.querySelector('select[name="businessTypeName"]'), lookupData.businessTypes || [], 'value', 'text', project.businessTypeName);
            populateSelectFromMap(row.querySelector('select[name="profitCenterZone"]'), lookupData.profitCenters || [], 'value', 'text', project.profitCenterZone);
            // TODO: Populate other selects if added (manager, tsBm)
        } else {
            console.warn("Lookup data not available for editable row dropdowns.");
        }
        return row;
    }

    // 从编辑行（包括新增行）获取数据
    function getEditableRowData(row) {
        const data = {};
        data.projectName = row.querySelector('input[name="projectName"]')?.value.trim();
        data.businessTypeName = row.querySelector('select[name="businessTypeName"]')?.value;
        data.profitCenterZone = row.querySelector('select[name="profitCenterZone"]')?.value;
        // data.projectManagerEmployee = row.querySelector('select[name="projectManagerEmployee"]')?.value;
        // data.tsBm = row.querySelector('select[name="tsBm"]')?.value;
        // data.projectDescription = row.querySelector('textarea[name="projectDescription"]')?.value.trim();

        const tagSelect = row.querySelector('select[name="tagIds"]');
        if (tagSelect) {
            data.tagIds = Array.from(tagSelect.selectedOptions).map(option => option.value).filter(val => val); // Filter empty values
        } else {
            data.tagIds = [];
        }

        for (const key in data) {
            if (data[key] === '') {
                data[key] = null;
            }
        }
        return data;
    }

    // 填充下拉框选项 (从 project_main.js 移入或共享)
    function populateSelect(selectElement, options, valueField, textField, isMultiple = false, selectedValue = null) {
        if (!selectElement || !Array.isArray(options)) return;
        const firstOption = selectElement.options[0];
        selectElement.innerHTML = '';
        if (firstOption && firstOption.value === '') {
            selectElement.appendChild(firstOption);
        }
        options.forEach(optionData => {
            const option = document.createElement('option');
            option.value = optionData[valueField];
            option.textContent = optionData[textField];
            if (selectedValue) {
                if (isMultiple && Array.isArray(selectedValue) && selectedValue.map(String).includes(String(option.value))) {
                    option.selected = true;
                } else if (!isMultiple && String(selectedValue) === String(option.value)) {
                    option.selected = true;
                }
            }
            selectElement.appendChild(option);
        });
    }
    // 从 project_main.js 移入或共享
    function populateSelectFromMap(selectElement, options, valueKey, textKey, selectedValue = null) {
        if (!selectElement || !Array.isArray(options)) return;
        const firstOption = selectElement.options[0];
        selectElement.innerHTML = '';
        if (firstOption && firstOption.value === '') {
            selectElement.appendChild(firstOption);
        }
        options.forEach(optionData => {
            const option = document.createElement('option');
            option.value = optionData[valueKey];
            option.textContent = optionData[textKey];
            if (selectedValue && String(selectedValue) === String(option.value)) { // Handle single select pre-selection
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
            const instanceData = flatpickrInstances.find(item => item.element === input);
            if (instanceData && instanceData.instance) {
                instanceData.instance.destroy();
                flatpickrInstances = flatpickrInstances.filter(item => item.element !== input);
            }
        });
    }


    // 暴露公共接口
    return {
        init: init
    };
})();
