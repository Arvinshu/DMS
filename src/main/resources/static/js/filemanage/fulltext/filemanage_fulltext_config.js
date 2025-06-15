/**
 * 目录结构: src/main/resources/static/js/filemanage/fulltext/filemanage_fulltext_config.js
 * 文件名称: filemanage_fulltext_config.js
 * 开发时间: 2025-06-04 10:00:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 存放全文搜索模块特有的常量和配置。
 */

// 使用 const 声明，并通过 IIFE 模式或者直接暴露对象的方式来避免全局污染
// 这里我们直接定义一个全局可访问的常量对象 (简单起见)
// 如果项目中有更复杂的模块管理系统 (如 ES6 Modules in browser with a bundler),
// 则可以采用 export const ... 的方式。
// 考虑到当前项目的JS结构，一个简单的全局对象是可行的。

const fileManageFulltextConfig = {
    /**
     * 每页显示的搜索结果数量。
     * 与设计文档中的分页设置保持一致。
     */
    RESULTS_PER_PAGE: 50,

    /**
     * 后端全文搜索API的端点路径。
     * 集中管理API路径，方便后续修改。
     */
    API_ENDPOINT_SEARCH: '/api/filemanage/fulltext/search', // 已在 fileManageApi.js 中定义，此处可作为参考或冗余

    /**
     * 默认的排序方式。
     * 可选值: "relevance", "modifiedDate_desc", "modifiedDate_asc", "filename_asc", "filename_desc"
     */
    DEFAULT_SORT_BY: 'relevance',

    /**
     * 支持的文件类型筛选选项。
     * 结构: { value: "实际值", label: "显示标签", id: "checkbox的id" }
     * 这些可以从后端动态获取，或者在此处硬编码作为初始列表。
     * 设计文档中HTML部分已硬编码了几个示例，这里可以定义更完整的列表。
     */
    SUPPORTED_FILE_TYPES: [
        { value: 'docx', label: 'DOCX', id: 'ftype-docx' },
        { value: 'pdf', label: 'PDF', id: 'ftype-pdf' },
        { value: 'txt', label: 'TXT', id: 'ftype-txt' },
        { value: 'xlsx', label: 'XLSX', id: 'ftype-xlsx' },
        { value: 'pptx', label: 'PPTX', id: 'ftype-pptx' },
        { value: 'doc', label: 'DOC', id: 'ftype-doc' },
        { value: 'xls', label: 'XLS', id: 'ftype-xls' },
        { value: 'ppt', label: 'PPT', id: 'ftype-ppt' },
    ],

    /**
     * 文件类型图标的映射。
     * key 为文件后缀名 (小写)，value 为 Font Awesome 图标类。
     * 用于在结果列表中显示文件类型图标。
     */
    FILE_TYPE_ICONS: {
        'default': 'far fa-file', // 默认图标
        'docx': 'far fa-file-word',
        'doc': 'far fa-file-word',
        'pdf': 'far fa-file-pdf',
        'txt': 'far fa-file-alt',
        'xlsx': 'far fa-file-excel',
        'xls': 'far fa-file-excel',
        'pptx': 'far fa-file-powerpoint',
        'ppt': 'far fa-file-powerpoint',
        'zip': 'far fa-file-archive',
        'rar': 'far fa-file-archive',
        'jpg': 'far fa-file-image',
        'jpeg': 'far fa-file-image',
        'png': 'far fa-file-image',
        'gif': 'far fa-file-image',
        // 根据需要添加更多类型
    },

    /**
     * Flatpickr 日期选择器的通用配置。
     */
    FLATPICKR_DEFAULT_CONFIG: {
        dateFormat: "Y-m-d", // 日期格式
        altInput: true, // 显示一个用户友好的格式，实际提交的是 dateFormat
        altFormat: "Y年m月d日", // 用户看到的格式
        allowInput: true, // 允许手动输入
        locale: "zh" // 假设已引入 flatpickr 的中文语言包
        // monthSelectorType: 'static', // 或者 'dropdown'
    }

    // 可以根据需要添加更多模块相关的配置项
    // 例如：搜索关键词高亮标签，加载提示文本等
};

// **关键修复**: 将对象明确附加到 window 上，使其成为真正的全局对象
window.fileManageFulltextConfig = fileManageFulltextConfig;
// 确保脚本加载后此对象可用
console.log('fileManageFulltextConfig.js loaded and configured.');

