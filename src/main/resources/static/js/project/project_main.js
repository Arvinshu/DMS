/**
 * 文件路径: src/main/resources/static/js/project/project_main.js
 * 开发时间: 2025-04-24 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 项目管理页面的主入口脚本，负责初始化各个子模块并协调交互。
 * 更新内容: 移除本文件中的 showLoading/hideLoading 调用，交由子模块控制。
 * 集成 Choices.js 用于标签多选下拉框。
 * 新增“清空筛选”功能。
 * 修改“清空筛选”后的刷新逻辑为模拟点击搜索按钮。
 * 移除 Choices.js 下拉选项中的 "Press to select" 文本。
 * 依赖: common.js, project_api.js, project_list.js, project_crud.js, project_tasks.js, flatpickr, choices.js
 */
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Initializing Project Management Page...');

    // --- DOM 元素获取 ---
    const sidebarLinks = document.querySelectorAll('.sub-nav a[data-view]');
    const views = document.querySelectorAll('.view-container .view');
    const projectManagementView = document.getElementById('project-management-view');
    const projectStatisticsView = document.getElementById('project-statistics-view');
    const contentArea = document.querySelector('.content-area');

    const clearSearchBtn = document.getElementById('clear-search-btn');
    const searchBtn = document.getElementById('search-project-btn');


    const choicesInstances = new Map();

    if (!contentArea || views.length === 0) {
        console.error('Content area or view containers not found.');
        return;
    }

    // --- 获取初始化所需数据 ---
    let lookupData = {};
    let stagesData = [];
    let employeesData = [];

    try {
        const [lookupsResponse, stagesResponse] = await Promise.all([
            ProjectApiModule.getProjectLookups(),
            ProjectApiModule.getAllStages()
        ]);

        lookupData = lookupsResponse || {};
        stagesData = stagesResponse || [];
        employeesData = lookupData.employees || [];

        console.log('Lookup data fetched:', lookupData);
        console.log('Stages data fetched:', stagesData);

        if (typeof ProjectListModule !== 'undefined' && typeof ProjectListModule.init === 'function') {
            ProjectListModule.init();
            populateSearchDropdowns(lookupData);
        } else {
            console.error('ProjectListModule is not defined or init function is missing.');
        }

        if (typeof ProjectCrudModule !== 'undefined' && typeof ProjectCrudModule.init === 'function') {
            ProjectCrudModule.init(lookupData, choicesInstances);
        } else {
            console.error('ProjectCrudModule is not defined or init function is missing.');
        }

        if (typeof ProjectTasksModule !== 'undefined' && typeof ProjectTasksModule.init === 'function') {
            ProjectTasksModule.init(stagesData, employeesData);
        } else {
            console.error('ProjectTasksModule is not defined or init function is missing.');
        }

        setupViewSwitcher(sidebarLinks, views);

        const defaultActiveView = document.querySelector('.view-container .view.active');
        if (defaultActiveView && defaultActiveView.id === 'project-statistics-view') {
            console.log('[PROJECT_MAIN] Default view is Statistics View, initializing statistics module...');
            if (typeof initializeOrRefreshStatisticsModule === 'function') {
                await initializeOrRefreshStatisticsModule();
            } else {
                console.error('[PROJECT_MAIN] initializeOrRefreshStatisticsModule function is not defined.');
            }
        }

        if (clearSearchBtn) {
            clearSearchBtn.addEventListener('click', handleClearSearchFilters);
        }


    } catch (error) {
        console.error('Error during project page initialization:', error);
        contentArea.innerHTML = `<div class="error-message p-4 bg-red-100 border border-red-400 text-red-700 rounded">页面初始化失败: ${error.message || '请刷新页面重试'}</div>`;
    }

    console.log('Project Management Page initialized.');

    /**
     * 销毁并重新初始化指定 select 元素的 Choices.js 实例
     * @param {HTMLSelectElement} selectElement
     * @param {Array<object>} optionsData - 新的选项数据
     * @param {string} valueField
     * @param {string} textField
     * @param {boolean} isMultiple
     * @param {object} choicesConfig - Choices.js 的配置对象
     */
    function reinitializeChoices(selectElement, optionsData, valueField, textField, isMultiple, choicesConfig) {
        if (choicesInstances.has(selectElement)) {
            choicesInstances.get(selectElement).destroy();
            choicesInstances.delete(selectElement);
        }
        populateSelect(selectElement, optionsData, valueField, textField, isMultiple);
        const newInstance = new Choices(selectElement, choicesConfig);
        choicesInstances.set(selectElement, newInstance);
    }


    /**
     * 填充搜索栏的下拉框选项
     * @param {object} lookups - 从 API 获取的查找数据
     */
    function populateSearchDropdowns(lookups) {
        console.log('Populating search dropdowns...');
        const tagsSelect = document.getElementById('search-project-tags');
        if (tagsSelect && lookups.tags) {
            populateSelect(tagsSelect, lookups.tags, 'tagId', 'tagName', true);
            if (choicesInstances.has(tagsSelect)) {
                choicesInstances.get(tagsSelect).destroy();
            }
            const choicesInstance = new Choices(tagsSelect, {
                removeItemButton: true,
                placeholder: true,
                placeholderValue: '选择或搜索标签...',
                allowHTML: false,
                searchResultLimit: 10,
                itemSelectText: '', // 新增：移除 "Press to select"
            });
            choicesInstances.set(tagsSelect, choicesInstance);
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

    // 新增：处理清空筛选按钮点击事件
    function handleClearSearchFilters() {
        console.log('Clearing search filters...');
        const nameInput = document.getElementById('search-project-name');
        if (nameInput) nameInput.value = '';

        ['search-business-type', 'search-profit-center', 'search-project-stage', 'search-project-status'].forEach(selectId => {
            const selectElement = document.getElementById(selectId);
            if (selectElement) selectElement.selectedIndex = 0;
        });

        const tagsSelect = document.getElementById('search-project-tags');
        if (tagsSelect && choicesInstances.has(tagsSelect)) {
            const choicesInstance = choicesInstances.get(tagsSelect);
            choicesInstance.clearInput();
            choicesInstance.removeActiveItems();
            // 注意：如果选项是动态加载的，或者 clearStore 后 placeholder 不正确，
            // 可能需要重新 populateSelect 并重新初始化 Choices 实例。
            // 目前假设 populateSelect 填充的是静态的完整列表。
        }

        if (searchBtn) {
            console.log('Simulating search button click to refresh list with cleared filters.');
            searchBtn.click();
        } else {
            console.error('Search button not found. Cannot refresh list after clearing filters.');
            if (typeof ProjectListModule !== 'undefined' && typeof ProjectListModule.refresh === 'function') {
                if (ProjectListModule.setCurrentFilters) {
                    ProjectListModule.setCurrentFilters({});
                }
                ProjectListModule.refresh();
            }
        }
        AppUtils.showMessage('筛选条件已清空。', 'info');
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
        const firstOptionHTML = selectElement.options[0] && selectElement.options[0].value === '' ? selectElement.options[0].outerHTML : '';
        selectElement.innerHTML = firstOptionHTML;

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
     * @param {string|null} [selectedValue=null]
     */
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
            link.addEventListener('click', async (event) => {
                event.preventDefault();
                const targetViewId = link.getAttribute('href')?.substring(1);
                if (!targetViewId) return;

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

                    if (isSwitchingToStats && !isAlreadyStatsView) {
                        console.log('[PROJECT_MAIN] Switched to Statistics View, initializing/refreshing statistics module...');
                        if (typeof initializeOrRefreshStatisticsModule === 'function') {
                            await initializeOrRefreshStatisticsModule();
                        } else {
                            console.error('[PROJECT_MAIN] initializeOrRefreshStatisticsModule function is not defined.');
                        }
                    }

                } else {
                    console.warn(`Target view with ID "${targetViewId}" not found.`);
                }
            });
        });

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
                    console.warn(`Default view element with ID "${defaultViewId}" (from active link) not found. Displaying first view.`);
                    viewElements.forEach((view, index) => {
                        view.style.display = index === 0 ? 'block' : 'none';
                        if(index === 0) view.classList.add('active');
                        else view.classList.remove('active');
                    });
                    links.forEach((l, index) => l.classList.toggle('active', index === 0));
                }
            } else {
                console.warn("Default active link has no valid href. Displaying first view.");
                viewElements.forEach((view, index) => {
                    view.style.display = index === 0 ? 'block' : 'none';
                    if(index === 0) view.classList.add('active');
                    else view.classList.remove('active');
                });
                links.forEach((l, index) => l.classList.toggle('active', index === 0));
            }
        } else {
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
});
