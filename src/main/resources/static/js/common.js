/**
 * 文件路径: src/main/resources/static/js/common.js
 * 开发时间: 2025-04-25 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 公共 JavaScript 工具函数
 * 更新内容: 修改 AppUtils.get 方法，使用 URLSearchParams 处理 GET 请求参数，确保正确编码。
 */
(function (window) {
    'use strict';

    const AppUtils = {};

    AppUtils.showMessage = function (message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);
        const messageDiv = document.createElement('div');
        // ... (样式代码不变) ...
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
            default:
                messageDiv.style.backgroundColor = '#2196F3';
                messageDiv.style.color = 'white';
                break;
        }
        document.body.appendChild(messageDiv);
        setTimeout(() => {
            messageDiv.remove();
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
                    errorData.details = errorJson;
                    console.error(`Server error response for ${url}:`, errorJson);
                } catch (e) {
                    console.error(`Could not parse error response body for ${url}. Status: ${response.status}`);
                }
                // *** 在抛出错误前显示消息 ***
                AppUtils.showMessage(`请求失败: ${errorData.message} (状态码: ${response.status})`, 'error');
                throw errorData;
            }
            if (response.status === 204) return null;
            return await response.json();
        } catch (error) {
            console.error(`[Fetch Error] ${method} ${url}:`, error);
            // 如果不是上面抛出的 HTTP 错误对象，则显示通用网络错误消息
            if (!error.status) {
                AppUtils.showMessage(`网络错误或请求无法完成: ${error.message || '请检查网络连接或联系管理员'}`, 'error');
            }
            throw error;
        }
    };

    /**
     * 发送 GET 请求的便捷函数 (修正：处理查询参数)
     * @param {string} baseUrl - 请求的基础 URL (不含查询参数)
     * @param {object} [params=null] - 查询参数对象 {key: value, ...}
     * @param {object} [headers={}] - 自定义请求头
     * @returns {Promise<object>}
     */
    AppUtils.get = function (baseUrl, params = null, headers = {}) {
        let url = baseUrl;
        if (params) {
            // 使用 URLSearchParams 自动处理参数编码
            const searchParams = new URLSearchParams();
            for (const key in params) {
                // 检查属性是否是对象自身的属性，并且值不是 null 或 undefined
                if (Object.hasOwnProperty.call(params, key) && params[key] !== null && params[key] !== undefined) {
                    const value = params[key];
                    // 如果值是数组 (例如 tagIds)，则为每个元素添加一个参数
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
        // 调用 request，不再传递 params，因为它们已在 URL 中
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
        if (!targetElement) return;
        let overlay = targetElement.querySelector('.loading-overlay-unique-id');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.className = 'loading-overlay-unique-id';
            overlay.style.position = (targetElement === document.body) ? 'fixed' : 'absolute';
            overlay.style.top = '0';
            overlay.style.left = '0';
            overlay.style.width = '100%';
            overlay.style.height = '100%';
            overlay.style.backgroundColor = 'rgba(255, 255, 255, 0.7)';
            overlay.style.display = 'flex';
            overlay.style.justifyContent = 'center';
            overlay.style.alignItems = 'center';
            overlay.style.zIndex = '9998';
            overlay.innerHTML = '<div class="loader"></div>';
            if (targetElement !== document.body && getComputedStyle(targetElement).position === 'static') {
                targetElement.style.position = 'relative';
            }
            targetElement.appendChild(overlay);
            console.debug('Loading overlay shown on:', targetElement);
        } else {
            overlay.style.display = 'flex';
            console.debug('Loading overlay reused on:', targetElement);
        }
    };

    AppUtils.hideLoading = function (targetElement = document.body) {
        if (!targetElement) return;
        const overlay = targetElement.querySelector('.loading-overlay-unique-id');
        if (overlay) {
            console.log('hideLoading: Overlay found. Removing it.');
            overlay.remove(); // 直接移除元素
            console.debug('Loading overlay removed from:', targetElement);
        } else {
            console.warn('hideLoading: Overlay not found inside:', targetElement);
        }
    };

    AppUtils.escapeHTML = function (str) {
        if (str === null || str === undefined) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    };

    AppUtils.formatDateIfPresent = function (dateInput, format = 'yyyy-MM-dd') {
        if (!dateInput) return '';
        try {
            let date;
            if (typeof dateInput === 'string' && dateInput.includes('T')) {
                date = new Date(dateInput);
            } else if (dateInput instanceof Date) {
                date = dateInput;
            } else if (typeof dateInput === 'number') {
                date = new Date(dateInput);
            } else if (typeof dateInput === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dateInput)) {
                return dateInput;
            } else {
                date = new Date(dateInput);
            }
            if (isNaN(date.getTime())) {
                return '';
            }
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        } catch (e) {
            console.error("Error formatting date:", dateInput, e);
            return '';
        }
    };

    window.AppUtils = AppUtils;

})(window);
