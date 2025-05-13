/**
 * 文件路径: src/main/resources/static/js/common.js
 * 开发时间: 2025-05-12 00:10:00 UTC/GMT+08:00
 * 作者: Gemini (更新者)
 * 代码用途: 公共 JavaScript 工具函数
 * 更新内容:
 * - 为 AppUtils.showLoading 和 AppUtils.hideLoading 实现引用计数机制。
 * - 在 showLoading 和 hideLoading 开头添加 console.trace() 用于调试。
 */
(function (window) {
    'use strict';

    const AppUtils = {};

    // 用于存储每个目标元素的加载请求计数器
    const loadingCounters = new Map();

    AppUtils.showMessage = function (message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);
        const messageDiv = document.createElement('div');
        messageDiv.style.position = 'fixed';
        messageDiv.style.top = '20px';
        messageDiv.style.left = '50%';
        messageDiv.style.transform = 'translateX(-50%)';
        messageDiv.style.padding = '10px 20px';
        messageDiv.style.borderRadius = '5px';
        messageDiv.style.zIndex = '10000';
        messageDiv.style.boxShadow = '0 2px 10px rgba(0,0,0,0.2)';
        messageDiv.textContent = message;
        switch (type) {
            case 'success':
                messageDiv.style.backgroundColor = '#4CAF50';
                messageDiv.style.color = 'white';
                break;
            case 'error':
                messageDiv.style.backgroundColor = '#f44336';
                messageDiv.style.color = 'white';
                break;
            case 'warning':
                messageDiv.style.backgroundColor = '#ff9800';
                messageDiv.style.color = 'black';
                break;
            default: // info
                messageDiv.style.backgroundColor = '#2196F3';
                messageDiv.style.color = 'white';
                break;
        }
        document.body.appendChild(messageDiv);
        setTimeout(() => {
            if (messageDiv.parentElement) { // 检查是否还存在于DOM中
                messageDiv.remove();
            }
        }, 3000);
    };

    AppUtils.request = async function (url, method = 'GET', data = null, headers = {}) {
        const options = {
            method: method.toUpperCase(),
            headers: {
                'Accept': 'application/json',
                ...headers
            },
        };
        if ((options.method === 'POST' || options.method === 'PUT') && data) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(data);
        }
        console.debug(`[Request] ${options.method} ${url}`, data ? `Data: ${JSON.stringify(data)}` : '');
        try {
            const response = await fetch(url, options);
            console.debug(`[Response] ${response.status} ${response.statusText} for ${url}`);
            if (!response.ok) {
                let errorData = {status: response.status, message: `HTTP error: ${response.statusText}`};
                try {
                    const errorJson = await response.json();
                    errorData.message = errorJson.message || errorData.message;
                    errorData.details = errorJson; // 包含完整的错误响应
                    console.error(`Server error response for ${url}:`, errorJson);
                } catch (e) {
                    console.error(`Could not parse error response body for ${url}. Status: ${response.status}`);
                }
                AppUtils.showMessage(`请求失败: ${errorData.message} (状态码: ${response.status})`, 'error');
                throw errorData; // 抛出包含status和message的对象
            }
            if (response.status === 204) { // No Content
                console.debug(`[Response] 204 No Content for ${url}`);
                return null; // 或者一个特定的成功对象，视API约定而定
            }
            return await response.json();
        } catch (error) {
            console.error(`[Fetch Error] ${method} ${url}:`, error);
            // 如果错误对象已经有status（来自上面抛出的errorData），则不重复显示通用网络错误
            if (!error.status) {
                AppUtils.showMessage(`网络错误或请求无法完成: ${error.message || '请检查网络连接或联系管理员'}`, 'error');
            }
            throw error; // 继续抛出，让调用者处理
        }
    };

    AppUtils.get = function (baseUrl, params = null, headers = {}) {
        let url = baseUrl;
        if (params) {
            const searchParams = new URLSearchParams();
            for (const key in params) {
                if (Object.hasOwnProperty.call(params, key) && params[key] !== null && params[key] !== undefined) {
                    const value = params[key];
                    if (Array.isArray(value)) {
                        value.forEach(item => searchParams.append(key, item));
                    } else {
                        searchParams.append(key, value);
                    }
                }
            }
            const queryString = searchParams.toString();
            if (queryString) {
                url += (url.includes('?') ? '&' : '?') + queryString;
            }
        }
        return AppUtils.request(url, 'GET', null, headers);
    };

    AppUtils.post = function (url, data, headers = {}) {
        return AppUtils.request(url, 'POST', data, headers);
    };
    AppUtils.put = function (url, data, headers = {}) {
        return AppUtils.request(url, 'PUT', data, headers);
    };
    AppUtils.delete = function (url, headers = {}) {
        return AppUtils.request(url, 'DELETE', null, headers);
    };
    AppUtils.debounce = function (func, delay) {
        let timeoutId;
        return function (...args) {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => {
                func.apply(this, args);
            }, delay);
        };
    };

    AppUtils.showLoading = function (targetElement = document.body) {
        // **修改点：添加 console.trace()**
        console.trace('[LOADING] showLoading called');

        if (!targetElement) {
            console.warn('showLoading: targetElement is null or undefined.');
            return;
        }

        let currentCount = loadingCounters.get(targetElement) || 0;
        currentCount++;
        loadingCounters.set(targetElement, currentCount);
        console.debug(`[LOADING] Show requested for:`, targetElement, `New count: ${currentCount}`);

        if (currentCount === 1) { // 只有当第一次请求显示时才创建/显示遮罩
            let overlay = targetElement.querySelector('.loading-overlay-unique-id'); // 查找已存在的
            if (!overlay) { // 如果不存在，则创建
                overlay = document.createElement('div');
                overlay.className = 'loading-overlay-unique-id'; // 使用特定类名
                overlay.style.position = (targetElement === document.body) ? 'fixed' : 'absolute';
                overlay.style.top = '0';
                overlay.style.left = '0';
                overlay.style.width = '100%';
                overlay.style.height = '100%';
                overlay.style.backgroundColor = 'rgba(255, 255, 255, 0.7)';
                overlay.style.display = 'flex';
                overlay.style.justifyContent = 'center';
                overlay.style.alignItems = 'center';
                overlay.style.zIndex = '9998'; // 确保足够高
                overlay.innerHTML = '<div class="loader"></div>'; // 确保 project_statistics.css 中有 .loader 样式

                // 如果目标元素不是body且其position是static，则设置为relative，以便遮罩层能正确定位
                if (targetElement !== document.body && getComputedStyle(targetElement).position === 'static') {
                    targetElement.style.position = 'relative';
                }
                targetElement.appendChild(overlay);
                console.debug('[LOADING] Overlay CREATED AND SHOWN on:', targetElement);
            } else { // 如果已存在，则确保它可见
                overlay.style.display = 'flex';
                console.debug('[LOADING] Overlay REUSED AND SHOWN on:', targetElement);
            }
        }
    };

    AppUtils.hideLoading = function (targetElement = document.body) {
        // **修改点：添加 console.trace()**
        console.trace('[LOADING] hideLoading called');

        if (!targetElement) {
            console.warn('hideLoading: targetElement is null or undefined.');
            return;
        }

        let currentCount = loadingCounters.get(targetElement) || 0;
        if (currentCount > 0) {
            currentCount--;
            loadingCounters.set(targetElement, currentCount);
        } else {
            console.warn('[LOADING] hideLoading called for target with zero or undefined count:', targetElement);
            // 即使计数器已经是0，也尝试移除，以防意外情况或状态不一致
        }
        console.debug(`[LOADING] Hide requested for:`, targetElement, `New count: ${currentCount}`);

        if (currentCount === 0) {
            const overlay = targetElement.querySelector('.loading-overlay-unique-id');
            if (overlay) {
                overlay.remove();
                console.debug('[LOADING] Overlay REMOVED from:', targetElement);
            } else {
                // 这个警告可能在某些情况下是正常的，例如，如果一个父元素的hideLoading移除了子元素的overlay
                console.warn('[LOADING] hideLoading: Overlay not found for removal, though count reached zero for:', targetElement);
            }
            loadingCounters.delete(targetElement); // 当计数为0时，从Map中删除该元素，避免Map无限增大
        }
    };

    AppUtils.escapeHTML = function (str) {
        if (str === null || str === undefined) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
    };

    AppUtils.formatDateIfPresent = function (dateInput, format = 'yyyy-MM-dd') {
        if (!dateInput) return '';
        try {
            let date;
            if (typeof dateInput === 'string' && dateInput.includes('T')) { // ISO 8601 with time
                date = new Date(dateInput);
            } else if (dateInput instanceof Date) {
                date = dateInput;
            } else if (typeof dateInput === 'number') { // Timestamp
                date = new Date(dateInput);
            } else if (typeof dateInput === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dateInput)) { // Already yyyy-MM-dd
                return dateInput;
            } else { // Try to parse other string formats, or could be invalid
                date = new Date(dateInput);
            }

            if (isNaN(date.getTime())) { // Check if date is valid
                console.warn("Invalid date input for formatDateIfPresent:", dateInput);
                return ''; // Return empty for invalid dates
            }

            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');

            if (format === 'yyyy-MM-dd') {
                return `${year}-${month}-${day}`;
            }
            // Add more formats if needed
            return `${year}-${month}-${day}`; // Default
        } catch (e) {
            console.error("Error formatting date:", dateInput, e);
            return ''; // Return empty on error
        }
    };

    AppUtils.getShortName = function (fullName, separator) {
        if (!fullName || typeof fullName !== 'string') {
            return '';
        }
        if (!separator || !fullName.includes(separator)) {
            return fullName.trim();
        }
        const parts = fullName.split(separator);
        return parts.pop().trim();
    };

    window.AppUtils = AppUtils;

})(window);
