/**
 * 工时导入弹窗脚本
 * 文件路径: src/main/resources/static/js/timesheet_import.js
 * 职责：处理 Excel 文件上传、解析、预览、编辑、统计和提交导入。
 * 修正：使用 sheet_to_json, 延迟事件监听器绑定, 包含完整函数实现和日志。
 * 依赖: common.js, pagination.js (分页在此处不需要), SheetJS (xlsx.full.min.js)
 */

document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // 创建或获取模块命名空间
    const TimesheetImportModule = window.TimesheetImportModule || {};
    window.TimesheetImportModule = TimesheetImportModule; // 确保挂载到 window

    // --- 确保 SheetJS 库已加载 ---
    if (typeof XLSX === 'undefined') {
        console.error("SheetJS (XLSX) library not loaded. Excel import functionality will not work.");
        const modalArea = document.getElementById('import-modal');
        if (modalArea) {
            modalArea.innerHTML = '<div class="p-4 text-red-600 bg-red-100 border border-red-400 rounded text-center">错误：Excel 解析库 (SheetJS) 加载失败，请检查网络或联系管理员。导入功能无法使用。</div>';
        }
        return; // 停止执行
    } else {
        console.debug("SheetJS (XLSX) library loaded successfully.");
    }

    // --- 配置和常量 ---
    const API_BATCH_ENDPOINT = '/api/timesheets/batch';
    const MODAL_ID = 'import-modal';
    const CLOSE_BUTTON_ID = 'import-modal-close-button';
    const UPLOAD_BUTTON_ID = 'upload-excel-button';
    const FILE_INPUT_ID = 'hidden-file-input';
    const CLEAR_BUTTON_ID = 'clear-preview-button';
    const SUBMIT_BUTTON_ID = 'submit-data-button';
    const DROP_ZONE_ID = 'import-preview-area';
    const DRAG_DROP_HINT_ID = 'drag-drop-zone';
    const TABLE_BODY_ID = 'import-preview-tbody';
    const STATS_RECORDS_ID = 'stats-total-records';
    const STATS_DATE_MIN_ID = 'stats-date-min';
    const STATS_DATE_MAX_ID = 'stats-date-max';
    const STATS_HOURS_ID = 'stats-total-hours';
    const PARSE_ERROR_DISPLAY_ID = 'parse-error-display';
    const SUBMIT_STATUS_ID = 'submit-status';
    const INITIAL_TBODY_MSG_ID = 'initial-tbody-message';

    // Excel 列索引到 TimesheetWork 属性的映射 (0-based)
    const columnMapping = {
        0: 'tsId',
        1: 'tr',
        2: 'employee',
        3: 'dep',
        4: 'tsDep',
        5: 'tsStatus',
        6: 'tsYm',
        7: 'natureYm',
        8: 'tsDate',
        9: 'tsHours',
        10: 'tsMonth',
        11: 'projBm',
        12: 'tsBm',
        13: 'tsName',
        14: 'zone',
        15: 'sProjBm',
        16: 'sTsBm',
        17: 'tsComments'
    };
    // 必需列的索引 (根据实际业务调整)
    // tsId, tr, employee, dep, tsStatus, tsYm, natureYm, tsDate, tsHours, tsMonth, tsBm
    const requiredColumns = [0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 12];

    // --- DOM 元素引用 ---
    let modal = null;
    let tableBody = null;
    let initialTbodyMsg = null;
    let parseErrorDisplay = null;
    let statsRecords, statsDateMin, statsDateMax, statsHours;
    let submitButton, clearButton, uploadButton, fileInput, closeButton, dropZone, dragDropHint, submitStatus;

    // --- 状态变量 ---
    let parsedDataArray = []; // 存储所有已解析和验证通过的数据对象 [{...}, {...}]
    let isSubmitting = false; // 防止重复提交
    let listenersAttached = false; // 标记模态框内部监听器是否已附加

    // --- 函数定义 ---

    /** 打开弹窗 */
    function openModal() {
        console.debug("Opening import modal...");
        if (modal) {
            modal.classList.remove('hidden');
            // 在打开时才设置内部监听器 (如果尚未设置)
            if (!listenersAttached) {
                setupEventListeners(); // 尝试设置监听器
            }
        } else {
            console.error("Modal element not found, cannot open.");
        }
    }

    /** 关闭弹窗 */
    function closeModal() {
        console.debug("Closing import modal...");
        if (modal) {
            modal.classList.add('hidden');
        }
    }

    /** 清空预览数据和状态 */
    function clearPreviewData() {
        console.log("Clearing preview data...");
        parsedDataArray = [];
        if (tableBody) {
            tableBody.innerHTML = ''; // 清空表格
            // 重新获取或创建初始提示信息行
            const initialMsgRow = document.createElement('tr');
            initialMsgRow.id = INITIAL_TBODY_MSG_ID; // 重新设置 ID
            initialMsgRow.innerHTML = `<td colspan="20" class="text-center py-10 text-gray-400">请上传或拖拽文件</td>`;
            tableBody.appendChild(initialMsgRow);
        } else {
            console.warn("Table body not found during clearPreviewData.");
        }
        updateStats(); // 更新统计信息
        if (parseErrorDisplay) {
            parseErrorDisplay.textContent = '';
            parseErrorDisplay.classList.add('hidden');
        }
        if (submitStatus) submitStatus.textContent = '';
        if (submitButton) submitButton.disabled = true;
        if (dragDropHint) dragDropHint.classList.remove('hidden');
        if (fileInput) fileInput.value = null; // 重置文件输入
        console.log("Preview data cleared.");
    }

    /** 更新统计信息显示 */
    function updateStats() {
        console.debug("Updating statistics...");
        if (!statsRecords || !statsDateMin || !statsDateMax || !statsHours) {
            console.warn("Statistics elements not found, cannot update stats.");
            return;
        }

        const recordCount = parsedDataArray.length;
        let minDate = null;
        let maxDate = null;
        let totalHours = 0;

        parsedDataArray.forEach(item => {
            const hours = parseFloat(item.tsHours);  //此处换算成人天更容易检查。
            if (!isNaN(hours)) {
                totalHours += hours;
            }
            if (item.tsDate instanceof Date && !isNaN(item.tsDate)) {
                if (minDate === null || item.tsDate < minDate) minDate = item.tsDate;
                if (maxDate === null || item.tsDate > maxDate) maxDate = item.tsDate;
            }
        });

        statsRecords.textContent = recordCount;
        statsDateMin.textContent = minDate ? formatDateForDisplay(minDate) : 'N/A';
        statsDateMax.textContent = maxDate ? formatDateForDisplay(maxDate) : 'N/A';
        statsHours.textContent = (totalHours / 8).toFixed(1);

        if (submitButton) {
            submitButton.disabled = recordCount === 0 || isSubmitting;
        }
        console.debug("Stats updated:", {recordCount, minDate, maxDate, totalHours});
    }

    /** 格式化日期为YYYY-MM-DD (用于显示) */
    function formatDateForDisplay(date) {
        if (!(date instanceof Date) || isNaN(date)) return '';
        try {
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            return `${year}-${month}-${day}`;
        } catch (e) {
            console.error("Error formatting date:", date, e);
            return '无效日期';
        }
    }

    /** 解析 Excel 日期 (处理数字格式和常见字符串格式) */
    function parseExcelDate(excelDateValue) {
        if (excelDateValue == null) return null;
        if (excelDateValue instanceof Date) {
            return !isNaN(excelDateValue) ? excelDateValue : null;
        }
        if (typeof excelDateValue === 'number') {
            try {
                const dateInfo = XLSX.SSF.parse_date_code(excelDateValue);
                if (dateInfo) {
                    const date = new Date(dateInfo.y, dateInfo.m - 1, dateInfo.d, dateInfo.H || 0, dateInfo.M || 0, dateInfo.S || 0);
                    // 简单修复1900年Excel错误 (可选)
                    // if (dateInfo.y === 1900 && dateInfo.m === 1 && dateInfo.d === 29) { /* ... */ }
                    return !isNaN(date) ? date : null;
                }
            } catch (e) {
                console.warn("Error parsing Excel numeric date:", e);
            }
        }
        if (typeof excelDateValue === 'string') {
            try {
                // 尝试多种格式，优先 YYYY-MM-DD 或 YYYY/MM/DD
                const cleanedDateStr = excelDateValue.replace(/\//g, '-');
                // 检查是否符合 YYYY-MM-DD 格式
                if (/^\d{4}-\d{1,2}-\d{1,2}$/.test(cleanedDateStr)) {
                    let date = new Date(cleanedDateStr + 'T00:00:00'); // 添加时间避免时区问题
                    if (!isNaN(date)) return date;
                }
                // 可以添加更多格式的尝试...
            } catch (e) {
                console.warn("Error parsing date string:", e);
            }
        }
        return null;
    }

    /** 渲染预览表格 */
    function renderPreviewTable() {
        console.log("[renderPreviewTable] Function called.");
        console.log("[renderPreviewTable] tableBody reference:", tableBody);

        if (!tableBody) {
            console.error("[renderPreviewTable] Aborting: tableBody element not found.");
            return;
        }
        tableBody.innerHTML = ''; // 清空
        const initialMsg = document.getElementById(INITIAL_TBODY_MSG_ID); // 尝试获取初始消息行
        if (initialMsg) initialMsg.remove(); // 移除初始提示

        if (parsedDataArray.length === 0) {
            console.log("[renderPreviewTable] No data to render.");
            // 重新创建并添加初始提示信息行
            const initialMsgRow = document.createElement('tr');
            initialMsgRow.id = INITIAL_TBODY_MSG_ID;
            initialMsgRow.innerHTML = `<td colspan="20" class="text-center py-10 text-gray-400">暂无预览数据，请上传或拖拽文件</td>`;
            tableBody.appendChild(initialMsgRow);
            return;
        }

        console.log(`[renderPreviewTable] Rendering ${parsedDataArray.length} rows.`);

        parsedDataArray.forEach((item, index) => {
            console.log(`[renderPreviewTable] Rendering row index ${index}, Data keys:`, Object.keys(item || {}));
            try {
                const row = tableBody.insertRow();
                row.setAttribute('data-index', index);
                if (item._importStatus) {
                    row.classList.add(item._importStatus === 'success' ? 'import-success' : 'import-failed');
                }

                // 使用 innerHTML 设置整行内容，并为可编辑单元格添加属性
                let valueTsDate = item.tsDate instanceof Date ? formatDateForDisplay(item.tsDate) : (item.tsDate || '');
                let valueTsMonth = typeof item.tsMonth === 'number' ? item.tsMonth.toFixed(4) : (item.tsMonth || '');
                let valueTsHours = typeof item.tsHours === 'number' ? item.tsHours.toFixed(1) : (item.tsHours || '');

                // 确保所有 item.xxx 的访问都是安全的，即使属性不存在也不会报错（使用 || ''）
                row.innerHTML = `
                    <td class="table-cell px-2 py-2 text-center bg-gray-50">${index + 1}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsId">${item.tsId || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tr">${item.tr || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="employee">${item.employee || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="dep">${item.dep || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsDep">${item.tsDep || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsStatus">${item.tsStatus || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsYm">${item.tsYm || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="natureYm">${item.natureYm || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsDate">${valueTsDate}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsHours">${valueTsHours}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsMonth">${valueTsMonth}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="projBm">${item.projBm || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsBm">${item.tsBm || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsName">${item.tsName || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="zone">${item.zone || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="sProjBm">${item.sProjBm || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="sTsBm">${item.sTsBm || ''}</td>
                    <td class="table-cell px-2 py-2" contenteditable="true" data-key="tsComments">${item.tsComments || ''}</td>
                    <td class="table-cell px-2 py-2 import-status-cell ${item._importStatus === 'success' ? 'success' : ''}">${item._importMessage || '-'}</td>
                 `;
                // 重新为新创建的可编辑单元格附加事件监听器
                row.querySelectorAll('td[contenteditable="true"]').forEach(cell => {
                    cell.addEventListener('blur', handleCellEdit); // 保存编辑
                    cell.addEventListener('input', handleCellInput); // 标记编辑
                });

            } catch (e) {
                console.error(`[renderPreviewTable] Error rendering row index ${index}:`, e, "Data:", item);
                const errorRow = tableBody.insertRow();
                errorRow.innerHTML = `<td colspan="20" class="text-red-500 px-2 py-2">渲染第 ${index + 1} 行时出错: ${e.message}</td>`;
            }
        });
        console.log("[renderPreviewTable] Finished rendering rows.");
    }

    /** 处理单元格输入事件 (仅用于标记) */
    function handleCellInput(event) {
        const cell = event.target;
        if (cell.hasAttribute('contenteditable') && cell.tagName === 'TD') {
            cell.classList.add('cell-edited');
        }
    }

    /** 处理单元格编辑 (使用 blur 事件) */
    function handleCellEdit(event) {
        const cell = event.target;
        const row = cell.closest('tr');
        if (!row) return; // 如果找不到行，则退出
        const index = parseInt(row.getAttribute('data-index'), 10);
        const key = cell.getAttribute('data-key');
        let newValue = cell.textContent.trim(); // 获取编辑后的文本内容

        if (isNaN(index) || !key || index >= parsedDataArray.length) {
            console.error("无法更新数据：无效的行索引或列标识", {index, key});
            return;
        }

        const originalRecord = parsedDataArray[index];

        console.debug(`Cell edited: Row ${index}, Key ${key}, New text value: "${newValue}"`);

        // --- 数据类型转换和验证 ---
        let parsedValue = newValue;
        let validationError = null;

        if (key === 'tsDate') {
            const parsedDate = parseExcelDate(newValue);
            if (newValue !== '' && parsedDate === null) {
                validationError = "日期格式无效";
            } else {
                parsedValue = parsedDate; // 存储 Date 对象或 null
            }
        } else if (key === 'tsHours') {
            // parsedValue = newValue === '' ? null : parseInt(newValue, 10);
            parsedValue = newValue === '' ? null : parseFloat(newValue);
            if (newValue !== '' && (isNaN(parsedValue) || parsedValue < 0)) {
                validationError = "工时必须是非负数";
            }
        } else if (key === 'tsMonth') {
            parsedValue = newValue === '' ? null : parseFloat(newValue);
            if (newValue !== '' && (isNaN(parsedValue) || parsedValue < 0)) {
                validationError = "人月数必须是非负数字";
            }
        }
        // 添加其他字段的验证...
        // 检查必填项是否被改为空 (基于 requiredColumns)
        if (requiredColumns.map(i => columnMapping[i]).includes(key) && newValue === '') {
            validationError = `必需字段 "${key}" 不能为空`;
        }


        if (validationError) {
            console.warn(`Validation Error: ${validationError}`);
            AppUtils.showMessage(`${validationError}，请重新输入`, 'warning');
            // 恢复原始值或标记错误
            let originalValue = originalRecord[key];
            if (key === 'tsDate' && originalValue instanceof Date) {
                originalValue = formatDateForDisplay(originalValue);
            } else if (key === 'tsMonth' && typeof originalValue === 'number') {
                originalValue = originalValue.toFixed(4);
            } else if (key === 'tsHours' && typeof originalValue === 'number') {
                originalValue = originalValue.toFixed(1);
            }
            cell.textContent = (originalValue !== null && originalValue !== undefined) ? originalValue : '';
            cell.classList.add('border', 'border-red-500'); // 标记错误
            // 移除编辑标记，因为值无效
            cell.classList.remove('cell-edited');
            return;
        } else {
            cell.classList.remove('border', 'border-red-500');
        }


        // 更新数据数组
        console.log(`Updating parsedDataArray[${index}].${key} from`, originalRecord[key], "to", parsedValue);
        originalRecord[key] = parsedValue;

        // 标记为已编辑 (在 handleCellInput 中完成)
        // cell.classList.add('cell-edited');

        // 编辑后重新计算统计信息
        updateStats();
    }

    /** 处理文件 */
    function handleFiles(files) {
        if (!files || files.length === 0) return;
        console.log(`Handling ${files.length} file(s)...`);
        if (dragDropHint) dragDropHint.classList.add('hidden');
        if (initialTbodyMsg) initialTbodyMsg.remove();
        if (tableBody && tableBody.innerHTML === '') {
            tableBody.innerHTML = `<tr><td colspan="20" class="text-center py-10 text-gray-400"><div class="loader"></div> 正在解析文件...</td></tr>`;
        }
        if (parseErrorDisplay) parseErrorDisplay.classList.add('hidden');

        let fileParsePromises = [];
        let fileErrors = [];

        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            // 简单的文件类型检查
            if (!/\.(xlsx|xls|csv)$/i.test(file.name)) {
                console.warn(`Skipping file with unsupported extension: ${file.name}`);
                fileErrors.push({fileName: file.name, message: "不支持的文件类型，请上传 .xls, .xlsx 或 .csv 文件。"});
                continue; // 跳过不支持的文件
            }

            const reader = new FileReader();
            const promise = new Promise((resolve, reject) => {
                reader.onload = function (e) {
                    console.log(`File loaded: ${file.name}`);
                    const data = e.target.result;
                    try {
                        const workbook = XLSX.read(data, {type: 'array', cellDates: false, dateNF: 'yyyy-mm-dd'});
                        const firstSheetName = workbook.SheetNames[0];
                        const worksheet = workbook.Sheets[firstSheetName];
                        const sheetData = XLSX.utils.sheet_to_json(worksheet, {header: 1, defval: null});
                        console.log(`Parsing sheet data for ${file.name}, ${sheetData.length} rows found.`);
                        const parsedResult = parseSheetData(sheetData, file.name);
                        if (parsedResult.success) {
                            resolve(parsedResult.data);
                        } else {
                            reject({fileName: file.name, message: parsedResult.message});
                        }
                    } catch (parseError) {
                        console.error(`Error parsing Excel file ${file.name}:`, parseError);
                        if (parseError instanceof TypeError && parseError.message.includes("is not a function")) {
                            reject({
                                fileName: file.name,
                                message: `文件解析库错误: ${parseError.message}。请确认xlsx.full.min.js已正确加载。`
                            });
                        } else {
                            reject({fileName: file.name, message: `文件解析失败: ${parseError.message}`});
                        }
                    }
                };
                reader.onerror = function (e) {
                    console.error(`Error reading file ${file.name}:`, e);
                    reject({fileName: file.name, message: "文件读取错误"});
                };
                reader.readAsArrayBuffer(file);
            });
            fileParsePromises.push(promise);
        }

        // 如果没有有效的文件 Promise，则提前结束
        if (fileParsePromises.length === 0) {
            console.log("No valid files to process.");
            if (tableBody && tableBody.innerHTML.includes('loader')) { // 如果只有加载提示，则恢复初始消息
                clearPreviewData(); // 使用 clear 来恢复初始状态
            }
            if (fileErrors.length > 0) {
                displayParseErrors(fileErrors);
            }
            return;
        }


        Promise.allSettled(fileParsePromises)
            .then(results => {
                let newData = [];
                results.forEach(result => {
                    if (result.status === 'fulfilled') {
                        newData = newData.concat(result.value);
                    } else {
                        // 已经收集的错误信息优先显示
                        if (!fileErrors.some(err => err.fileName === result.reason.fileName)) {
                            fileErrors.push(result.reason);
                        }
                    }
                });
                parsedDataArray = parsedDataArray.concat(newData);
                if (fileErrors.length > 0) {
                    displayParseErrors(fileErrors);
                } else {
                    if (parseErrorDisplay) parseErrorDisplay.classList.add('hidden');
                }

                console.log(`[handleFiles] Preparing to render. Parsed data count: ${parsedDataArray.length}`);
                if (parsedDataArray.length > 0) {
                    console.log("[handleFiles] First few records:", JSON.stringify(parsedDataArray.slice(0, 3), null, 2));
                }
                console.log("[handleFiles] Calling renderPreviewTable()...");
                renderPreviewTable();
                console.log("[handleFiles] Calling updateStats()...");
                updateStats();
                console.log(`File handling complete. Total parsed records: ${parsedDataArray.length}. Errors: ${fileErrors.length}`);
            });
    }

    /** 解析单个工作表的数据 */
    function parseSheetData(sheetData, fileName) {
        const dataFromFile = [];
        let firstError = null;
        if (!sheetData || sheetData.length < 2) {
            return {success: false, data: [], message: `文件 "${fileName}" 为空或缺少数据行。`};
        }
        console.log(`[parseSheetData] Starting parse for ${fileName}`);

        for (let i = 1; i < sheetData.length; i++) { // 从第二行开始
            const row = sheetData[i];
            const record = {};
            let rowHasError = false;

            if (!row || row.every(cell => cell === null || String(cell).trim() === '')) {
                console.debug(`[parseSheetData] Skipping empty row ${i + 1} in file ${fileName}`);
                continue;
            }
            console.debug(`[parseSheetData] Processing row ${i + 1}:`, row);

            for (const colIndex in columnMapping) {
                const key = columnMapping[colIndex];
                const rawValue = row[colIndex];
                let value = (rawValue !== null && rawValue !== undefined) ? String(rawValue).trim() : null;

                // --- 验证和转换 ---
                if (requiredColumns.includes(parseInt(colIndex, 10)) && (value === null || value === '')) {
                    rowHasError = true;
                    firstError = firstError || `文件 "${fileName}" 第 ${i + 1} 行: 必需列 "${key}" (第 ${parseInt(colIndex, 10) + 1} 列) 不能为空。`;
                    console.warn(firstError);
                    break;
                }
                if (key === 'tsDate') {
                    const parsedDate = parseExcelDate(rawValue);
                    if (value !== null && parsedDate === null) {
                        rowHasError = true;
                        firstError = firstError || `文件 "${fileName}" 第 ${i + 1} 行: "工时日期"列 "${key}" 格式无效 ("${rawValue}")。请使用 YYYY-MM-DD 或标准格式。`;
                        console.warn(firstError);
                        break;
                    }
                    value = parsedDate;
                } else if (key === 'tsHours') {
                    value = value === null ? null : parseFloat(value);
                    if (value !== null && (isNaN(value) || value < 0)) {
                        rowHasError = true;
                        firstError = firstError || `文件 "${fileName}" 第 ${i + 1} 行: "工时小时数"数列 "${key}" 必须是非负数。`;
                        console.warn(firstError);
                        break;
                    }
                } else if (key === 'tsMonth') {
                    value = value === null ? null : parseFloat(value);
                    if (value !== null && (isNaN(value) || value < 0)) {
                        rowHasError = true;
                        firstError = firstError || `文件 "${fileName}" 第 ${i + 1} 行: “人月数”列 "${key}" 必须是非负数字。`;
                        console.warn(firstError);
                        break;
                    }
                }
                record[key] = value;
            } // 结束列循环

            if (rowHasError) {
                console.warn(`Parsing stopped for file "${fileName}" due to error at row ${i + 1}: ${firstError}`);
                return {success: false, data: [], message: firstError};
            }
            dataFromFile.push(record);
        } // 结束行循环
        console.log(`[parseSheetData] Successfully parsed ${dataFromFile.length} records from ${fileName}`);
        return {success: true, data: dataFromFile};
    }

    /** 显示解析错误 */
    function displayParseErrors(errors) {
        if (!parseErrorDisplay) return;
        let errorHtml = '<strong class="block mb-1 text-red-700">文件处理过程中遇到以下错误 (已跳过包含错误的整个文件):</strong><ul class="list-disc list-inside text-sm text-red-600">';
        errors.forEach(err => {
            errorHtml += `<li class="ml-2">${err.fileName ? `<span class="font-medium">[${err.fileName}]</span> ` : ''}${err.message}</li>`;
        });
        errorHtml += '</ul>';
        parseErrorDisplay.innerHTML = errorHtml;
        parseErrorDisplay.classList.remove('hidden');
        console.warn("Parse errors displayed:", errors);
    }

    /** 处理提交数据 */
    async function handleSubmitData() {
        console.log("[handleSubmitData] Submit button clicked.");
        if (isSubmitting || parsedDataArray.length === 0) {
            console.warn("提交被阻止：正在提交或无数据可提交。");
            return;
        }

        isSubmitting = true;
        if (submitButton) submitButton.disabled = true;
        if (clearButton) clearButton.disabled = true;
        if (uploadButton) uploadButton.disabled = true;
        if (submitStatus) submitStatus.textContent = `正在提交 ${parsedDataArray.length} 条数据...`;
        console.log("Submitting data (first few):", JSON.stringify(parsedDataArray.slice(0, 3), null, 2));

        // 转换日期为 YYYY-MM-DD 字符串，如果后端需要
        const dataToSend = parsedDataArray.map(item => ({
            ...item,
            tsDate: item.tsDate instanceof Date ? formatDateForDisplay(item.tsDate) : null
            // 确保其他字段也是后端期望的类型
        }));

        // 清除旧的导入状态信息
        parsedDataArray.forEach(item => {
            delete item._importStatus;
            delete item._importMessage;
        });
        tableBody.querySelectorAll('tr').forEach(row => {
            row.classList.remove('import-failed', 'import-success-removing');
            const statusCell = row.cells[row.cells.length - 1];
            if (statusCell) statusCell.textContent = '-';
            statusCell.classList.remove('success');
        });


        try {
            const results = await AppUtils.post(API_BATCH_ENDPOINT, dataToSend);
            console.log("[handleSubmitData] Batch submission response:", results);
            AppUtils.showMessage('数据提交处理完成！', 'info');
            handleSubmissionResults(results); // 处理结果
        } catch (error) {
            console.error("提交数据失败:", error);
            AppUtils.showMessage(`提交失败: ${error.message || '请检查网络或联系管理员'}`, 'error');
            if (submitStatus) submitStatus.textContent = `提交出错: ${error.message || ''}`;
        } finally {
            isSubmitting = false;
            // 根据是否还有未成功的数据决定按钮状态
            submitButton.disabled = parsedDataArray.length === 0;
            if (clearButton) clearButton.disabled = false;
            if (uploadButton) uploadButton.disabled = false;
            // submitStatus 的最终状态在 handleSubmissionResults 中设置可能更好
            if (submitStatus && submitStatus.textContent.startsWith('正在提交')) {
                submitStatus.textContent = '处理完成，请查看表格状态。';
            }
            setTimeout(() => {
                if (submitStatus) submitStatus.textContent = '';
            }, 8000);
        }
    }

    /** 处理后端返回的批量提交结果 */
    function handleSubmissionResults(results) {
        if (!results || !Array.isArray(results)) {
            console.error("无效的提交结果格式:", results);
            AppUtils.showMessage("处理提交结果时出错", "error");
            if (submitStatus) submitStatus.textContent = '处理结果格式错误';
            return;
        }

        let successCount = 0;
        let failedCount = 0;
        const failedRecords = []; // 存储导入失败的原始记录
        const rowsToRemove = []; // 存储导入成功的行元素

        results.forEach(result => {
            const originalIndex = result.originalIndex;
            if (originalIndex === undefined || originalIndex < 0 || originalIndex >= parsedDataArray.length) {
                console.warn("Received result with invalid originalIndex:", result);
                failedCount++; // 将无效索引视为失败
                return;
            }

            const originalData = parsedDataArray[originalIndex];
            const row = tableBody ? tableBody.querySelector(`tr[data-index="${originalIndex}"]`) : null;

            // 更新原始数据的状态信息
            originalData._importStatus = result.success ? 'success' : 'failed';
            originalData._importMessage = result.message || (result.success ? '成功' : '失败');

            if (row) {
                const statusCell = row.cells[row.cells.length - 1];
                if (statusCell) {
                    statusCell.textContent = originalData._importMessage;
                    statusCell.classList.toggle('success', result.success);
                }

                if (result.success) {
                    successCount++;
                    row.classList.add('import-success-removing');
                    rowsToRemove.push(row);
                } else {
                    failedCount++;
                    row.classList.add('import-failed');
                    failedRecords.push(originalData); // 保留失败的记录到新数组
                }
            } else {
                console.warn(`Cannot find table row for result index ${originalIndex}`);
                if (!result.success) {
                    failedCount++;
                    failedRecords.push(originalData); // 即使行找不到，失败的记录也要保留
                } else {
                    successCount++; // 假设成功了，但无法移除行
                }
            }
        });

        // 动画结束后移除成功的行
        if (rowsToRemove.length > 0) {
            setTimeout(() => {
                rowsToRemove.forEach(row => row.remove());
                // 更新数据数组，只保留失败的记录
                parsedDataArray = failedRecords;
                // 重新渲染失败的记录，并更新它们的 data-index (重要！)
                renderPreviewTable(); // 重新渲染以更新索引和显示
                updateStats(); // 更新统计
                console.log(`Removed ${rowsToRemove.length} successful rows. ${failedRecords.length} rows failed and remain.`);
                if (submitStatus) submitStatus.textContent = `处理完成: ${successCount} 成功, ${failedCount} 失败。`;
            }, 500); // 匹配 CSS 动画时间
        } else {
            // 如果没有成功的行，直接更新数据数组和统计
            parsedDataArray = failedRecords;
            renderPreviewTable(); // 仍然需要重新渲染以显示错误状态
            updateStats();
            console.log(`No rows removed. ${failedRecords.length} rows failed.`);
            if (submitStatus) submitStatus.textContent = `处理完成: ${successCount} 成功, ${failedCount} 失败。`;
        }
        // 更新提交按钮状态
        if (submitButton) submitButton.disabled = parsedDataArray.length === 0;
    }


    /** 设置模态框内部元素的事件监听器 */
    function setupEventListeners() {
        console.log("[setupEventListeners] Attempting to attach listeners...");

        // 获取模态框内部元素引用
        closeButton = document.getElementById(CLOSE_BUTTON_ID);
        uploadButton = document.getElementById(UPLOAD_BUTTON_ID);
        fileInput = document.getElementById(FILE_INPUT_ID);
        clearButton = document.getElementById(CLEAR_BUTTON_ID);
        submitButton = document.getElementById(SUBMIT_BUTTON_ID);
        dropZone = document.getElementById(DROP_ZONE_ID);
        dragDropHint = document.getElementById(DRAG_DROP_HINT_ID);
        // tableBody 已在 init 中获取 (需要再次确认)
        if (!tableBody) tableBody = document.getElementById(TABLE_BODY_ID);
        statsRecords = document.getElementById(STATS_RECORDS_ID);
        statsDateMin = document.getElementById(STATS_DATE_MIN_ID);
        statsDateMax = document.getElementById(STATS_DATE_MAX_ID);
        statsHours = document.getElementById(STATS_HOURS_ID);
        parseErrorDisplay = document.getElementById(PARSE_ERROR_DISPLAY_ID);
        submitStatus = document.getElementById(SUBMIT_STATUS_ID);

        // 再次检查关键元素是否存在
        if (!closeButton || !uploadButton || !fileInput || !clearButton || !submitButton || !dropZone || !tableBody || !statsRecords) {
            console.error("Modal elements not found inside setupEventListeners, cannot attach listeners.");
            if (modal) modal.querySelector('.relative.mx-auto').insertAdjacentHTML('afterbegin', '<p class="text-red-500 p-2">错误：无法初始化模态框内部控件。</p>');
            return; // 阻止继续执行和设置 listenersAttached = true
        }

        console.log("[setupEventListeners] All required modal elements found.");

        // 关闭按钮
        closeButton.addEventListener('click', closeModal);
        console.log("[setupEventListeners] Close button listener attached.");

        // 点击“上传文件”按钮触发隐藏的文件输入框
        uploadButton.addEventListener('click', () => {
            console.log("[setupEventListeners] Upload button clicked, triggering file input.");
            fileInput.click();
        });
        console.log("[setupEventListeners] Upload button listener attached.");

        // 文件输入框选择文件后处理
        fileInput.addEventListener('change', (event) => {
            console.log("[setupEventListeners] File input changed.");
            handleFiles(event.target.files);
            // 重置文件输入框以便可以再次选择相同的文件
            event.target.value = null;
        });
        console.log("[setupEventListeners] File input listener attached.");

        // 清空数据按钮
        clearButton.addEventListener('click', clearPreviewData);
        console.log("[setupEventListeners] Clear button listener attached.");

        // 提交数据按钮
        submitButton.addEventListener('click', handleSubmitData);
        console.log("[setupEventListeners] Submit button listener attached.");

        // --- 拖拽事件 ---
        dropZone.addEventListener('dragover', (event) => {
            event.preventDefault();
            dropZone.classList.add('drag-over');
        });
        dropZone.addEventListener('dragleave', (event) => {
            // 检查离开的目标是否仍在 dropZone 内部，防止闪烁
            if (!dropZone.contains(event.relatedTarget)) {
                dropZone.classList.remove('drag-over');
            }
        });
        dropZone.addEventListener('drop', (event) => {
            event.preventDefault();
            dropZone.classList.remove('drag-over');
            const files = event.dataTransfer.files;
            console.log("[setupEventListeners] Files dropped.");
            handleFiles(files);
        });
        console.log("[setupEventListeners] Drag and drop listeners attached.");

        // --- 表格内编辑事件监听器 ---
        // 注意：事件委托目标是 tableBody
        tableBody.addEventListener('blur', (event) => {
            if (event.target.hasAttribute('contenteditable') && event.target.tagName === 'TD') {
                handleCellEdit(event);
            }
        }, true); // 使用捕获阶段
        tableBody.addEventListener('input', (event) => {
            if (event.target.hasAttribute('contenteditable') && event.target.tagName === 'TD') {
                handleCellInput(event);
            }
        });
        console.log("[setupEventListeners] Table edit listeners attached.");


        listenersAttached = true; // 标记监听器已成功附加
        console.log("Import modal internal event listeners setup complete.");
    }

    /** 初始化模块 */
    function init() {
        console.log("Initializing Timesheet Import Module...");
        modal = document.getElementById(MODAL_ID);
        tableBody = document.getElementById(TABLE_BODY_ID);
        initialTbodyMsg = document.getElementById(INITIAL_TBODY_MSG_ID);

        if (!modal) {
            console.error("Timesheet Import Module initialization failed: Modal container not found.");
            return;
        }
        if (!tableBody) {
            console.warn("Timesheet Import Module initialization warning: Table body not found initially.");
        }

        const openModalButton = document.getElementById('open-import-modal-button');
        if (openModalButton) {
            openModalButton.addEventListener('click', openModal);
            console.log("Listener attached to open modal button.");
        } else {
            console.warn("Open Import Modal button ('#open-import-modal-button') not found.");
        }
        console.log("Timesheet Import Module Initialized (listeners deferred).");
    }

    // --- 执行初始化 ---
    if (!window.AppUtils) {
        console.error("common.js (AppUtils) not loaded. Timesheet Import Module cannot initialize properly.");
        return;
    }
    init();

}); // 结束 DOMContentLoaded


