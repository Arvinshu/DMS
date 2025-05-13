/**
 * 文件路径: src/main/resources/static/js/project/project_main.js
 * 开发时间: 2025-04-24 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 项目管理页面的主入口脚本，负责初始化各个子模块并协调交互。
 * 更新内容: 移除本文件中的 showLoading/hideLoading 调用，交由子模块控制。
 * 依赖: common.js, project_api.js, project_list.js, project_crud.js, project_tasks.js, flatpickr
 */
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Initializing Project Management Page...');

    // --- DOM 元素获取 ---
    const sidebarLinks = document.querySelectorAll('.sub-nav a[data-view]');
    const views = document.querySelectorAll('.view-container .view');
    const projectManagementView = document.getElementById('project-management-view'); // 仍然可以获取，即使不是默认
    const projectStatisticsView = document.getElementById('project-statistics-view'); // 获取统计视图
    const contentArea = document.querySelector('.content-area'); // 获取内容区域用于显示错误

    if (!contentArea || views.length === 0) { // 检查核心元素
        console.error('Content area or view containers not found.');
        return;
    }

    // --- 获取初始化所需数据 ---
    let lookupData = {};
    let stagesData = [];
    let employeesData = [];

    try {
        // 并行获取 Lookups 和 Stages 数据
        const [lookupsResponse, stagesResponse] = await Promise.all([
            ProjectApiModule.getProjectLookups(),
            ProjectApiModule.getAllStages()
        ]);

        lookupData = lookupsResponse || {};
        stagesData = stagesResponse || [];
        employeesData = lookupData.employees || [];

        console.log('Lookup data fetched:', lookupData);
        console.log('Stages data fetched:', stagesData);

        // --- 初始化各个模块 ---
        // 这些模块的初始化不依赖于哪个视图是默认显示的
        if (typeof ProjectListModule !== 'undefined' && typeof ProjectListModule.init === 'function') {
            ProjectListModule.init();
            populateSearchDropdowns(lookupData);
        } else {
            console.error('ProjectListModule is not defined or init function is missing.');
        }

        if (typeof ProjectCrudModule !== 'undefined' && typeof ProjectCrudModule.init === 'function') {
            ProjectCrudModule.init(lookupData);
        } else {
            console.error('ProjectCrudModule is not defined or init function is missing.');
        }

        if (typeof ProjectTasksModule !== 'undefined' && typeof ProjectTasksModule.init === 'function') {
            ProjectTasksModule.init(stagesData, employeesData);
        } else {
            console.error('ProjectTasksModule is not defined or init function is missing.');
        }

        // --- 初始化视图切换 (会根据 HTML 中的 active 类设置默认视图) ---
        setupViewSwitcher(sidebarLinks, views);

        // --- 重要：如果默认视图是统计视图，需要确保统计模块被初始化 ---
        // setupViewSwitcher 会处理视图的显示/隐藏，但我们需要手动触发统计模块的初始化逻辑
        const defaultActiveView = document.querySelector('.view-container .view.active');
        if (defaultActiveView && defaultActiveView.id === 'project-statistics-view') {
            console.log('[PROJECT_MAIN] Default view is Statistics View, initializing statistics module...');
            if (typeof initializeOrRefreshStatisticsModule === 'function') {
                // 调用统计模块的初始化函数
                await initializeOrRefreshStatisticsModule();
            } else {
                console.error('[PROJECT_MAIN] initializeOrRefreshStatisticsModule function is not defined.');
            }
        }


    } catch (error) {
        console.error('Error during project page initialization:', error);
        contentArea.innerHTML = `<div class="error-message p-4 bg-red-100 border border-red-400 text-red-700 rounded">页面初始化失败: ${error.message || '请刷新页面重试'}</div>`;
    }

    console.log('Project Management Page initialized.');
});

// ... (populateSearchDropdowns, populateSelect, populateSelectFromMap 函数保持不变) ...

/**
 * 填充搜索栏的下拉框选项
 * @param {object} lookups - 从 API 获取的查找数据
 */
function populateSearchDropdowns(lookups) {
    console.log('Populating search dropdowns...');
    const tagsSelect = document.getElementById('search-project-tags');
    if (tagsSelect && lookups.tags) {
        populateSelect(tagsSelect, lookups.tags, 'tagId', 'tagName', true);
    }
    const businessTypeSelect = document.getElementById('search-business-type');
    if (businessTypeSelect && lookups.businessTypes) {
        populateSelectFromMap(businessTypeSelect, lookups.businessTypes, 'value', 'text');
    }
    const profitCenterSelect = document.getElementById('search-profit-center');
    if (profitCenterSelect && lookups.profitCenters) {
        populateSelectFromMap(profitCenterSelect, lookups.profitCenters, 'value', 'text');
    }
    const stageSelect = document.getElementById('search-project-stage');
    if (stageSelect && lookups.stages) {
        populateSelect(stageSelect, lookups.stages, 'stageId', 'stageName');
    }
    console.log('Search dropdowns populated.');
}

