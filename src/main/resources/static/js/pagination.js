/**
 * 分页组件逻辑 (添加最终状态日志)
 * 文件路径: src/main/resources/static/js/pagination.js
 * 修正：每次调用时重新生成内部结构，使用 data-pagination-role 定位元素，添加最终 innerHTML 日志。
 * 依赖: common.js
 */

(function(window) {
    'use strict';

    // --- 创建或获取 AppUtils 命名空间 ---
    const AppUtils = window.AppUtils || {};
    window.AppUtils = AppUtils; // 确保 AppUtils 在 window 上

    // --- 移除全局状态变量 ---
    // let paginationElements = {}; // 不再需要全局缓存元素
    // let pageChangeCallback = null; // 回调在每次调用时传入
    // let listenersAttached = false; // 监听器在每次生成时绑定

    /**
     * 创建单个页码按钮元素
     * @param {number} pageNumber 页码
     * @param {number} currentPage 当前页
     * @param {Function} onClickCallback 点击回调
     * @returns {HTMLButtonElement}
     */
    function createPageButton(pageNumber, currentPage, onClickCallback) {
        const button = document.createElement('button');
        button.textContent = pageNumber;
        button.type = 'button';
        // 基础样式 - 确保这些 Tailwind 类存在或在 CSS 中定义
        button.classList.add('relative', 'inline-flex', 'items-center', 'px-4', 'py-2', 'border', 'text-sm', 'font-medium');
        if (pageNumber === currentPage) {
            // 当前页样式
            button.classList.add('z-10', 'bg-indigo-50', 'border-indigo-500', 'text-indigo-600');
            button.setAttribute('aria-current', 'page');
            button.disabled = true;
        } else {
            // 其他页样式
            button.classList.add('bg-white', 'border-gray-300', 'text-gray-500', 'hover:bg-gray-50');
            if (typeof onClickCallback === 'function') {
                // 为动态创建的页码按钮绑定事件
                button.addEventListener('click', () => onClickCallback(pageNumber));
            }
        }
        return button;
    }

    /**
     * 创建省略号元素
     * @returns {HTMLSpanElement}
     */
    function createEllipsis() {
        const span = document.createElement('span');
        span.textContent = '...';
        // 基础样式
        span.classList.add('relative', 'inline-flex', 'items-center', 'px-4', 'py-2', 'border', 'border-gray-300', 'bg-white', 'text-sm', 'font-medium', 'text-gray-700');
        return span;
    }

    /**
     * 初始化并渲染分页组件
     * @param {object} options - 配置对象
     * @param {string} options.containerId - 分页最外层容器元素的 ID (例如 'dept-pagination-container')
     * @param {number} options.currentPage
     * @param {number} options.totalPages
     * @param {number} options.totalRecords
     * @param {number} [options.maxPagesToShow=5] - 最多显示的页码按钮数（奇数）
     * @param {Function} options.onPageChange - 页码改变时的回调函数
     */
    function setupPagination(options) {
        const {
            containerId, // 必须由调用者传入
            currentPage = 1,
            totalPages = 1,
            totalRecords = 0,
            maxPagesToShow = 5,
            onPageChange // 回调函数
        } = options;

        // 1. 检查 containerId 是否有效
        if (!containerId) {
            console.error("[Pagination] 错误：调用 setupPagination 时必须提供 containerId。");
            return;
        }

        // 2. 获取当前调用的分页容器
        const container = document.getElementById(containerId);
        if (!container) {
            console.error(`[Pagination] 错误：未找到 ID 为 "${containerId}" 的分页容器。`);
            return;
        }
        console.debug(`[Pagination] Setting up pagination for #${containerId}`);

        // 3. 动态生成分页结构
        //    每次调用都重新生成内部结构，避免状态混乱
        container.innerHTML = `
            <div class="flex-1 flex justify-between sm:hidden">
                <button data-pagination-role="prev-mobile" type="button" class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
                    上一页
                </button>
                <button data-pagination-role="next-mobile" type="button" class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
                    下一页
                </button>
            </div>
            <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                    <p class="text-sm text-gray-700">
                        共 <span data-pagination-role="total-records" class="font-medium">0</span> 条记录，
                        第 <span data-pagination-role="current-page" class="font-medium">0</span> / <span data-pagination-role="total-pages" class="font-medium">0</span> 页
                    </p>
                </div>
                <div>
                    <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination" data-pagination-role="links-container">
                        <button data-pagination-role="prev" type="button" class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
                            <span class="sr-only">上一页</span>
                            <svg class="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" /></svg>
                        </button>
                        <button data-pagination-role="next" type="button" class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
                            <span class="sr-only">下一页</span>
                            <svg class="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" /></svg>
                        </button>
                    </nav>
                </div>
            </div>
        `;

        // 4. 在新生成的结构中查找元素
        const linksContainer = container.querySelector('[data-pagination-role="links-container"]');
        const prevButton = container.querySelector('[data-pagination-role="prev"]');
        const nextButton = container.querySelector('[data-pagination-role="next"]');
        const prevMobileButton = container.querySelector('[data-pagination-role="prev-mobile"]');
        const nextMobileButton = container.querySelector('[data-pagination-role="next-mobile"]');
        const totalRecordsSpan = container.querySelector('[data-pagination-role="total-records"]');
        const currentPageSpan = container.querySelector('[data-pagination-role="current-page"]');
        const totalPagesSpan = container.querySelector('[data-pagination-role="total-pages"]');

        // 5. 检查元素是否都找到
        if (!linksContainer || !prevButton || !nextButton || !prevMobileButton || !nextMobileButton || !totalRecordsSpan || !currentPageSpan || !totalPagesSpan) {
            console.error(`分页组件错误：在 #${containerId} 内部未能找到所有必需的 data-pagination-role 元素。`);
            container.innerHTML = '<div class="text-red-500 text-center text-sm">分页控件结构错误</div>';
            return;
        }

        // 6. 更新信息显示
        totalRecordsSpan.textContent = totalRecords;
        currentPageSpan.textContent = totalPages > 0 ? currentPage : 0;
        totalPagesSpan.textContent = totalPages;

        // 7. 处理总页数 <= 1 的情况
        if (totalPages <= 1) {
            prevButton.disabled = true; nextButton.disabled = true;
            prevMobileButton.disabled = true; nextMobileButton.disabled = true;
            // 隐藏桌面端的页码导航区，但保留信息区
            const desktopNavContainer = linksContainer.closest('div'); // 找到包含 nav 和 p 的父 div
            if(desktopNavContainer) {
                // 移除 justify-between，改为居中显示信息
                const parentDiv = desktopNavContainer.parentElement;
                if(parentDiv) parentDiv.classList.add('justify-center');
                desktopNavContainer.classList.add('justify-center'); // 信息居中
            }
            linksContainer.style.display = 'none'; // 隐藏页码按钮本身
            console.debug(`[Pagination] Total pages <= 1, hiding links for #${containerId}`);
            // 打印最终 HTML 状态
            // console.log(`[Pagination] Finished setup for #${containerId}. Final innerHTML of container:`, container.innerHTML);
            return; // 不再生成页码
        } else {
            // 确保页码导航显示，并移除可能添加的居中样式
            linksContainer.style.display = 'inline-flex';
            const desktopNavContainer = linksContainer.closest('div');
            if(desktopNavContainer) {
                const parentDiv = desktopNavContainer.parentElement;
                if(parentDiv) parentDiv.classList.remove('justify-center');
                desktopNavContainer.classList.remove('justify-center');
            }
        }

        // 8. 生成页码按钮
        // --- 清空 linksContainer 内部除了 prev/next 之外的内容 ---
        const pageButtons = linksContainer.querySelectorAll('button:not([data-pagination-role="prev"]):not([data-pagination-role="next"]), span');
        pageButtons.forEach(btn => {
            try { linksContainer.removeChild(btn); } catch(e) {/* 忽略错误 */}
        });


        let startPage, endPage;
        const halfMax = Math.floor(maxPagesToShow / 2);
        // ... (计算 startPage, endPage 的逻辑不变) ...
        if (totalPages <= maxPagesToShow) { startPage = 1; endPage = totalPages; }
        else if (currentPage <= halfMax + 1) { startPage = 1; endPage = maxPagesToShow; } // 调整边界条件
        else if (currentPage + halfMax >= totalPages) { startPage = totalPages - maxPagesToShow + 1; endPage = totalPages; }
        else { startPage = currentPage - halfMax; endPage = currentPage + halfMax; }


        const fragment = document.createDocumentFragment(); // 使用文档片段提高性能

        // 添加 "首页" 和 "..."
        if (startPage > 1) {
            fragment.appendChild(createPageButton(1, currentPage, onPageChange));
            if (startPage > 2) { fragment.appendChild(createEllipsis()); }
        }
        // 创建中间页码
        for (let i = startPage; i <= endPage; i++) {
            fragment.appendChild(createPageButton(i, currentPage, onPageChange));
        }
        // 添加 "..." 和 "末页"
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) { fragment.appendChild(createEllipsis()); }
            fragment.appendChild(createPageButton(totalPages, currentPage, onPageChange));
        }
        // 将生成的页码按钮插入到 prev 和 next 按钮之间
        linksContainer.insertBefore(fragment, nextButton);


        // 9. 更新按钮状态和绑定事件
        prevButton.disabled = currentPage === 1;
        nextButton.disabled = currentPage === totalPages;
        prevMobileButton.disabled = currentPage === 1;
        nextMobileButton.disabled = currentPage === totalPages;

        // --- 事件监听 ---
        // 由于按钮是重新生成的，需要重新绑定事件
        // (或者使用事件委托到 container 上)
        // 这里使用简单的 onclick 赋值
        if (typeof onPageChange === 'function') {
            prevButton.onclick = () => { if (currentPage > 1) onPageChange(currentPage - 1); };
            nextButton.onclick = () => { if (currentPage < totalPages) onPageChange(currentPage + 1); };
            prevMobileButton.onclick = () => { if (currentPage > 1) onPageChange(currentPage - 1); };
            nextMobileButton.onclick = () => { if (currentPage < totalPages) onPageChange(currentPage + 1); };
        } else {
            // 如果没有回调，清空 onclick
            prevButton.onclick = null;
            nextButton.onclick = null;
            prevMobileButton.onclick = null;
            nextMobileButton.onclick = null;
        }

        // --- 最终状态日志 ---
        console.log(`[Pagination] Finished setup for #${containerId}. Final innerHTML of container:`, container.innerHTML);
        // ------------------------------------
    }

    // 将 setupPagination 函数暴露出去
    AppUtils.setupPagination = setupPagination;
    console.log("Pagination script loaded and setupPagination attached to AppUtils.");

})(window);

