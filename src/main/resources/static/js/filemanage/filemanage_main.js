/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_main.js
 * 文件名: filemanage_main.js
 * 开发时间: 2025-06-04 16:00:00 (Asia/Shanghai)
 * 作者: Gemini
 * 用途: 文件管理页面的主入口 JavaScript 文件，负责初始化、事件绑定协调。
 * 本次更新: 添加对 fileManageFulltextEvents 模块的初始化调用。
 */

document.addEventListener('DOMContentLoaded', () => {

    console.log('File Manage Main JS Loaded');

    // --- Get DOM Elements (Check if they exist for basic layout) ---
    const sidebar = document.getElementById('filemanage-sidebar');
    const content = document.getElementById('filemanage-content');
    const querySection = document.getElementById('query-download-section');
    const syncSection = document.getElementById('sync-management-section');
    const fulltextSearchSection = document.getElementById('fulltext-search-section'); // 新增对全文搜索区域的检查

    // Basic check for essential layout elements
    if (!sidebar || !content || !querySection || !syncSection || !fulltextSearchSection) { // 确保所有区域都存在
        console.error("一个或多个核心布局元素未找到。中止 filemanage 初始化。");
        // Optionally display a user-friendly error message on the page
        // document.body.innerHTML = '<p style="color: red; padding: 20px;">页面加载失败：缺少必要的页面结构。</p>';
        return; // Stop further execution
    }

    // --- Initialization ---
    try {
        // Initialize UI interactions (sidebar switching etc.)
        // fileManageUI.initSidebar() 会根据HTML中哪个链接有 'active' 类来决定初始显示哪个区域
        if (typeof fileManageUI !== 'undefined' && typeof fileManageUI.initSidebar === 'function') {
            fileManageUI.initSidebar();
        } else {
            console.error("fileManageUI module or its initSidebar function not found!");
        }


        // Initialize Query/Download module (handles its own elements and listeners)
        if (typeof fileManageQuery !== 'undefined' && typeof fileManageQuery.init === 'function') {
            fileManageQuery.init();
        } else {
            console.error("fileManageQuery module or its init function not found!");
        }

        // Initialize Sync Management module (handles its own elements, listeners, and polling)
        if (typeof fileManageSync !== 'undefined' && typeof fileManageSync.init === 'function') {
            fileManageSync.init();
        } else {
            console.error("fileManageSync module or its init function not found!");
        }

        // 新增：Initialize Fulltext Search module
        if (typeof fileManageFulltextEvents !== 'undefined' && typeof fileManageFulltextEvents.init === 'function') {
            fileManageFulltextEvents.init();
        } else {
            console.error("fileManageFulltextEvents module or its init function not found!");
        }

    } catch (error) {
        console.error("文件管理模块初始化过程中发生错误:", error);
        alert("页面初始化失败，部分功能可能无法使用。请检查控制台获取详细信息。");
    }


    // --- Cleanup on page unload ---
    window.addEventListener('beforeunload', () => {
        // Stop polling if sync module exposed a stop function
        if (typeof fileManageSync !== 'undefined' && typeof fileManageSync.stopSyncStatusPolling === 'function') {
            fileManageSync.stopSyncStatusPolling();
        } else {
            console.warn("在页面卸载时无法调用 fileManageSync 模块的 stopSyncStatusPolling 方法。");
        }
    });

}); // End DOMContentLoaded