// * **说明:**
//     * **结构:** 使用 IIFE 封装，定义常量、DOM 引用、状态变量和函数。
//     * **核心逻辑:**
//         * `init`: 获取 DOM 引用，设置事件监听器（包括主页面上的“导入数据”按钮，假设其 ID 为 `open-import-modal-button`）。
//         * `openModal`/`closeModal`: 控制弹窗显示。
//         * `clearPreviewData`: 清空预览区。
//         * `handleFiles`: 处理文件选择或拖拽，调用 `FileReader` 和 SheetJS。
//         * `parseSheetData`: 解析单个工作表数据（Array of Arrays），进行列映射和行级验证（包括日期 `parseExcelDate` 和数字）。如果行错误则停止处理当前文件并返回错误。
//         * `parseExcelDate`: 尝试将 Excel 日期（数字或字符串）转为 JS Date 对象。
//         * `renderPreviewTable`: 将 `parsedDataArray` 渲染到预览表格，添加行号，设置单元格可编辑 (`contenteditable`)，并附加编辑事件监听。
//         * `handleCellEdit`: 处理单元格编辑完成（`blur`）事件，进行数据验证、更新 `parsedDataArray` 并标记单元格。
//         * `updateStats`: 根据 `parsedDataArray` 计算并更新统计信息。
//         * `handleSubmitData`: 处理“提交数据”按钮点击，调用后端 `/api/timesheets/batch` API。
//         * `handleSubmissionResults`: 处理后端返回的批量导入结果，更新表格行的状态（移除成功行，标记失败行及原因）。
//     * **依赖:** 依赖 `common.js` (AppUtils) 和 `xlsx.full.min.js` (SheetJS)。
//     * **错误处理:** 包含了文件读取、解析、验证和提交过程中的基本错误处理和用户提示。
//     * **编辑:** 使用了 `contenteditable` 实现简单的行内编辑，并在 `blur` 事件时更新