//
// **修改说明:**
//
// 1.  **移除全局状态:** 删除了可能引起冲突的全局（模块作用域）变量 `paginationElements`, `listenersAttached`。
// 2.  **每次重新生成 HTML:** `setupPagination` 函数现在通过设置 `container.innerHTML` 来完全重新创建分页控件的内部结构。这保证了每次调用都是一个干净的状态。
// 3.  **使用 `data-pagination-role`:** HTML 结构中的关键元素（按钮、容器、span）使用 `data-pagination-role` 属性进行标识，JS 通过 `container.querySelector('[data-pagination-role="..."]')` 来查找它们，避免了 ID 冲突的可能性。
// 4.  **事件监听:** 由于每次都重新生成 HTML，事件监听器也需要在每次 `setupPagination` 调用时重新绑定到新生成的按钮上。这里改用了简单的 `onclick` 赋值方式。页码按钮的监听器则在 `createPageButton` 中创建时绑定。
// 5.  **页码计算调整:** 微调了 `startPage`/`endPage` 的计算逻辑，使其在靠近边界时表现更自然。
// 6.  **最终日志:** 保留了在函数末尾打印容器 `innerHTML` 的日志，方便您检查最终生成的 HTML。
//
// 请将您本地的 `pagination.js` 替换为以上代码，并再次测试数据维护页面的分页功能。同时观察控制台输出的最终 `innerHTML` 日志，确认生成的结构是否符合预期且不包含意外的隐藏