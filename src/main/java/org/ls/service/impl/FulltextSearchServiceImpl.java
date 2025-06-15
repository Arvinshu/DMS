/**
 * 目录结构: src/main/java/org/ls/service/impl/FulltextSearchServiceImpl.java
 * 文件名称: FulltextSearchServiceImpl.java
 * 开发时间: 2025-06-03 22:15:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: FulltextSearchService 接口的实现类。
 * 本次更新: 使用经过验证的正确代码更新了日期范围筛选的逻辑。
 */
package org.ls.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import org.ls.config.properties.ElasticsearchProperties;
import org.ls.dto.FulltextSearchRequestDto;
import org.ls.dto.FulltextSearchResultDto;
import org.ls.dto.PageDto;
import org.ls.service.FulltextSearchService;
import org.ls.utils.DateUtils;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FulltextSearchServiceImpl implements FulltextSearchService {

    private static final Logger log = LoggerFactory.getLogger(FulltextSearchServiceImpl.class);

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchProperties elasticsearchProperties;

    // --- 关键修复：更新所有常量以匹配 Elasticsearch 索引中实际的字段名 ---
    private static final String ES_FIELD_FILENAME = "filename"; // 修正
    private static final String ES_FIELD_RELATIVE_PATH = "source_path"; // 修正
    private static final String ES_FIELD_LAST_MODIFIED = "last_modified"; // 修正
    private static final String ES_FIELD_FILE_SIZE = "file_size_bytes"; // 修正
    private static final String ES_FIELD_CONTENT = "content"; // 假设这个是正确的
    private static final String ES_FIELD_TITLE = "title";     // 假设这个是正确的

    @Autowired
    public FulltextSearchServiceImpl(ElasticsearchClient elasticsearchClient,
                                     ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchClient = elasticsearchClient;
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Override
    public PageDto<FulltextSearchResultDto> search(FulltextSearchRequestDto requestDto) {
        if (requestDto == null) {
            log.warn("全文搜索请求 DTO 为空。");
            return new PageDto<>(new ArrayList<>(), 1, 0, 0L);
        }

        String indexName = elasticsearchProperties.getIndexName();
        if (StringUtils.isBlank(indexName)) {
            log.error("Elasticsearch 索引名称未配置。");
            return new PageDto<>(new ArrayList<>(), requestDto.getPage(), requestDto.getSize(), 0L);
        }

        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(indexName);

        buildQuery(searchRequestBuilder, requestDto.getQuery(), requestDto.getFilters());

        int from = (requestDto.getPage() - 1) * requestDto.getSize();
        searchRequestBuilder.from(from).size(requestDto.getSize());

        buildSort(searchRequestBuilder, requestDto.getSortBy());

        buildHighlight(searchRequestBuilder, requestDto.getQuery());

        SearchRequest esSearchRequest = searchRequestBuilder.build();
        log.debug("执行 Elasticsearch 查询: {}", esSearchRequest.toString());

        try {
            SearchResponse<Map> response = elasticsearchClient.search(esSearchRequest, Map.class);
            return parseSearchResponse(response, requestDto.getPage(), requestDto.getSize());
        } catch (IOException e) {
            log.error("执行 Elasticsearch 搜索时发生 IO 异常: {}", e.getMessage(), e);
            return new PageDto<>(new ArrayList<>(), requestDto.getPage(), requestDto.getSize(), 0L);
        } catch (Exception e) {
            log.error("执行 Elasticsearch 搜索时发生未知异常: {}", e.getMessage(), e);
            return new PageDto<>(new ArrayList<>(), requestDto.getPage(), requestDto.getSize(), 0L);
        }
    }

    private void buildQuery(SearchRequest.Builder searchRequestBuilder, String queryText, FulltextSearchRequestDto.Filters filters) {
        searchRequestBuilder.query(q -> q
                .bool(b -> {
                    if (StringUtils.isNotBlank(queryText)) {
                        b.must(m -> m
                                .multiMatch(mm -> mm
                                        .query(queryText)
                                        .fields(ES_FIELD_CONTENT, ES_FIELD_TITLE + "^2")
                                )
                        );
                    } else {
                        b.must(m -> m.matchAll(ma -> ma));
                    }

                    if (filters != null) {
                        if (filters.getFileTypes() != null && !filters.getFileTypes().isEmpty()) {
                            b.filter(filterBool -> filterBool
                                    .bool(shouldBool -> {
                                        for (String fileType : filters.getFileTypes()) {
                                            shouldBool.should(s -> s
                                                    .wildcard(w -> w
                                                            .field(ES_FIELD_FILENAME + ".keyword")
                                                            .value("*." + fileType.toLowerCase())
                                                    )
                                            );
                                        }
                                        return shouldBool;
                                    })
                            );
                        }

                        // --- 关键修复：使用您提供的经过验证的日期范围筛选代码 ---
                        // 日期范围筛选 (基于 last_modified - 纪元秒字段)
                        if (StringUtils.isNotBlank(filters.getDateFrom()) || StringUtils.isNotBlank(filters.getDateTo())) {
                            // 1. 创建一个 DateRangeQuery.Builder 来配置日期范围查询的细节
                            DateRangeQuery dateRangeQuery = DateRangeQuery.of(drq -> {
                                // 2. 指定要进行范围查询的字段
                                drq.field(ES_FIELD_LAST_MODIFIED);

                                // 3. 如果存在开始日期，则设置 "gte" (大于或等于) 条件
                                if (StringUtils.isNotBlank(filters.getDateFrom())) {
                                    try {
                                        // 将 "YYYY-MM-DD" 格式的字符串转换为当天的开始时间的纪元秒
                                        long fromEpochSecond = LocalDate.parse(filters.getDateFrom(), DateTimeFormatter.ISO_LOCAL_DATE)
                                                .atStartOfDay(ZoneOffset.UTC).toEpochSecond();
                                        // 设置范围的下限。注意：这里 gte, lt 等方法接受的是字符串形式的值。
                                        drq.gte(String.valueOf(fromEpochSecond));
                                    } catch (Exception e) {
                                        log.warn("无法解析开始日期 '{}'，日期范围筛选将忽略此条件。", filters.getDateFrom(), e);
                                    }
                                }
                                // 4. 如果存在结束日期，则设置 "lt" (小于) 条件
                                if (StringUtils.isNotBlank(filters.getDateTo())) {
                                    try {
                                        // 将 "YYYY-MM-DD" 格式的字符串转换为下一天的开始时间的纪元秒，以包含结束日期的全天
                                        long toEpochSecond = LocalDate.parse(filters.getDateTo(), DateTimeFormatter.ISO_LOCAL_DATE)
                                                .plusDays(1) // 包含当天
                                                .atStartOfDay(ZoneOffset.UTC).toEpochSecond();
                                        // 设置范围的上限。
                                        drq.lt(String.valueOf(toEpochSecond));
                                    } catch (Exception e) {
                                        log.warn("无法解析结束日期 '{}'，日期范围筛选将忽略此条件。", filters.getDateTo(), e);
                                    }
                                }
                                return drq;
                            });

                            // 5. 将特定类型的 DateRangeQuery 转换为通用的 RangeQuery
                            RangeQuery rangeQuery = dateRangeQuery._toRangeQuery();
                            // 6. 将 RangeQuery 包装成通用的 Query 对象
                            Query finalRangeQuery = new Query(rangeQuery);
                            // 7. 将此范围查询作为 filter 条件添加到主布尔查询中
                            b.filter(finalRangeQuery);
                        }
                    }
                    return b;
                })
        );
    }

    private void buildSort(SearchRequest.Builder searchRequestBuilder, String sortBy) {
        if (StringUtils.isBlank(sortBy) || "relevance".equalsIgnoreCase(sortBy)) {
            return;
        }
        switch (sortBy.toLowerCase()) {
            case "modifieddate_desc":
                searchRequestBuilder.sort(s -> s.field(f -> f.field(ES_FIELD_LAST_MODIFIED).order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)));
                break;
            case "modifieddate_asc":
                searchRequestBuilder.sort(s -> s.field(f -> f.field(ES_FIELD_LAST_MODIFIED).order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)));
                break;
            case "filename_asc":
                searchRequestBuilder.sort(s -> s.field(f -> f.field(ES_FIELD_FILENAME + ".keyword").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)));
                break;
            case "filename_desc":
                searchRequestBuilder.sort(s -> s.field(f -> f.field(ES_FIELD_FILENAME + ".keyword").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)));
                break;
            default:
                log.warn("不支持的排序方式: '{}'，将使用默认相关度排序。", sortBy);
        }
    }

    private void buildHighlight(SearchRequest.Builder searchRequestBuilder, String queryText) {
        if (StringUtils.isNotBlank(queryText)) {
            searchRequestBuilder.highlight(h -> h
                    .fields(ES_FIELD_CONTENT, hf -> hf
                            .preTags("<mark>")
                            .postTags("</mark>")
                            .numberOfFragments(10)
                            .fragmentSize(200)
                    )
            );
        }
    }


    private PageDto<FulltextSearchResultDto> parseSearchResponse(SearchResponse<Map> response, int requestPage, int requestSize) {
        List<FulltextSearchResultDto> results = new ArrayList<>();
        TotalHits totalHits = response.hits().total();
        long totalElements = (totalHits != null) ? totalHits.value() : 0;

        if (!response.hits().hits().isEmpty()) {
            log.debug("第一条搜索结果的 _source 内容: {}", response.hits().hits().get(0).source());
        }

        for (Hit<Map> hit : response.hits().hits()) {
            FulltextSearchResultDto dto = new FulltextSearchResultDto();
            dto.setId(hit.id());
            if (hit.score() != null) {
                dto.setScore(hit.score());
            } else {
                dto.setScore(0.0d);
            }

            Map<String, Object> source = hit.source();
            if (source != null) {
                dto.setSourceFilename((String) source.get(ES_FIELD_FILENAME));
                dto.setSourceRelativePath((String) source.get(ES_FIELD_RELATIVE_PATH));

                Object fileSizeObj = source.get(ES_FIELD_FILE_SIZE);
                if (fileSizeObj instanceof Number) {
                    dto.setFileSize(((Number) fileSizeObj).longValue());
                }

                Object lastModifiedObj = source.get(ES_FIELD_LAST_MODIFIED);
                if (lastModifiedObj instanceof Number) {
                    long epochSeconds = ((Number) lastModifiedObj).longValue();
                    dto.setLastModifiedDate(
                            DateUtils.formatDateTime(DateUtils.convertlocalDateTimeToDate(
                                    java.time.LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
                            ))
                    );
                } else if (lastModifiedObj instanceof String) {
                    dto.setLastModifiedDate((String) lastModifiedObj);
                }
            }

            Map<String, List<String>> highlightFields = hit.highlight();
            if (highlightFields != null && highlightFields.containsKey(ES_FIELD_CONTENT)) {
                dto.setMatchedParagraph(String.join(" ... ", highlightFields.get(ES_FIELD_CONTENT)));
            } else if (source != null && source.containsKey(ES_FIELD_CONTENT)) {
                String originalContent = (String) source.get(ES_FIELD_CONTENT);
                dto.setMatchedParagraph(StringUtils.isNotBlank(originalContent) ?
                        (originalContent.length() > 200 ? originalContent.substring(0, 200) + "..." : originalContent)
                        : "无内容摘要。");
            } else {
                dto.setMatchedParagraph("无匹配内容段落。");
            }
            results.add(dto);
        }
        return new PageDto<>(results, requestPage, requestSize, totalElements);
    }
}
