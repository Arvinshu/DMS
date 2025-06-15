/**
 * 目录结构: src/main/resources/static/js/filemanage/fulltext/filemanage_fulltext_service.js
 * 文件名称: filemanage_fulltext_service.js
 * 开发时间: 2025-06-04 14:30:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 负责处理全文搜索的业务逻辑，主要是收集搜索参数并调用API。
 * 本次更新: 将模块的IIFE返回值明确附加到 window 对象。
 */

// **关键修复**: 将IIFE的返回值赋值给 window 上的属性
window.fileManageFulltextService = (() => {

    const dom = window.fileManageFulltextDOM;
    const config = window.fileManageFulltextConfig;
    const api = window.fileManageApi;

    const collectSearchParams = (page = 1) => {
        if (!dom || !config) {
            console.error('DOM 或 Config 对象未初始化，无法收集搜索参数。');
            return null;
        }

        const searchParams = {
            query: dom.queryKeywordInput ? dom.queryKeywordInput.value.trim() : '',
            page: page,
            size: config.RESULTS_PER_PAGE,
            filters: {
                fileTypes: [],
                dateFrom: '',
                dateTo: ''
            },
            sortBy: dom.sortOptionsSelect ? dom.sortOptionsSelect.value : config.DEFAULT_SORT_BY
        };

        if (dom.fileTypeFilterContainer) {
            const checkedFileTypeCheckboxes = dom.fileTypeFilterContainer.querySelectorAll('input[type="checkbox"]:checked');
            checkedFileTypeCheckboxes.forEach(checkbox => {
                searchParams.filters.fileTypes.push(checkbox.value);
            });
        }

        if (dom.dateFromInput && dom.dateFromInput.value) {
            if (dom.dateFromInput._flatpickr && dom.dateFromInput._flatpickr.selectedDates.length > 0) {
                searchParams.filters.dateFrom = dom.dateFromInput._flatpickr.formatDate(dom.dateFromInput._flatpickr.selectedDates[0], "Y-m-d");
            } else {
                searchParams.filters.dateFrom = dom.dateFromInput.value;
            }
        }
        if (dom.dateToInput && dom.dateToInput.value) {
            if (dom.dateToInput._flatpickr && dom.dateToInput._flatpickr.selectedDates.length > 0) {
                searchParams.filters.dateTo = dom.dateToInput._flatpickr.formatDate(dom.dateToInput._flatpickr.selectedDates[0], "Y-m-d");
            } else {
                searchParams.filters.dateTo = dom.dateToInput.value;
            }
        }

        console.log('收集到的搜索参数:', searchParams);
        return searchParams;
    };

    const executeSearch = (searchParams) => {
        if (!api || typeof api.searchFulltext !== 'function') {
            console.error('fileManageApi 或 searchFulltext 方法未定义。');
            return Promise.reject(new Error('API服务不可用。'));
        }
        if (!searchParams) {
            console.error('搜索参数对象为空，无法执行搜索。');
            return Promise.reject(new Error('无效的搜索参数。'));
        }

        // 确保 fileManageApi 中有 searchFulltext 方法
        // 假设我们将在 fileManageApi.js 中添加此方法
        if (!api.searchFulltext) {
            api.searchFulltext = (params) => {
                return api.post('/fulltext/search', params); // 调用 POST /api/filemanage/fulltext/search
            };
        }
        return api.searchFulltext(searchParams);
    };

    return {
        collectSearchParams,
        executeSearch
    };

})();

console.log('fileManageFulltextService.js loaded.');
