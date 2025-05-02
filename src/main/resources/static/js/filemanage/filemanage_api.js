/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_api.js
 * 文件名: filemanage_api.js
 * 开发时间: 2025-04-29 10:32:10 EDT
 * 作者: Gemini
 * 用途: 封装文件管理和同步功能的后端 API 调用。
 */

const fileManageApi = (() => {

    const BASE_URL = '/api/filemanage'; // API 基础路径

    /**
     * 处理 Fetch API 响应
     * @param {Response} response - Fetch 响应对象
     * @returns {Promise<any>} - 解析后的 JSON 数据或在错误时 reject
     */
    const handleResponse = async (response) => { // Make async for potential text() fallback
        if (!response.ok) {
            let errorData = {};
            let message = `HTTP error! status: ${response.status}`;
            try {
                // Try to parse JSON error first
                errorData = await response.json();
                message = errorData.message || errorData.error || message;
            } catch (e) {
                // If JSON parsing fails, try to get text response
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
        // Handle successful responses (2xx)
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return response.json();
        } else {
            // For non-JSON success responses (like simple OK for delete), resolve successfully
            // Could also check for specific status codes like 204 No Content
            return Promise.resolve({ success: true, status: response.status });
        }
    };

    /**
     * 发送 GET 请求
     * @param {string} endpoint - API 端点路径 (相对于 BASE_URL)
     * @param {object} [params] - URL 查询参数对象
     * @returns {Promise<any>}
     */
    const get = (endpoint, params = {}) => {
        const url = new URL(BASE_URL + endpoint, window.location.origin);
        Object.keys(params).forEach(key => url.searchParams.append(key, params[key]));
        console.debug(`API GET: ${url}`);
        return fetch(url)
            .then(handleResponse);
    };

    /**
     * 发送 POST 请求
     * @param {string} endpoint - API 端点路径 (相对于 BASE_URL)
     * @param {object} [body] - 请求体数据 (将转为 JSON)
     * @returns {Promise<any>}
     */
    const post = (endpoint, body = {}) => {
        console.debug(`API POST: ${BASE_URL + endpoint}`, body);
        return fetch(BASE_URL + endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // Add other headers like CSRF token if needed
            },
            body: JSON.stringify(body) // POST 请求通常需要请求体
        })
            .then(handleResponse);
    };


    // --- API Function Definitions ---

    /**
     * 搜索解密目录中的文件
     * @param {string} keyword - 搜索关键字
     * @param {number} page - 页码
     * @param {number} size - 每页大小
     * @returns {Promise<PageDto<DecryptedFileDto>>}
     */
    const searchDecryptedFiles = (keyword, page, size) => {
        return get('/decrypted/search', { keyword, page, size });
    };

    /**
     * 获取文件下载的 URL (注意：此函数只生成 URL，实际下载由浏览器处理)
     * @param {string} relativePath - 相对路径
     * @param {string} filename - 文件名
     * @returns {string} - 文件下载 URL
     */
    const getDecryptedFileDownloadUrl = (relativePath, filename) => {
        const url = new URL(BASE_URL + '/decrypted/download', window.location.origin);
        // 需要对参数进行 URL 编码
        url.searchParams.append('relativePath', encodeURIComponent(relativePath || ''));
        url.searchParams.append('filename', encodeURIComponent(filename));
        console.debug(`Generated Download URL: ${url}`);
        return url.toString();
    };

    // --- NEW Encrypted File API Functions ---

    /**
     * 搜索加密源目录中的文件
     * @param {string} keyword - 搜索关键字
     * @param {number} page - 页码
     * @param {number} size - 每页大小
     * @returns {Promise<PageDto<DecryptedFileDto>>} // Still uses same DTO structure
     */
    const searchEncryptedFiles = (keyword, page, size) => {
        return get('/encrypted/search', { keyword, page, size }); // Call new endpoint
    };

    /**
     * 获取加密文件下载的 URL
     * @param {string} relativePath - 相对路径
     * @param {string} filename - 文件名
     * @returns {string} - 文件下载 URL
     */
    const getEncryptedFileDownloadUrl = (relativePath, filename) => {
        const url = new URL(BASE_URL + '/encrypted/download', window.location.origin); // Call new endpoint
        url.searchParams.append('relativePath', encodeURIComponent(relativePath || ''));
        url.searchParams.append('filename', encodeURIComponent(filename));
        console.debug(`Generated ENCRYPTED Download URL: ${url}`);
        return url.toString();
    };

    /**
     * 获取待同步的文件列表
     * @param {number} page - 页码
     * @param {number} size - 每页大小
     * @returns {Promise<PageDto<PendingFileSyncDto>>}
     */
    const getPendingFiles = (page, size) => {
        return get('/pending', { page, size });
    };

    /**
     * 获取文件同步服务的当前状态
     * @returns {Promise<FileSyncStatusDto>}
     */
    const getSyncStatus = () => {
        return get('/sync/status');
    };

    /**
     * 启动手动文件同步流程
     * @returns {Promise<FileSyncTaskControlResultDto>}
     */
    const startSync = () => {
        return post('/sync/start');
    };

    /**
     * 暂停当前的手动文件同步流程
     * @returns {Promise<FileSyncTaskControlResultDto>}
     */
    const pauseSync = () => {
        return post('/sync/pause');
    };

    /**
     * 恢复已暂停的手动文件同步流程
     * @returns {Promise<FileSyncTaskControlResultDto>}
     */
    const resumeSync = () => {
        return post('/sync/resume');
    };

    /**
     * 停止当前的手动文件同步流程
     * @returns {Promise<FileSyncTaskControlResultDto>}
     */
    const stopSync = () => {
        return post('/sync/stop');
    };

    // --- NEW: Confirm Delete API Function ---
    /**
     * 发送确认删除请求到后端。
     * @param {number[]} ids - 包含要删除记录 ID 的数组。
     * @returns {Promise<object>} - 后端返回的简单成功或错误对象。
     */
    const confirmDelete = (ids) => {
        // Ensure ids is an array
        if (!Array.isArray(ids)) {
            return Promise.reject(new Error("确认删除需要一个 ID 数组。"));
        }
        if (ids.length === 0) {
            return Promise.resolve({ success: true, message: "没有需要删除的 ID。" }); // Nothing to delete
        }
        return post('/sync/confirm-delete', ids); // Send IDs in the request body
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
        confirmDelete // Expose the new function
    };

})(); // Immediately Invoked Function Expression (IIFE) to create a module scope

