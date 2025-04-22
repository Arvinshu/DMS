/**
 * 公共 JavaScript 工具函数
 * 文件路径: src/main/resources/static/js/common.js
 */

// 使用一个立即执行函数表达式 (IIFE) 来创建作用域，避免污染全局命名空间
(function(window) {
    'use strict'; // 启用严格模式

    const AppUtils = {}; // 创建一个命名空间对象

    /**
     * 显示一个简单的消息提示 (可以使用更复杂的库替代)
     * @param {string} message - 要显示的消息
     * @param {string} type - 消息类型 ('success', 'error', 'info', 'warning')
     */
    AppUtils.showMessage = function(message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);
        // 实际项目中，这里会替换为更友好的 UI 提示，例如 Toastr, SweetAlert 等
        // alert(`[${type.toUpperCase()}] ${message}`); // 避免使用 alert
        // 示例：创建一个临时 div 显示消息
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
                messageDiv.style.backgroundColor = '#4CAF50'; // 绿色
                messageDiv.style.color = 'white';
                break;
            case 'error':
                messageDiv.style.backgroundColor = '#f44336'; // 红色
                messageDiv.style.color = 'white';
                break;
            case 'warning':
                messageDiv.style.backgroundColor = '#ff9800'; // 橙色
                messageDiv.style.color = 'black';
                break;
            case 'info':
            default:
                messageDiv.style.backgroundColor = '#2196F3'; // 蓝色
                messageDiv.style.color = 'white';
                break;
        }

        document.body.appendChild(messageDiv);

        // 3秒后自动移除
        setTimeout(() => {
            messageDiv.remove();
        }, 3000);
    };


    /**
     * 核心 AJAX 请求函数
     * @param {string} url - 请求的 URL
     * @param {string} method - HTTP 方法 (GET, POST, PUT, DELETE)
     * @param {object} [data=null] - 发送的数据 (对于 POST, PUT)
     * @param {object} [headers={}] - 自定义请求头
     * @returns {Promise<object>} - 返回一个 Promise，解析为响应的 JSON 数据或在错误时拒绝
     */
    AppUtils.request = async function(url, method = 'GET', data = null, headers = {}) {
        const options = {
            method: method.toUpperCase(),
            headers: {
                'Accept': 'application/json', // 期望接收 JSON
                ...headers // 合并自定义头
            },
        };

        // 为 POST 和 PUT 请求添加 body 和 Content-Type
        if ((options.method === 'POST' || options.method === 'PUT') && data) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(data);
        }

        console.debug(`[Request] ${options.method} ${url}`, data ? `Data: ${JSON.stringify(data)}` : ''); // 调试日志

        try {
            const response = await fetch(url, options);

            console.debug(`[Response] ${response.status} ${response.statusText} for ${url}`); // 调试日志

            // 处理 HTTP 错误状态 (非 2xx)
            if (!response.ok) {
                let errorData = { status: response.status, message: `HTTP error: ${response.statusText}` };
                try {
                    // 尝试解析错误响应体 (如果服务器返回了 JSON 错误信息)
                    const errorJson = await response.json();
                    errorData.message = errorJson.message || errorData.message; // 使用服务器返回的消息
                    errorData.details = errorJson; // 包含完整的错误详情
                    console.error(`Server error response for ${url}:`, errorJson);
                } catch (e) {
                    // 如果响应体不是 JSON 或解析失败，使用默认消息
                    console.error(`Could not parse error response body for ${url}. Status: ${response.status}`);
                }
                // 抛出包含状态和消息的错误对象
                throw errorData;
            }

            // 处理 204 No Content 响应 (例如 DELETE 成功)
            if (response.status === 204) {
                return null; // 没有内容可解析，返回 null
            }

            // 解析 JSON 响应体
            return await response.json();

        } catch (error) {
            console.error(`[Fetch Error] ${method} ${url}:`, error);
            // 如果是网络错误或上面抛出的 HTTP 错误，重新抛出给调用者处理
            // 可以包装成更具体的错误类型
            throw error;
        }
    };

    /**
     * 发送 GET 请求的便捷函数
     * @param {string} url - 请求的 URL (可以包含查询参数)
     * @param {object} [headers={}] - 自定义请求头
     * @returns {Promise<object>}
     */
    AppUtils.get = function(url, headers = {}) {
        return AppUtils.request(url, 'GET', null, headers);
    };

    /**
     * 发送 POST 请求的便捷函数
     * @param {string} url - 请求的 URL
     * @param {object} data - 要发送的 JSON 数据
     * @param {object} [headers={}] - 自定义请求头
     * @returns {Promise<object>}
     */
    AppUtils.post = function(url, data, headers = {}) {
        return AppUtils.request(url, 'POST', data, headers);
    };

    /**
     * 发送 PUT 请求的便捷函数
     * @param {string} url - 请求的 URL
     * @param {object} data - 要发送的 JSON 数据
     * @param {object} [headers={}] - 自定义请求头
     * @returns {Promise<object>}
     */
    AppUtils.put = function(url, data, headers = {}) {
        return AppUtils.request(url, 'PUT', data, headers);
    };

    /**
     * 发送 DELETE 请求的便捷函数
     * @param {string} url - 请求的 URL
     * @param {object} [headers={}] - 自定义请求头
     * @returns {Promise<null>} - DELETE 成功通常返回 null (204 No Content)
     */
    AppUtils.delete = function(url, headers = {}) {
        return AppUtils.request(url, 'DELETE', null, headers);
    };

    /**
     * 简单的防抖函数
     * @param {Function} func - 要执行的函数
     * @param {number} delay - 延迟时间 (毫秒)
     * @returns {Function} - 包装后的防抖函数
     */
    AppUtils.debounce = function(func, delay) {
        let timeoutId;
        return function(...args) {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => {
                func.apply(this, args);
            }, delay);
        };
    };


    // 将 AppUtils 暴露到全局 window 对象
    window.AppUtils = AppUtils;

})(window); // 传入 window 对象

// * **说明:**
//     * 使用 IIFE 创建了 `AppUtils` 命名空间。
//     * `showMessage`: 提供一个简单的 UI 提示功能（实际项目应替换为 UI 库）。
//     * `request`: 核心 AJAX 函数，使用 `fetch` API，处理请求头、请求体、JSON 解析和基本的 HTTP 错误。它返回一个 Promise。
//     * `get`, `post`, `put`, `delete`: 对 `request` 的便捷封装。
//     * `debounce`: 提供了一个简单的防抖函数，可用于处理频繁触发的事件（如输入框的 `input` 事