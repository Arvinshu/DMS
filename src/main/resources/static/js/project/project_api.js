/**
 * 文件路径: src/main/resources/static/js/project/project_api.js
 * 开发时间: 2025-04-25 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 封装项目管理模块所有与后端 API 的交互逻辑。
 * 更新内容: 修改 fetchProjects, getProjectDetail, fetchTasks, getAllStages, getAllTags 调用 AppUtils.get 的方式。
 * 依赖: common.js (提供 AppUtils 对象)
 */
const ProjectApiModule = (() => {

    // API 端点常量
    const PROJECT_API_URL = '/api/projects';
    const TASK_API_URL = '/api/tasks';
    const STAGE_API_URL = '/api/project-stages';
    const TAG_API_URL = '/api/project-tags';

    /**
     * 获取项目列表 (带过滤、分页、排序)
     * @param {object} params - 查询参数对象
     * @returns {Promise<object>}
     */
    async function fetchProjects(params = {}) {
        console.debug('API Call: fetchProjects, params:', params);
        // *** 修改: 将 params 作为第二个参数传递给 AppUtils.get ***
        return AppUtils.get(PROJECT_API_URL, params);
    }

    /**
     * 获取单个项目详情
     * @param {number|string} projectId - 项目 ID
     * @returns {Promise<object>}
     */
    async function getProjectDetail(projectId) {
        console.debug('API Call: getProjectDetail, projectId:', projectId);
        if (!projectId) throw new Error('Project ID is required for getProjectDetail');
        // *** 修改: 无参数，第二个参数为 null 或省略 ***
        return AppUtils.get(`${PROJECT_API_URL}/${projectId}`);
    }

    /**
     * 创建新项目
     * @param {object} projectData - 项目数据 (ProjectCreateDto)
     * @returns {Promise<object>}
     */
    async function createProject(projectData) {
        console.debug('API Call: createProject, data:', projectData);
        return AppUtils.post(PROJECT_API_URL, projectData);
    }

    /**
     * 更新项目
     * @param {number|string} projectId - 项目 ID
     * @param {object} projectData - 更新的项目数据 (ProjectCreateDto)
     * @returns {Promise<object>}
     */
    async function updateProject(projectId, projectData) {
        console.debug('API Call: updateProject, projectId:', projectId, 'data:', projectData);
        if (!projectId) throw new Error('Project ID is required for updateProject');
        return AppUtils.put(`${PROJECT_API_URL}/${projectId}`, projectData);
    }

    /**
     * 删除项目
     * @param {number|string} projectId - 项目 ID
     * @returns {Promise<void>}
     */
    async function deleteProject(projectId) {
        console.debug('API Call: deleteProject, projectId:', projectId);
        if (!projectId) throw new Error('Project ID is required for deleteProject');
        return AppUtils.delete(`${PROJECT_API_URL}/${projectId}`);
    }

    /**
     * 获取项目创建/搜索所需的下拉选项数据
     * @returns {Promise<object>}
     */
    async function getProjectLookups() {
        console.debug('API Call: getProjectLookups');
        // *** 修改: 无参数 ***
        return AppUtils.get(`${PROJECT_API_URL}/lookups`);
    }

    // --- 任务相关 API ---

    /**
     * 获取指定项目下的任务列表
     * @param {number|string} projectId - 项目 ID
     * @returns {Promise<Array<object>>}
     */
    async function fetchTasks(projectId) {
        console.debug('API Call: fetchTasks, projectId:', projectId);
        if (!projectId) throw new Error('Project ID is required for fetchTasks');
        // *** 修改: 无参数 ***
        return AppUtils.get(`${TASK_API_URL}/project/${projectId}`);
    }

    /**
     * 获取单个任务详情
     * @param {number|string} taskId - 任务 ID
     * @returns {Promise<object>} Promise resolving with the task DTO
     */
    async function getTaskById(taskId) { // 添加这个函数
        console.debug('API Call: getTaskById, taskId:', taskId);
        if (!taskId) throw new Error('Task ID is required for getTaskById');
        // *** 修改: 无参数 ***
        return AppUtils.get(`${TASK_API_URL}/${taskId}`);
    }


    /**
     * 创建新任务
     * @param {object} taskData - 任务数据 (TaskDto)
     * @returns {Promise<object>}
     */
    async function createTask(taskData) {
        console.debug('API Call: createTask, data:', taskData);
        return AppUtils.post(TASK_API_URL, taskData);
    }

    /**
     * 更新任务
     * @param {number|string} taskId - 任务 ID
     * @param {object} taskData - 更新的任务数据 (TaskDto)
     * @returns {Promise<object>}
     */
    async function updateTask(taskId, taskData) {
        console.debug('API Call: updateTask, taskId:', taskId, 'data:', taskData);
        if (!taskId) throw new Error('Task ID is required for updateTask');
        return AppUtils.put(`${TASK_API_URL}/${taskId}`, taskData);
    }

    /**
     * 删除任务
     * @param {number|string} taskId - 任务 ID
     * @returns {Promise<void>}
     */
    async function deleteTask(taskId) {
        console.debug('API Call: deleteTask, taskId:', taskId);
        if (!taskId) throw new Error('Task ID is required for deleteTask');
        return AppUtils.delete(`${TASK_API_URL}/${taskId}`);
    }

    // --- 阶段和标签查询 API ---

    /**
     * 获取所有启用的项目阶段
     * @returns {Promise<Array<object>>}
     */
    async function getAllStages() {
        console.debug('API Call: getAllStages');
        // *** 修改: 无参数 ***
        return AppUtils.get(`${STAGE_API_URL}/all-enabled`);
    }

    /**
     * 获取所有项目标签
     * @returns {Promise<Array<object>>}
     */
    async function getAllTags() {
        console.debug('API Call: getAllTags');
        // *** 修改: 无参数 ***
        return AppUtils.get(`${TAG_API_URL}/all`);
    }


    // 暴露公共方法
    return {
        fetchProjects,
        getProjectDetail,
        createProject,
        updateProject,
        deleteProject,
        getProjectLookups,
        fetchTasks,
        getTaskById, // 暴露 getTaskById
        createTask,
        updateTask,
        deleteTask,
        getAllStages,
        getAllTags
    };

})();
