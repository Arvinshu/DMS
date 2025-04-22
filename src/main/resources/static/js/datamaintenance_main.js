/**
 * 数据维护主页面控制脚本
 * 文件路径: src/main/resources/static/js/datamaintenance_main.js
 * 职责：处理侧边栏菜单点击，切换显示不同的数据维护模块，并触发各模块的初始化加载。
 * 依赖: 各模块 JS 文件中暴露的初始化函数 (例如 DepartmentModule.init)
 */
document.addEventListener('DOMContentLoaded', function() {
    'use strict';

    const sidebarMenuItems = document.querySelectorAll('.data-menu-item'); // 获取所有数据维护菜单项
    const dataSections = document.querySelectorAll('.data-section'); // 获取所有数据模块的 section
    const sidebar = document.querySelector('aside'); // 获取侧边栏元素，用于更新 activeMenu (如果需要的话)

    // 存储模块初始化函数，假设各模块JS会将其暴露出来
    const moduleInitializers = {
        'departments': window.DepartmentModule ? window.DepartmentModule.init : null,
        'employees': window.EmployeeModule ? window.EmployeeModule.init : null,
        'timesheet-codes': window.TimesheetCodeModule ? window.TimesheetCodeModule.init : null,
        'business-types': window.BusinessTypeModule ? window.BusinessTypeModule.init : null,
        'profit-centers': window.ProfitCenterModule ? window.ProfitCenterModule.init : null,
        'sync-data': window.SyncDataModule ? window.SyncDataModule.init : null, // 新增同步模块
    };

    // 存储模块是否已初始化
    const moduleInitialized = {};

    /**
     * 显示指定的模块，隐藏其他模块
     * @param {string} targetId - 要显示的模块 section 的 ID (不含 #)
     */
    function showSection(targetId) {
        console.log(`[Main] Attempting to show section: ${targetId}`); // 调试日志
        let foundTarget = false;
        dataSections.forEach(section => {
            if (section.id === targetId) {
                section.classList.remove('hidden');
                foundTarget = true;
                console.log(`[Main] Section shown: ${targetId}`);
                // 触发模块初始化（如果尚未初始化）
                const moduleKey = targetId.replace('-section', '');
                if (moduleInitializers[moduleKey] && !moduleInitialized[moduleKey]) {
                    console.log(`[Main] Initializing module: ${moduleKey}`);
                    try {
                        moduleInitializers[moduleKey](); // 调用初始化函数
                        moduleInitialized[moduleKey] = true; // 标记为已初始化
                    } catch (error) {
                        console.error(`[Main] Error initializing module ${moduleKey}:`, error);
                    }
                } else if (!moduleInitializers[moduleKey]) {
                    console.warn(`[Main] Initializer function not found for module: ${moduleKey}`);
                } else {
                    console.log(`[Main] Module ${moduleKey} already initialized.`);
                    // 可选：如果模块需要每次显示时都刷新，可以在这里调用一个刷新函数
                    // if (window[moduleKey + 'Module'] && typeof window[moduleKey + 'Module'].refresh === 'function') {
                    //     window[moduleKey + 'Module'].refresh();
                    // }
                }
            } else {
                section.classList.add('hidden');
            }
        });
        if (!foundTarget) {
            console.error(`[Main] Target section with id "${targetId}" not found.`);
        }
    }

    /**
     * 更新侧边栏菜单项的激活状态
     * @param {Element} clickedItem - 被点击的菜单项元素
     */
    function updateSidebarActiveState(clickedItem) {
        sidebarMenuItems.forEach(item => {
            item.classList.remove('bg-gray-900', 'text-white');
        });
        if (clickedItem) {
            clickedItem.classList.add('bg-gray-900', 'text-white');
            console.log("[Main] Sidebar active state updated for:", clickedItem.dataset.target);
        }
    }

    // 为侧边栏菜单项添加点击事件监听器
    sidebarMenuItems.forEach(item => {
        item.addEventListener('click', function(event) {
            event.preventDefault();
            const targetModule = this.dataset.target;
            console.log(`[Main] Menu item clicked: ${targetModule}`);
            if (targetModule) {
                const targetSectionId = `${targetModule}-section`;
                showSection(targetSectionId);
                updateSidebarActiveState(this);
            } else {
                console.warn('[Main] Clicked menu item does not have a data-target attribute:', this);
            }
        });
    });

    // --- 初始化 ---
    // 查找 URL hash 或者默认显示第一个有效模块
    let initialTarget = 'departments'; // 默认目标
    if (window.location.hash) {
        const hashTarget = window.location.hash.substring(1); // 去掉 #
        if (moduleInitializers[hashTarget]) { // 检查 hash 是否对应一个有效模块
            initialTarget = hashTarget;
        }
    }
    console.log(`[Main] Initial target module: ${initialTarget}`);

    const defaultActiveItem = document.querySelector(`.data-menu-item[data-target="${initialTarget}"]`);
    if (defaultActiveItem) {
        showSection(`${initialTarget}-section`);
        updateSidebarActiveState(defaultActiveItem);
    } else {
        // 如果默认的 'departments' 或 hash 目标找不到，尝试显示第一个可用的 section
        console.warn(`[Main] Default active menu item '[data-target="${initialTarget}"]' not found. Trying first available section.`);
        if (dataSections.length > 0) {
            const firstSectionId = dataSections[0].id;
            const firstModuleKey = firstSectionId.replace('-section', '');
            const firstMenuItem = document.querySelector(`.data-menu-item[data-target="${firstModuleKey}"]`);
            showSection(firstSectionId);
            updateSidebarActiveState(firstMenuItem);
        } else {
            console.error("[Main] No data sections found on the page.");
        }
    }
    console.log("[Main] Data Maintenance Main Script Initialized.");

});

// * **说明:**
//     * 监听 `DOMContentLoaded` 事件。
//     * 获取所有侧边栏菜单项 (`.data-menu-item`) 和所有数据模块区域 (`.data-section`)。
//     * `moduleInitializers`: 存储对每个模块 JS 文件中暴露的 `init` 函数的引用（假设每个模块 JS 文件会将其 `init` 函数挂载到 `window` 下的特定对象上，如 `window.DepartmentModule.init`）。
//     * `showSection`: 根据传入的 ID 显示对应的模块区域，隐藏其他的，并负责调用对应模块的初始化函数（仅首次显示时调用）。
//     * `updateSidebarActiveState`: 更新侧边栏菜单项的样式，高亮当前选中的项。
//     * 事件监听：为每个侧边栏菜单项添加点击事件，点击时阻止默认行为，获取 `data-target` 值，调用 `showSection` 和 `updateSidebarActiveState`。
//     * 初始化：页面加载时，默认查找并显示“部门管理”模块，并高亮对应的侧边

//
// * **修改说明:**
// * 在 `moduleInitializers` 对象中添加了 `'sync-data': window.SyncDataModule ? window.SyncDataModule.init : null`。
// * 添加了更多 `console.log` / `console.debug` 帮助追踪流程。
// * 初始化逻辑现在会尝试读取 URL hash (`#`) 来决定初始显示的模块，如果 hash 无效或不存在，则默认显示 `department