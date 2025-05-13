/**
 * 文件路径: src/main/resources/static/js/project/statistics/project_statistics_api.js
 * 开发时间: 2025-05-10 23:05:00 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 封装项目统计模块相关的API请求。
 * 更新: 改进 STATS_API_BASE_URL 初始化逻辑，增加日志，使用 AppUtils.showMessage。
 */
'use strict';

let STATS_API_BASE_URL_INSTANCE; // 用于存储已初始化的API基础URL

/**
 * 获取并返回API的基础URL。
 * 它会尝试从全局变量 CONTEXT_PATH 构建URL。
 * 添加了日志以帮助调试 CONTEXT_PATH 的值和最终的URL。
 * @returns {string} API的基础URL。
 */
function getStatsApiBaseUrl() {
    if (!STATS_API_BASE_URL_INSTANCE) {
        let contextPathValue = ''; // 默认为空字符串（根路径部署）

        if (typeof CONTEXT_PATH !== 'undefined') {
            console.log(`[Stats API] CONTEXT_PATH found: "${CONTEXT_PATH}" (type: ${typeof CONTEXT_PATH})`);
            if (typeof CONTEXT_PATH === 'string') {
                contextPathValue = CONTEXT_PATH;
            } else {
                console.warn(`[Stats API] CONTEXT_PATH is not a string. Defaulting to empty context path.`);
            }
        } else {
            console.warn('[Stats API] CONTEXT_PATH is not defined. Defaulting to empty context path. This might lead to incorrect API URLs if the app is not deployed at root.');
        }

        // 规范化 contextPathValue:
        // 1. 如果非空且不以 "/" 开头, 则在前面添加 "/"
        if (contextPathValue && !contextPathValue.startsWith('/')) {
            contextPathValue = '/' + contextPathValue;
        }
        // 2. 如果不是根路径 "/" 且以 "/" 结尾, 则移除末尾的 "/"
        if (contextPathValue !== '/' && contextPathValue.endsWith('/')) {
            contextPathValue = contextPathValue.slice(0, -1);
        }
        // 3. 如果 contextPathValue 是 "/" (通常不会，@{/} 对于根路径是空字符串), 则保持不变或视为空字符串处理
        //    对于我们的目的，如果 contextPathValue 是 "/" , 最终URL会是 "//api/project-stats", 这是不好的。
        //    所以如果它是 "/" , 我们应该视作空字符串，这样得到 "/api/project-stats"
        if (contextPathValue === '/') {
            contextPathValue = '';
        }

        STATS_API_BASE_URL_INSTANCE = `${contextPathValue}/api/project-stats`;
        console.log(`[Stats API] STATS_API_BASE_URL initialized to: "${STATS_API_BASE_URL_INSTANCE}" (based on context: "${CONTEXT_PATH}")`);
        console.log(`[Stats API] Current page origin: ${window.location.origin}`);
    }
    return STATS_API_BASE_URL_INSTANCE;
}


/**
 * 处理API响应，检查是否成功，并返回JSON数据或抛出错误。
 * @param {Response} response - Fetch API的响应对象。
 * @returns {Promise<Object>} - 解析后的JSON数据中的 'data' 字段。
 * @throws {Error} - 如果响应不成功或数据格式不正确。
 */
async function handleApiResponse(response) {
    if (!response.ok) {
        let errorData;
        let errorMessage = `API请求失败，状态码: ${response.status}`;
        try {
            errorData = await response.json(); // 后端应该返回 { success: false, message: "...", data: null }
            if (errorData && errorData.message) {
                errorMessage = errorData.message;
            }
        } catch (e) {
            // 如果响应体不是JSON或解析失败，使用HTTP状态文本
            errorMessage = `网络响应错误: ${response.status} ${response.statusText || 'Unknown error'}`;
        }
        console.error(`[API Error] Status: ${response.status}, Message: ${errorMessage}, URL: ${response.url}`);
        throw new Error(errorMessage);
    }
    const result = await response.json(); // 后端应返回 { success: true, data: {...} }
    if (result && result.success === true && typeof result.data !== 'undefined') {
        return result.data;
    } else if (result && result.success === false && result.message) {
        console.error(`[API Operation Failed] Message: ${result.message}, URL: ${response.url}`);
        throw new Error(result.message);
    } else {
        console.error(`[API Response Invalid] Unexpected format from URL: ${response.url}`, result);
        throw new Error('API响应格式不正确或操作失败');
    }
}