/**
 * 通用函数：使用对象数组填充下拉框选项
 * @param {HTMLSelectElement} selectElement
 * @param {Array<object>} options
 * @param {string} valueField
 * @param {string} textField
 * @param {boolean} [isMultiple=false]
 * @param {string|Array<string>} [selectedValue=null]
 */
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
/**
 * 通用函数：使用 Map 列表填充下拉框选项 (value/text)
 * @param {HTMLSelectElement} selectElement
 * @param {Array<Map<String, Object>>} options
 * @param {string} valueKey
 * @param {string} textKey
 */
function populateSelectFromMap(selectElement, options, valueKey, textKey) {
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
        selectElement.appendChild(option);
    });
}


/**
 * 设置侧边栏视图切换逻辑 (适配内嵌侧边栏)
 * @param {NodeListOf<Element>} links
 * @param {NodeListOf<Element>} viewElements
 */
function setupViewSwitcher(links, viewElements) {
    if (!links || links.length === 0 || !viewElements || viewElements.length === 0) {
        console.warn('View switcher setup skipped: links or views not found or empty.');
        if(viewElements && viewElements.length > 0) {
            viewElements.forEach((view, index) => {
                view.style.display = index === 0 ? 'block' : 'none';
                if(index === 0) view.classList.add('active');
                else view.classList.remove('active');
            });
        }
        return;
    }
    console.log(`Setting up view switcher for ${links.length} links.`);

    links.forEach(link => {
        link.addEventListener('click', async (event) => { // **修改点：添加 async**
            event.preventDefault();
            const targetViewId = link.getAttribute('href')?.substring(1);
            if (!targetViewId) return;

            // 检查是否是切换到统计视图
            const isSwitchingToStats = targetViewId === 'project-statistics-view';
            const currentActiveView = document.querySelector('.view-container .view.active');
            const isAlreadyStatsView = currentActiveView && currentActiveView.id === 'project-statistics-view';

            let viewFound = false;
            viewElements.forEach(view => {
                if (view.id === targetViewId) {
                    view.style.display = 'block';
                    view.classList.add('active');
                    viewFound = true;
                } else {
                    view.style.display = 'none';
                    view.classList.remove('active');
                }
            });
            if(viewFound) {
                links.forEach(l => l.classList.remove('active'));
                link.classList.add('active');
                console.log(`Switched to view: ${targetViewId}`);

                // --- 重要：如果切换到统计视图，并且之前不是统计视图，则初始化/刷新统计模块 ---
                if (isSwitchingToStats && !isAlreadyStatsView) {
                    console.log('[PROJECT_MAIN] Switched to Statistics View, initializing/refreshing statistics module...');
                    if (typeof initializeOrRefreshStatisticsModule === 'function') {
                        // 调用统计模块的初始化/刷新函数
                        await initializeOrRefreshStatisticsModule(); // **修改点：使用 await**
                    } else {
                        console.error('[PROJECT_MAIN] initializeOrRefreshStatisticsModule function is not defined.');
                    }
                }

            } else {
                console.warn(`Target view with ID "${targetViewId}" not found.`);
            }
        });
    });

    // --- 设置初始视图 ---
    // 这部分逻辑现在会读取 HTML 中设置的 active 类来确定默认视图
    const defaultViewLink = document.querySelector('.sub-nav a[data-view].active') || links[0];
    if (defaultViewLink) {
        const defaultViewId = defaultViewLink.getAttribute('href')?.substring(1);
        if(defaultViewId){
            let defaultViewFound = false;
            viewElements.forEach(view => {
                if (view.id === defaultViewId) {
                    view.style.display = 'block';
                    view.classList.add('active');
                    defaultViewFound = true;
                } else {
                    view.style.display = 'none';
                    view.classList.remove('active');
                }
            });
            if(defaultViewFound){
                links.forEach(l => l.classList.remove('active'));
                defaultViewLink.classList.add('active');
                console.log(`Default view set to: ${defaultViewId}`);
            } else {
                // Fallback if active class in HTML points to non-existent view
                console.warn(`Default view element with ID "${defaultViewId}" (from active link) not found. Displaying first view.`);
                viewElements.forEach((view, index) => {
                    view.style.display = index === 0 ? 'block' : 'none';
                    if(index === 0) view.classList.add('active');
                    else view.classList.remove('active');
                });
                links.forEach((l, index) => l.classList.toggle('active', index === 0));
            }
        } else {
            // Fallback if active link has no href
            console.warn("Default active link has no valid href. Displaying first view.");
            viewElements.forEach((view, index) => {
                view.style.display = index === 0 ? 'block' : 'none';
                if(index === 0) view.classList.add('active');
                else view.classList.remove('active');
            });
            links.forEach((l, index) => l.classList.toggle('active', index === 0));
        }
    } else {
        // Fallback if no links found or no active link
        console.warn("Could not determine default view link. Displaying first view.");
        viewElements.forEach((view, index) => {
            view.style.display = index === 0 ? 'block' : 'none';
            if(index === 0) view.classList.add('active');
            else view.classList.remove('active');
        });
        if(links.length > 0) {
            links.forEach((l, index) => l.classList.toggle('active', index === 0));
        }
    }
}
