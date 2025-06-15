/**
 * 目录结构: src/main/resources/static/js/filemanage/fulltext/filemanage_fulltext_dom.js
 * 文件名称: filemanage_fulltext_dom.js
 * 开发时间: 2025-06-04 10:05:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 集中管理全文搜索模块所需的DOM元素引用。
 * 本次更新: 添加开始和结束日志，用于调试脚本执行流程。
 */

console.log('--- [DEBUG] filemanage_fulltext_dom.js: 开始执行...');

const fileManageFulltextDOM = {
    // 主内容区域
    section: null,

    // 查询输入区
    queryKeywordInput: null,
    clearSearchIcon: null,
    queryBtn: null,

    // 高级筛选区
    advancedFiltersToggleBtn: null, // 展开/收起按钮
    advancedFiltersContainer: null, // 筛选器外层容器 (用于控制显示/隐藏)
    fileTypeFilterContainer: null,  // 文件类型复选框的容器
    clearFileTypeFilterBtn: null,
    dateFromInput: null,
    dateToInput: null,
    clearDateFilterBtn: null,
    // applyFiltersBtn: null, // 如果有“应用筛选”按钮

    // 结果统计与排序区
    resultsCountSpan: null,
    sortOptionsSelect: null,

    // 结果展示区
    resultsListContainer: null,

    // 分页区
    paginationContainer: null,

    /**
     * 初始化函数，获取并缓存所有相关的DOM元素。
     * 应在 DOMContentLoaded 之后，模块主逻辑执行之前调用。
     */
    init: function() {
        console.log('--- [DEBUG] fileManageFulltextDOM.init() 被调用。');
        this.section = document.getElementById('fulltext-search-section');

        // 查询输入区元素
        this.queryKeywordInput = document.getElementById('fulltext-query-keyword');
        this.clearSearchIcon = document.getElementById('fulltext-clear-search-icon');
        this.queryBtn = document.getElementById('fulltext-query-btn');

        // 高级筛选区元素
        this.advancedFiltersToggleBtn = document.getElementById('fulltext-advanced-filters-toggle');
        this.advancedFiltersContainer = document.getElementById('fulltext-advanced-filters-container');
        this.fileTypeFilterContainer = document.getElementById('fulltext-filter-file-type');
        this.clearFileTypeFilterBtn = document.getElementById('fulltext-clear-file-type-filter');
        this.dateFromInput = document.getElementById('fulltext-filter-date-from');
        this.dateToInput = document.getElementById('fulltext-filter-date-to');
        this.clearDateFilterBtn = document.getElementById('fulltext-clear-date-filter');
        // this.applyFiltersBtn = document.getElementById('fulltext-apply-filters-btn'); // 如果有

        // 结果统计与排序区元素
        this.resultsCountSpan = document.getElementById('fulltext-results-count');
        this.sortOptionsSelect = document.getElementById('fulltext-sort-options');

        // 结果展示区元素
        this.resultsListContainer = document.getElementById('fulltext-results-list');

        // 分页区元素
        this.paginationContainer = document.getElementById('fulltext-pagination');

        if (!this.section || !this.queryKeywordInput || !this.queryBtn || !this.resultsListContainer || !this.paginationContainer) {
            console.warn('fileManageFulltextDOM: 一个或多个核心DOM元素未找到。请检查HTML ID是否正确。');
        }
        console.log('--- [DEBUG] fileManageFulltextDOM.init() 执行完毕。');
    }
};

// **关键修复**: 将对象明确附加到 window 上
window.fileManageFulltextDOM = fileManageFulltextDOM;
console.log('--- [DEBUG] filemanage_fulltext_dom.js: 执行完毕，fileManageFulltextDOM 对象已创建。');
