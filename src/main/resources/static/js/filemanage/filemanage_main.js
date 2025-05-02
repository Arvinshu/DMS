/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_main.js
 * 文件名: filemanage_main.js
 * 开发时间: 2025-04-30 13:05:00 EDT (Final Version after debugging)
 * 作者: Gemini
 * 用途: 文件管理页面的主入口 JavaScript 文件，负责初始化、事件绑定协调。
 */

document.addEventListener('DOMContentLoaded', () => {

    console.log('File Manage Main JS Loaded');

    // --- Get DOM Elements (Check if they exist for basic layout) ---
    const sidebar = document.getElementById('filemanage-sidebar');
    const content = document.getElementById('filemanage-content');
    const querySection = document.getElementById('query-download-section');
    const syncSection = document.getElementById('sync-management-section');

    // Basic check for essential layout elements
    if (!sidebar || !content || !querySection || !syncSection) {
        console.error("Core layout elements not found. Aborting filemanage initialization.");
        // Optionally display a user-friendly error message on the page
        // document.body.innerHTML = '<p style="color: red; padding: 20px;">页面加载失败：缺少必要的页面结构。</p>';
        return; // Stop further execution
    }

    // --- Initialization ---
    try {
        // Initialize UI interactions (sidebar switching etc.)
        fileManageUI.initSidebar();

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

    } catch (error) {
        console.error("Error during file manage module initialization:", error);
        alert("页面初始化失败，部分功能可能无法使用。请检查控制台获取详细信息。");
    }


    // --- Cleanup on page unload ---
    window.addEventListener('beforeunload', () => {
        // Stop polling if sync module exposed a stop function
        if (typeof fileManageSync !== 'undefined' && typeof fileManageSync.stopSyncStatusPolling === 'function') {
            fileManageSync.stopSyncStatusPolling();
        } else {
            console.warn("Could not call stopSyncStatusPolling on fileManageSync module during unload.");
            // Avoid trying to clear interval directly as the ID is managed within the module
        }
    });

}); // End DOMContentLoaded
