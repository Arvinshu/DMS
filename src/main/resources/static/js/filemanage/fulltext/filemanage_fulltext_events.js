/**
 * 目录结构: src/main/resources/static/js/filemanage/fulltext/filemanage_fulltext_events.js
 * 文件名称: filemanage_fulltext_events.js
 * 开发时间: 2025-06-04 15:00:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 全文搜索模块的主入口和事件协调器。
 * 本次更新: 修正了 handleDownloadClick 方法，以正确处理包含子目录的文件的下载路径。
 */

// **关键修复**: 将IIFE的返回值赋值给 window 上的属性
window.fileManageFulltextEvents = (() => {

    // 依赖在 init 方法内部获取
    let dom, ui, service, config, globalApi;

    let currentPage = 1;

    const handleSearch = (page = 1) => {
        currentPage = page;
        if (!service || !ui) {
            console.error('Service 或 UI 模块未初始化。');
            return;
        }
        const searchParams = service.collectSearchParams(currentPage);
        if (!searchParams) return;
        ui.displayLoading(true);
        service.executeSearch(searchParams)
            .then(pageDto => {
                if (pageDto) {
                    ui.renderResultsList(pageDto.content, handleDownloadClick);
                    ui.updatePaginationUI(pageDto, handleSearch);
                    ui.updateResultsCount(pageDto.totalElements);
                    ui.displayNoResults(pageDto.content.length === 0, searchParams.query);
                } else {
                    console.error('API调用成功但返回的pageDto无效。');
                    ui.displayError('无法获取有效的搜索结果。');
                    ui.updateResultsCount(0);
                    ui.updatePaginationUI({ pageNumber: 1, totalPages: 0, totalElements: 0 }, handleSearch);
                }
            })
            .catch(error => {
                console.error("全文搜索失败:", error);
                ui.displayError(error.message || '搜索时发生未知错误。');
                ui.updateResultsCount(0);
                ui.updatePaginationUI({ pageNumber: currentPage, totalPages: 0, totalElements: 0 }, handleSearch);
            })
            .finally(() => {
                ui.displayLoading(false);
            });
    };

    /**
     * 处理结果项中文件名（下载链接）的点击事件。
     * @param {object} fileData - 包含 sourceRelativePath 和 sourceFilename 的对象。
     */
    const handleDownloadClick = (fileData) => {
        if (!globalApi || typeof globalApi.getEncryptedFileDownloadUrl !== 'function') {
            console.error('全局 fileManageApi 或其 getEncryptedFileDownloadUrl 方法未定义。');
            alert('下载功能初始化失败。');
            return;
        }
        if (!fileData || !fileData.sourceFilename) {
            alert('无法下载：文件信息不完整。');
            return;
        }

        // --- 关键修复：从包含文件名的完整相对路径中提取目录路径 ---
        let fullRelativePath = fileData.sourceRelativePath;
        const filename = fileData.sourceFilename;
        let directoryPath = ""; // 默认为根目录

        // 检查 fullRelativePath 是否存在且以 filename 结尾
        if (fullRelativePath && fullRelativePath.endsWith(filename)) {
            // 计算目录路径的结束位置（即文件名开始前的位置）
            const dirEndIndex = fullRelativePath.length - filename.length;

            // 如果文件名之前有内容，则认为是目录路径
            if (dirEndIndex > 0) {
                // 截取从开始到文件名之前的所有字符作为目录路径
                directoryPath = fullRelativePath.substring(0, dirEndIndex);
                console.warn(`检测到 sourceRelativePath ('${fullRelativePath}') 包含文件名。提取出的目录路径为: '${directoryPath}'`);
            } else {
                // 如果文件名之前没有内容，说明文件在根目录，路径为空字符串
                directoryPath = "";
            }
        } else {
            // 如果 fullRelativePath 不以 filename 结尾，这是一个不符合预期的场景。
            // 但为了健壮性，我们假设此时 fullRelativePath 就是目录路径。
            console.warn(`sourceRelativePath ('${fullRelativePath}') 与 sourceFilename ('${filename}') 格式不匹配。将直接使用 sourceRelativePath 作为路径。`);
            directoryPath = fullRelativePath;
        }
        // --- 修复结束 ---

        console.log(`请求下载文件: 修正后的相对路径='${directoryPath}', 文件名='${filename}'`);
        try {
            const downloadUrl = globalApi.getEncryptedFileDownloadUrl(directoryPath, filename);
            window.location.href = downloadUrl;
        } catch (error) {
            console.error('生成下载链接时出错:', error);
            alert(`生成下载链接失败: ${error.message || '未知错误'}`);
        }
    };

    const init = () => {
        console.log('--- [DEBUG] fileManageFulltextEvents.init() 被调用。');

        dom = window.fileManageFulltextDOM;
        ui = window.fileManageFulltextUI;
        service = window.fileManageFulltextService;
        config = window.fileManageFulltextConfig;
        globalApi = window.fileManageApi;

        const dependencies = { dom, ui, service, config, globalApi };
        for (const [name, moduleObj] of Object.entries(dependencies)) {
            if (!moduleObj) {
                console.error(`全文搜索模块初始化中止: 依赖模块 "${name}" 未在window上找到。`);
                return;
            }
        }

        dom.init();
        ui.initFlatpickr();
        ui.populateFileTypeFilters();

        if (dom.queryBtn) {
            dom.queryBtn.addEventListener('click', () => handleSearch(1));
        }
        if (dom.queryKeywordInput) {
            dom.queryKeywordInput.addEventListener('keypress', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    handleSearch(1);
                }
            });
            dom.queryKeywordInput.addEventListener('input', () => {
                ui.toggleClearIconVisibility(dom.queryKeywordInput.value.length > 0);
            });
        }
        if (dom.clearSearchIcon) {
            dom.clearSearchIcon.addEventListener('click', () => {
                if(dom.queryKeywordInput) {
                    dom.queryKeywordInput.value = '';
                    ui.toggleClearIconVisibility(false);
                    dom.queryKeywordInput.focus();
                }
            });
        }
        if (dom.fileTypeFilterContainer) {
            dom.fileTypeFilterContainer.addEventListener('change', (event) => {
                if (event.target.type === 'checkbox') {
                    handleSearch(1);
                }
            });
        }
        if (dom.clearFileTypeFilterBtn) {
            dom.clearFileTypeFilterBtn.addEventListener('click', () => {
                if (dom.fileTypeFilterContainer) {
                    const checkboxes = dom.fileTypeFilterContainer.querySelectorAll('input[type="checkbox"]');
                    checkboxes.forEach(cb => cb.checked = false);
                    handleSearch(1);
                }
            });
        }
        const dateInputs = [dom.dateFromInput, dom.dateToInput];
        dateInputs.forEach(input => {
            if (input) {
                input.addEventListener('blur', () => {
                    handleSearch(1);
                });
            }
        });
        if (dom.clearDateFilterBtn) {
            dom.clearDateFilterBtn.addEventListener('click', () => {
                if (dom.dateFromInput && dom.dateFromInput._flatpickr) dom.dateFromInput._flatpickr.clear();
                if (dom.dateToInput && dom.dateToInput._flatpickr) dom.dateToInput._flatpickr.clear();
                handleSearch(1);
            });
        }
        if (dom.sortOptionsSelect) {
            dom.sortOptionsSelect.addEventListener('change', () => handleSearch(1));
        }
        if (dom.resultsListContainer) {
            dom.resultsListContainer.addEventListener('click', (event) => {
                const targetLink = event.target.closest('a.source-filename-link');
                if (targetLink) {
                    event.preventDefault();
                    // 事件已在 renderResultsList 中单独绑定，此处的委托主要作为备用或用于其他可能的点击事件。
                }
            });
        }

        ui.updateResultsCount(0);
        ui.displayNoResults(true, '');
        ui.updatePaginationUI({ pageNumber: 1, totalPages: 0, totalElements: 0 }, handleSearch);
        ui.toggleClearIconVisibility(false);
        console.log('全文搜索模块 (fileManageFulltextEvents) 已成功初始化。');
    };

    return { init };
})();
