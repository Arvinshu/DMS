/**
 * 目录结构: src/main/java/org/ls/service/FulltextSearchService.java
 * 文件名称: FulltextSearchService.java
 * 开发时间: 2025-06-03 22:00:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 定义全文搜索服务的接口。
 * 包含一个核心的搜索方法，用于根据请求参数从Elasticsearch中检索数据并返回分页结果。
 */
package org.ls.service;

import org.ls.dto.FulltextSearchRequestDto;
import org.ls.dto.FulltextSearchResultDto;
import org.ls.dto.PageDto; // 假设 PageDto 是一个通用的分页结果封装类

public interface FulltextSearchService {

    /**
     * 执行全文搜索。
     *
     * @param requestDto 包含搜索关键词、分页信息、筛选条件和排序方式的请求对象。
     * @return 返回一个 PageDto 对象，其中包含 FulltextSearchResultDto 列表和分页信息。
     * 如果搜索过程中发生错误，具体行为取决于实现类（例如，可能抛出自定义异常或返回空的PageDto）。
     */
    PageDto<FulltextSearchResultDto> search(FulltextSearchRequestDto requestDto);

}
