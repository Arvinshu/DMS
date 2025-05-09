/**
 * 目录: src/main/resources/static/js/filemanage/filemanage_query.js
 * 文件名: filemanage_query.js
 * 开发时间: 2025-04-30 15:20:10 EDT (Update: Corrected UI function call to updateDecryptedFilesTable)
 * 作者: Gemini
 * 用途: 处理文件管理页面中“文档查询下载”部分的逻辑 (查询和下载加密文件)。
 */

const fileManageQuery = (() => {

    // --- DOM Elements ---
    const queryKeywordInput = document.getElementById('query-keyword');
    const queryBtn = document.getElementById('query-btn');
    // 注意：当前查询区域的表格和分页仍使用 'decrypted-' ID
    const resultsTableBody = document.getElementById('decrypted-files-table')?.querySelector('tbody');
    const resultsPaginationContainer = document.getElementById('decrypted-pagination');

    // --- State ---
    let currentPage = 1;
    const pageSize = 100; // 每页显示数量

    // --- Core Functions ---

    /**
     * 加载并显示加密文件列表的指定页面。
     * @param {number} page - 要加载的页码 (从 1 开始)。
     */
    const loadEncryptedFiles = (page) => {
        const keyword = queryKeywordInput ? queryKeywordInput.value.trim() : '';
        currentPage = page; // 更新当前页状态
        console.log(`Query Module: Loading ENCRYPTED files: page=${page}, keyword='${keyword}'`);

        if (!resultsTableBody) {
            console.error("文档查询表格主体 (tbody) 未找到。");
            return;
        }
        // 显示加载提示
        resultsTableBody.innerHTML = '<tr><td colspan="5" class="text-center py-4">加载中...</td></tr>'; // Colspan 5 (文件名, 路径, 大小, 日期, 操作)

        // 调用 API 获取加密文件数据
        fileManageApi.searchEncryptedFiles(keyword, page, pageSize)
            .then(pageDto => {
                // --- CORRECTED CALL ---
                // 使用正确的 UI 函数名 updateDecryptedFilesTable 来更新表格
                fileManageUI.updateDecryptedFilesTable(pageDto, loadEncryptedFiles, handleEncryptedDownloadClick);
            })
            .catch(error => {
                console.error('Query Module: 加载加密文件列表时出错:', error);
                // 将错误信息传递给 alert
                alert(`加载加密文件列表失败: ${error.message || error}`);
                // 出错时清空表格，仍使用正确的 UI 函数名
                fileManageUI.updateDecryptedFilesTable(null, loadEncryptedFiles, handleEncryptedDownloadClick);
            });
    };

    // == 解密文件加载函数 (保留，当前未使用) ==
    /**
     * 加载并显示解密文件列表的指定页面 (保留函数)。
     * @param {number} page - 要加载的页码 (从 1 开始)。
     */
    const loadDecryptedFiles_Internal = (page) => {
        const keyword = queryKeywordInput ? queryKeywordInput.value.trim() : '';
        currentPage = page;
        console.log(`Query Module: Loading DECRYPTED files: page=${page}, keyword='${keyword}'`);

        if (!resultsTableBody) {
            console.error("文档查询表格主体 (tbody) 未找到。");
            return;
        }
        resultsTableBody.innerHTML = '<tr><td colspan="5" class="text-center py-4">加载中...</td></tr>';

        // 调用 API 获取解密文件数据
        fileManageApi.searchDecryptedFiles(keyword, page, pageSize)
            .then(pageDto => {
                // 使用 UI 模块更新表格
                fileManageUI.updateDecryptedFilesTable(pageDto, loadDecryptedFiles_Internal, handleDecryptedDownloadClick_Internal);
            })
            .catch(error => {
                console.error('Query Module: 加载解密文件列表时出错:', error);
                alert(`加载解密文件列表失败: ${error.message || error}`);
                fileManageUI.updateDecryptedFilesTable(null, loadDecryptedFiles_Internal, handleDecryptedDownloadClick_Internal);
            });
    };


    // --- Event Handlers ---

    /**
     * 处理查询按钮点击事件 (当前触发加密文件搜索)。
     */
    const handleQueryClick = () => {
        loadEncryptedFiles(1); // 触发加载加密文件的第一页
    };

    // == 加密文件下载处理 (当前激活使用) ==
    /**
     * 处理加密文件下载按钮点击事件。
     * @param {object} fileData - 包含 relativePath 和 filename 的文件数据对象。
     */
    const handleEncryptedDownloadClick = (fileData) => {
        if (!fileData || !fileData.filename) {
            alert('无法下载：文件信息不完整。');
            return;
        }
        console.log(`Query Module: 请求下载加密文件: ${fileData.relativePath}${fileData.filename}`);
        try {
            // 调用 API 获取加密文件下载 URL
            const downloadUrl = fileManageApi.getEncryptedFileDownloadUrl(fileData.relativePath, fileData.filename);
            // 通过修改 window.location.href 触发浏览器下载
            window.location.href = downloadUrl;
        } catch (error) {
            console.error('Query Module: 生成加密文件下载链接时出错:', error);
            alert(`生成加密文件下载链接失败: ${error.message || error}`);
        }
    };

    // == 解密文件下载处理 (保留，当前未使用) ==
    /**
     * 处理解密文件下载按钮点击事件 (保留函数)。
     * @param {object} fileData - 包含 relativePath 和 filename 的文件数据对象。
     */
    const handleDecryptedDownloadClick_Internal = (fileData) => {
        if (!fileData || !fileData.filename) {
            alert('无法下载：文件信息不完整。');
            return;
        }
        console.log(`Query Module: 请求下载解密文件: ${fileData.relativePath}${fileData.filename}`);
        try {
            // 调用 API 获取解密文件下载 URL
            const downloadUrl = fileManageApi.getDecryptedFileDownloadUrl(fileData.relativePath, fileData.filename);
            window.location.href = downloadUrl;
        } catch (error) {
            console.error('Query Module: 生成解密文件下载链接时出错:', error);
            alert(`生成解密文件下载链接失败: ${error.message || error}`);
        }
    };

    /**
     * 处理分页控件点击事件 (事件委托，当前触发加密文件加载)。
     * @param {Event} event - 点击事件对象。
     */
    const handlePaginationClick = (event) => {
        const target = event.target;
        const paginationNav = target.closest('.pagination');
        // 确保点击事件发生在正确的（当前显示的）分页控件内
        if (paginationNav && resultsPaginationContainer && resultsPaginationContainer.contains(paginationNav)) {
            if (target.tagName === 'A' || target.tagName === 'BUTTON') { // 处理链接和按钮
                event.preventDefault();
                const pageNum = target.dataset.page;
                // 确保页码有效且按钮/链接不是禁用或当前活动状态
                if (pageNum && !target.disabled && !target.closest('li')?.classList.contains('disabled') && !target.closest('li')?.classList.contains('active')) {
                    const page = parseInt(pageNum, 10);
                    if (page !== currentPage) {
                        loadEncryptedFiles(page); // 调用加载加密文件的函数
                    }
                }
            }
        }
    };

    /**
     * 处理下载按钮点击事件 (事件委托，当前触发加密文件下载)。
     * @param {Event} event - 点击事件对象。
     */
    const handleDownloadDelegation = (event) => {
        // 查找被点击的下载按钮
        if (event.target && event.target.classList.contains('btn-download')) {
            const button = event.target;
            // 从按钮的 data-* 属性获取文件信息
            const relativePath = button.dataset.relativePath;
            const filename = button.dataset.filename;
            // 确保信息存在
            if (relativePath !== undefined && filename !== undefined) {
                handleEncryptedDownloadClick({ relativePath, filename }); // 调用加密文件下载处理函数
            } else {
                console.error("下载按钮上缺少 data-relativePath 或 data-filename 属性。");
                alert("无法下载：缺少文件信息。");
            }
        }
    };

    // --- Initialization ---
    const init = () => {
        console.log('Initializing File Manage Query Module (Activating Encrypted File Logic)...');
        // 绑定查询按钮和输入框事件
        if (queryBtn) {
            queryBtn.addEventListener('click', handleQueryClick);
        }
        if (queryKeywordInput) {
            queryKeywordInput.addEventListener('keypress', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    handleQueryClick();
                }
            });
        }

        // 绑定分页事件委托
        if (resultsPaginationContainer) {
            resultsPaginationContainer.removeEventListener('click', handlePaginationClick); // 移除旧监听器
            resultsPaginationContainer.addEventListener('click', handlePaginationClick); // 添加新监听器
        }

        // 绑定下载按钮事件委托
        if (resultsTableBody) {
            resultsTableBody.removeEventListener('click', handleDownloadDelegation); // 移除旧监听器
            resultsTableBody.addEventListener('click', handleDownloadDelegation); // 添加新监听器
        }

        // 页面加载时，默认加载第一页的加密文件
        loadEncryptedFiles(currentPage);

        console.log('File Manage Query Module (Encrypted File Logic Activated) Initialized.');
    };

    // --- Public Interface ---
    return {
        init,
        loadEncryptedFiles: loadEncryptedFiles // 如果需要外部调用
    };

})();
