/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_api.js
 * 文件名: filemanage_api.js
 * 开发时间: 2025-04-29 10:32:10 EDT
 * 作者: Gemini
 * 代码用途: 封装文件管理和同步功能的后端 API 调用。
 * 本次更新: 将模块的IIFE返回值明确附加到 window 对象，并添加 searchFulltext 方法。
 */

// **关键修复**: 将IIFE的返回值赋值给 window 上的属性
window.fileManageApi = (() => {

    const BASE_URL = '/api/filemanage'; // API 基础路径

    const handleResponse = async (response) => {
        if (!response.ok) {
            let errorData = {};
            let message = `HTTP error! status: ${response.status}`;
            try {
                errorData = await response.json();
                message = errorData.message || errorData.error || message;
            } catch (e) {
                try {
                    const textResponse = await response.text();
                    if (textResponse) message = textResponse;
                } catch (textErr) {
                    // Ignore text parsing error
                }
            }
            console.error('API Error:', message, errorData);
            return Promise.reject(new Error(message));
        }
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return response.json();
        } else {
            return Promise.resolve({ success: true, status: response.status });
        }
    };

    const get = (endpoint, params = {}) => {
        const url = new URL(BASE_URL + endpoint, window.location.origin);
        Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));
        console.debug(`API GET: ${url}`);
        return fetch(url).then(handleResponse);
    };

    const post = (endpoint, body = {}) => {
        console.debug(`API POST: ${BASE_URL + endpoint}`, body);
        return fetch(BASE_URL + endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        }).then(handleResponse);
    };

    // --- API Function Definitions ---

    const searchDecryptedFiles = (keyword, page, size) => {
        return get('/decrypted/search', { keyword, page, size });
    };

    const getDecryptedFileDownloadUrl = (relativePath, filename) => {
        const url = new URL(BASE_URL + '/decrypted/download', window.location.origin);
        url.searchParams.append('relativePath', encodeURIComponent(relativePath || ''));
        url.searchParams.append('filename', encodeURIComponent(filename));
        console.debug(`Generated Download URL: ${url}`);
        return url.toString();
    };

    const searchEncryptedFiles = (keyword, page, size) => {
        return get('/encrypted/search', { keyword, page, size });
    };

    const getEncryptedFileDownloadUrl = (relativePath, filename) => {
        const url = new URL(BASE_URL + '/encrypted/download', window.location.origin);
        url.searchParams.append('relativePath', encodeURIComponent(relativePath || ''));
        url.searchParams.append('filename', encodeURIComponent(filename));
        console.debug(`Generated ENCRYPTED Download URL: ${url}`);
        return url.toString();
    };

    const getPendingFiles = (page, size) => {
        return get('/pending', { page, size });
    };

    const getSyncStatus = () => {
        return get('/sync/status');
    };

    const startSync = () => {
        return post('/sync/start');
    };

    const pauseSync = () => {
        return post('/sync/pause');
    };

    const resumeSync = () => {
        return post('/sync/resume');
    };

    const stopSync = () => {
        return post('/sync/stop');
    };

    const confirmDelete = (ids) => {
        if (!Array.isArray(ids)) {
            return Promise.reject(new Error("确认删除需要一个 ID 数组。"));
        }
        if (ids.length === 0) {
            return Promise.resolve({ success: true, message: "没有需要删除的 ID。" });
        }
        return post('/sync/confirm-delete', ids);
    };

    /**
     * 新增: 调用后端全文搜索API
     * @param {object} searchParams - 包含查询条件、分页、筛选和排序的对象
     * @returns {Promise<PageDto<FulltextSearchResultDto>>}
     */
    const searchFulltext = (searchParams) => {
        return post('/fulltext/search', searchParams);
    };

    // --- Public Interface ---
    return {
        searchDecryptedFiles,
        getDecryptedFileDownloadUrl,
        searchEncryptedFiles,
        getEncryptedFileDownloadUrl,
        getPendingFiles,
        getSyncStatus,
        startSync,
        pauseSync,
        resumeSync,
        stopSync,
        confirmDelete,
        searchFulltext // 暴露新的方法
    };

})();

console.log("fileManageApi.js loaded and attached to window.");
