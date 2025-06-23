/**
 * 目录结构: src/main/java/org/ls/service/impl/FulltextSearchServiceImpl.java
 * 文件名称: FulltextSearchServiceImpl.java
 * 开发时间: 2025-06-03 22:15:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: FulltextSearchService 接口的实现类。
 * 本次更新: 使用 script 查询替换 regexp 查询，以最可靠的方式修复文件类型筛选器。
 */
package org.ls.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Script;
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

    // 根据后台日志确认的、在Elasticsearch中实际使用的字段名
    private static final String ES_FIELD_FILENAME = "filename";
    private static final String ES_FIELD_RELATIVE_PATH = "source_path";
    private static final String ES_FIELD_LAST_MODIFIED = "last_modified";
    private static final String ES_FIELD_FILE_SIZE = "file_size_bytes";
    private static final String ES_FIELD_CONTENT = "content";
    private static final String ES_FIELD_TITLE = "title";

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
                .bool(b -> { // b 是 BoolQuery.Builder
                    // 1. 构建 must 子句，用于全文搜索，这部分会影响评分
                    if (StringUtils.isNotBlank(queryText)) {
                        b.must(m -> m
                                .multiMatch(mm -> mm
                                        .query(queryText)
                                        .fields(ES_FIELD_CONTENT, ES_FIELD_TITLE + "^2") // 标题字段权重更高
                                )
                        );
                    } else {
                        // 如果没有关键词，则匹配所有文档
                        b.must(m -> m.matchAll(ma -> ma));
                    }

                    // 2. 构建 filter 子句，用于精确匹配和范围查询，不影响评分，性能更高
                    if (filters != null) {

                        // --- 关键修复：使用 script 查询替代 regexp 查询，以最可靠的方式实现文件类型筛选 ---
                        if (filters.getFileTypes() != null && !filters.getFileTypes().isEmpty()) {
                            // 为所有文件类型创建一个 should 列表，它们之间是 OR 的关系
                            Query fileTypeQuery = Query.of(queryBuilder -> queryBuilder
                                    .bool(shouldBool -> {
                                        for (String fileType : filters.getFileTypes()) {
                                            // Painless 脚本，用于检查字段是否以指定后缀结尾（不区分大小写）
//                                            String scriptCode = "doc['" + ES_FIELD_FILENAME + ".keyword'].value.toLowerCase().endsWith(params.suffix)";

                                            // Painless 脚本，用于检查 _source 中的字段是否以指定后缀结尾（不区分大小写）
                                            // ctx._source 允许我们直接访问原始文档内容
                                            // 先检查字段是否存在，避免 NullPointerException
                                            // Painless 脚本。在查询脚本中，应使用 `params._source` 来访问原始文档。
                                            // 'ctx' 变量是用于更新脚本的。
                                            // 我们还加入了对字段存在性和类型的检查，以增加脚本的健壮性。
                                            String scriptCode =
                                                    "if (params._source.containsKey('" + ES_FIELD_FILENAME + "') && params._source['" + ES_FIELD_FILENAME + "'] instanceof String) { " +
                                                            "    return params._source['" + ES_FIELD_FILENAME + "'].toLowerCase().endsWith(params.suffix);" +
                                                            "} else { " +
                                                            "    return false;" +
                                                            "}";

                                            // --- 正确的脚本构建方式 (ES Client 8.15.0+) ---
                                            // 在新版本中，不再有 InlineScript 类和 .inline() 方法。
                                            // 直接在 Script.Builder 上设置 source 和 params 来创建内联脚本。
                                            Script script = new Script.Builder()
                                                    .source(scriptCode)
                                                    .params("suffix", JsonData.of("." + fileType.toLowerCase()))
                                                    .lang("painless") // 可选，因为 "painless" 是默认语言
                                                    .build();

                                            // 在 should 子句中使用最终的 Script 对象
                                            shouldBool.should(s -> s.script(sc -> sc.script(script)));
                                        }
                                        // 至少需要匹配一个 "should" 子句才算命中
                                        shouldBool.minimumShouldMatch("1");
                                        return shouldBool;
                                    })
                            );
                            // 将这个包含多个脚本查询的 bool 查询作为 filter 添加到主查询中
                            b.filter(fileTypeQuery);
                        }

                        // --- 使用您验证过的正确日期范围筛选代码 ---
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
                            .numberOfFragments(5)
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
