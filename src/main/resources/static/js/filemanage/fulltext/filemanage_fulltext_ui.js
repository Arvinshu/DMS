/**
 * 目录结构: src/main/resources/static/js/filemanage/fulltext/filemanage_fulltext_ui.js
 * 文件名称: filemanage_fulltext_ui.js
 * 开发时间: 2025-06-04 11:00:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 专注于全文搜索模块的UI渲染和更新。
 * 本次更新: 将模块的IIFE返回值明确附加到 window 对象。
 */

console.log('--- [DEBUG] filemanage_fulltext_ui.js: 开始执行...');

// **关键修复**: 将IIFE的返回值赋值给 window 上的属性
window.fileManageFulltextUI = (() => {

    const dom = window.fileManageFulltextDOM;
    const config = window.fileManageFulltextConfig;

    const formatFileSize = (bytes) => {
        if (bytes < 0 || bytes === null || bytes === undefined) return 'N/A';
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = (bytes > 0) ? Math.floor(Math.log(bytes) / Math.log(k)) : 0;
        const index = Math.min(i, sizes.length - 1);
        return parseFloat((bytes / Math.pow(k, index)).toFixed(1)) + ' ' + sizes[index];
    };

    const getFileTypeIconClass = (filename) => {
        if (!config || !config.FILE_TYPE_ICONS) return 'far fa-file'; // 防御性编程
        if (!filename || typeof filename !== 'string') {
            return config.FILE_TYPE_ICONS['default'];
        }
        const extension = filename.slice(filename.lastIndexOf('.') + 1).toLowerCase();
        return config.FILE_TYPE_ICONS[extension] || config.FILE_TYPE_ICONS['default'];
    };

    const initFlatpickr = () => {
        if (typeof flatpickr === 'undefined') {
            console.warn('Flatpickr库未加载，日期选择器无法初始化。');
            return;
        }
        if (dom && dom.dateFromInput && dom.dateToInput) {
            const commonConfig = (config && config.FLATPICKR_DEFAULT_CONFIG) ? { ...config.FLATPICKR_DEFAULT_CONFIG } : {};
            try {
                flatpickr(dom.dateFromInput, {
                    ...commonConfig,
                    altInput: true, altFormat: "Y年m月d日", dateFormat: "Y-m-d",
                    onChange: function(selectedDates, dateStr, instance) {
                        if (dom.dateToInput._flatpickr && selectedDates[0]) {
                            dom.dateToInput._flatpickr.set('minDate', selectedDates[0]);
                        }
                    }
                });
                flatpickr(dom.dateToInput, {
                    ...commonConfig,
                    altInput: true, altFormat: "Y年m月d日", dateFormat: "Y-m-d",
                    onChange: function(selectedDates, dateStr, instance) {
                        if (dom.dateFromInput._flatpickr && selectedDates[0]) {
                            dom.dateFromInput._flatpickr.set('maxDate', selectedDates[0]);
                        }
                    }
                });
                console.log('Flatpickr日期选择器已初始化。');
            } catch (e) {
                console.error('初始化Flatpickr时出错:', e);
            }
        } else {
            console.warn('日期输入框DOM元素未找到，无法初始化Flatpickr。');
        }
    };


    const renderResultsList = (resultsData, downloadCallback) => {
        if (!dom || !dom.resultsListContainer) {
            console.error('结果列表容器DOM元素未找到。');
            return;
        }
        dom.resultsListContainer.innerHTML = '';

        if (!resultsData || resultsData.length === 0) {
            return;
        }

        const fragment = document.createDocumentFragment();
        resultsData.forEach(item => {
            const resultItemDiv = document.createElement('div');
            resultItemDiv.className = 'fulltext-result-item';
            const paragraphP = document.createElement('p');
            paragraphP.className = 'matched-paragraph mb-2 text-gray-700 text-sm';
            paragraphP.innerHTML = item.matchedParagraph || '无内容摘要。';
            const filenameContainerDiv = document.createElement('div');
            filenameContainerDiv.className = 'source-filename-container text-base';
            const iconI = document.createElement('i');
            iconI.className = getFileTypeIconClass(item.sourceFilename);
            filenameContainerDiv.appendChild(iconI);
            const filenameLinkA = document.createElement('a');
            filenameLinkA.href = '#';
            filenameLinkA.className = 'source-filename-link hover:underline';
            filenameLinkA.textContent = item.sourceFilename || '未知文件名';
            filenameLinkA.dataset.relativePath = item.sourceRelativePath;
            filenameLinkA.dataset.filename = item.sourceFilename;
            if (typeof downloadCallback === 'function') {
                filenameLinkA.addEventListener('click', (e) => {
                    e.preventDefault();
                    downloadCallback({
                        sourceRelativePath: item.sourceRelativePath,
                        sourceFilename: item.sourceFilename
                    });
                });
            }
            filenameContainerDiv.appendChild(filenameLinkA);
            const metadataDiv = document.createElement('div');
            metadataDiv.className = 'file-metadata-info mt-1 text-xs';
            const modDateSpan = document.createElement('span');
            modDateSpan.innerHTML = `<i class="far fa-calendar-alt me-1"></i>修改日期: ${item.lastModifiedDate || 'N/A'}`;
            metadataDiv.appendChild(modDateSpan);
            const sizeSpan = document.createElement('span');
            sizeSpan.innerHTML = `<i class="fas fa-file-invoice-dollar me-1"></i>大小: ${formatFileSize(item.fileSize)}`;
            metadataDiv.appendChild(sizeSpan);
            const pathSpan = document.createElement('span');
            pathSpan.innerHTML = `<i class="far fa-folder me-1"></i>路径: ${item.sourceRelativePath || 'N/A'}`;
            pathSpan.title = item.sourceRelativePath || 'N/A';
            metadataDiv.appendChild(pathSpan);
            if (item.score !== null && item.score !== undefined) {
                const scoreSpan = document.createElement('span');
                scoreSpan.innerHTML = `<i class="fas fa-star-half-alt me-1"></i>相关度: ${item.score.toFixed(2)}`;
                metadataDiv.appendChild(scoreSpan);
            }
            resultItemDiv.appendChild(filenameContainerDiv);
            resultItemDiv.appendChild(paragraphP);
            resultItemDiv.appendChild(metadataDiv);
            fragment.appendChild(resultItemDiv);
        });
        dom.resultsListContainer.appendChild(fragment);
    };

    const updatePaginationUI = (pageDto, onPageChangeCallback) => {
        if (!dom || !dom.paginationContainer) {
            console.error('分页容器DOM元素未找到。');
            return;
        }
        if (typeof AppUtils !== 'undefined' && typeof AppUtils.setupPagination === 'function') {
            AppUtils.setupPagination({
                containerId: dom.paginationContainer.id,
                currentPage: pageDto.pageNumber,
                totalPages: pageDto.totalPages,
                totalRecords: pageDto.totalElements,
                onPageChange: onPageChangeCallback
            });
        } else {
            console.warn('AppUtils.setupPagination 未定义，无法更新分页。');
            dom.paginationContainer.innerHTML = `第 ${pageDto.pageNumber} 页 / 共 ${pageDto.totalPages} 页 (总计 ${pageDto.totalElements} 条)`;
        }
    };

    const displayLoading = (isLoading) => {
        if (!dom || !dom.resultsListContainer || !dom.queryBtn) return;
        if (isLoading) {
            dom.resultsListContainer.innerHTML = '<div class="text-center text-muted p-5"><i class="fas fa-spinner fa-spin fa-2x"></i><p class="mt-2">正在努力搜索中...</p></div>';
            dom.queryBtn.disabled = true;
            dom.queryBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 搜索中';
        } else {
            dom.queryBtn.disabled = false;
            dom.queryBtn.innerHTML = '搜索';
        }
    };

    const displayNoResults = (show, keyword) => {
        if (!dom || !dom.resultsListContainer) return;
        if (show) {
            let message = '未能检索到相关内容。';
            if (keyword) {
                message = `未能检索到与“<strong>${escapeHtml(keyword)}</strong>”相关的内容。请尝试其他关键词。`;
            }
            dom.resultsListContainer.innerHTML = `<div class="text-center text-muted p-5"><i class="fas fa-meh fa-2x"></i><p class="mt-2">${message}</p></div>`;
        }
    };

    const displayError = (message) => {
        if (!dom || !dom.resultsListContainer) return;
        dom.resultsListContainer.innerHTML = `<div class="alert alert-danger text-center p-5" role="alert"><i class="fas fa-exclamation-triangle fa-2x"></i><p class="mt-2">搜索出错：${escapeHtml(message)}</p></div>`;
    };

    const updateResultsCount = (totalElements) => {
        if (dom && dom.resultsCountSpan) {
            dom.resultsCountSpan.textContent = `找到约 ${totalElements} 条结果`;
        }
    };

    const toggleClearIconVisibility = (show) => {
        if (dom && dom.clearSearchIcon) {
            dom.clearSearchIcon.style.display = show ? 'inline' : 'none';
        }
    };

    const populateFileTypeFilters = () => {
        if (!dom || !dom.fileTypeFilterContainer || !config || !config.SUPPORTED_FILE_TYPES) {
            console.warn('文件类型筛选容器或配置未找到，无法生成筛选选项。');
            return;
        }
        dom.fileTypeFilterContainer.innerHTML = '';
        const fragment = document.createDocumentFragment();
        config.SUPPORTED_FILE_TYPES.forEach(fileType => {
            const span = document.createElement('span');
            span.className = 'checkbox-item';
            const input = document.createElement('input');
            input.type = 'checkbox';
            input.id = fileType.id;
            input.value = fileType.value;
            input.className = 'form-check-input';
            const label = document.createElement('label');
            label.htmlFor = fileType.id;
            label.textContent = fileType.label;
            label.className = 'ms-1 form-check-label';
            span.appendChild(input);
            span.appendChild(label);
            fragment.appendChild(span);
        });
        dom.fileTypeFilterContainer.appendChild(fragment);
    };

    const escapeHtml = (str) => {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    };

    return {
        initFlatpickr,
        populateFileTypeFilters,
        renderResultsList,
        updatePaginationUI,
        displayLoading,
        displayNoResults,
        displayError,
        updateResultsCount,
        toggleClearIconVisibility
    };

})();

console.log('--- [DEBUG] filemanage_fulltext_ui.js: 执行完毕，fileManageFulltextUI 对象已创建。');