/**
 * 获取所有在任务中被分配过的员工列表（去重）。
 * @returns {Promise<Array<String>>} - 员工姓名列表。
 */
async function getAssignees() {
    const apiUrl = `${getStatsApiBaseUrl()}/assignees`;
    console.log(`[Stats API] Fetching assignees from: ${apiUrl}`);
    try {
        const response = await fetch(apiUrl);
        return await handleApiResponse(response);
    } catch (error) {
        console.error('获取任务负责人列表失败:', error);
        if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
            AppUtils.showMessage('获取任务负责人列表失败: ' + error.message, 'error');
        }
        throw error;
    }
}

/**
 * 获取部门整体概览统计数据。
 * @param {string} dateRange - 日期范围字符串。
 * @param {string} projectTypeDimension - 项目构成分析的维度。
 * @returns {Promise<Object>} - 部门概览数据。
 */
async function getDepartmentOverviewStats(dateRange, projectTypeDimension) {
    const params = new URLSearchParams({
        dateRange: dateRange,
        projectTypeDimension: projectTypeDimension
    });
    const apiUrl = `${getStatsApiBaseUrl()}/department-overview?${params.toString()}`;
    console.log(`[Stats API] Fetching department overview from: ${apiUrl}`);
    try {
        const response = await fetch(apiUrl);
        return await handleApiResponse(response);
    } catch (error) {
        console.error('获取部门整体概览数据失败:', error);
        if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
            AppUtils.showMessage('获取部门整体概览数据失败: ' + error.message, 'error');
        }
        throw error;
    }
}

/**
 * 获取被识别为有风险的项目列表。
 * @returns {Promise<Array<Object>>} - 风险项目列表。
 */
async function getAtRiskProjects() {
    const apiUrl = `${getStatsApiBaseUrl()}/at-risk-projects`;
    console.log(`[Stats API] Fetching at-risk projects from: ${apiUrl}`);
    try {
        const response = await fetch(apiUrl);
        return await handleApiResponse(response);
    } catch (error) {
        console.error('获取风险项目列表失败:', error);
        if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
            AppUtils.showMessage('获取风险项目列表失败: ' + error.message, 'error');
        }
        throw error;
    }
}

/**
 * 获取即将到期和已逾期的任务列表。
 * @param {number} upcomingDueDays - 定义“即将到期”的天数。
 * @returns {Promise<Object>} - 包含任务列表的对象。
 */
async function getTaskDeadlineInfo(upcomingDueDays) {
    const params = new URLSearchParams({
        upcomingDueDays: upcomingDueDays
    });
    const apiUrl = `${getStatsApiBaseUrl()}/task-deadlines?${params.toString()}`;
    console.log(`[Stats API] Fetching task deadline info from: ${apiUrl}`);
    try {
        const response = await fetch(apiUrl);
        return await handleApiResponse(response);
    } catch (error) {
        console.error('获取任务到期与逾期信息失败:', error);
        if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
            AppUtils.showMessage('获取任务到期与逾期信息失败: ' + error.message, 'error');
        }
        throw error;
    }
}

/**
 * 获取特定员工的详细工作情况统计。
 * @param {string} employeeName - 员工姓名。
 * @param {string} dateRange - 日期范围字符串。
 * @returns {Promise<Object>} - 员工个人工作详情数据。
 */
async function getEmployeeDetails(employeeName, dateRange) {
    const params = new URLSearchParams({
        employeeName: employeeName,
        dateRange: dateRange
    });
    const apiUrl = `${getStatsApiBaseUrl()}/employee-details?${params.toString()}`;
    console.log(`[Stats API] Fetching employee details for "${employeeName}" from: ${apiUrl}`);
    try {
        const response = await fetch(apiUrl);
        return await handleApiResponse(response);
    } catch (error) {
        console.error(`获取员工 "${employeeName}" 的工作详情失败:`, error);
        if (typeof AppUtils !== 'undefined' && AppUtils.showMessage) {
            AppUtils.showMessage(`获取员工 "${employeeName}" 的工作详情失败: ` + error.message, 'error');
        }
        throw error;
    }
}
