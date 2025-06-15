/**
 * 目录结构: src/main/java/org/ls/dto/FulltextSearchRequestDto.java
 * 文件名称: FulltextSearchRequestDto.java
 * 开发时间: 2025-06-03 21:32:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 定义发送给后端的全文搜索请求的数据传输对象 (DTO)。
 * 包含搜索关键词、分页信息、筛选条件和排序方式。
 */
package org.ls.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulltextSearchRequestDto {

    /**
     * 用户输入的搜索关键词。
     */
    private String query;

    /**
     * 请求的页码 (从1开始)。
     * 默认为 1。
     */
    private int page = 1;

    /**
     * 每页显示的记录数。
     * 默认为 50 (与设计文档一致)。
     */
    private int size = 50;

    /**
     * 筛选条件。
     */
    private Filters filters;

    /**
     * 排序方式。
     * 例如: "relevance", "modifiedDate_desc", "filename_asc"。
     * 默认为 "relevance"。
     */
    private String sortBy = "relevance";

    /**
     * 内部类，用于封装筛选条件。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filters {
        /**
         * 要筛选的文件类型列表 (例如: ["docx", "pdf"])。
         * 可选。
         */
        private List<String> fileTypes;

        /**
         * 日期范围的开始日期 (格式: "YYYY-MM-DD")。
         * 对应文件修改日期。
         * 可选。
         */
        private String dateFrom;

        /**
         * 日期范围的结束日期 (格式: "YYYY-MM-DD")。
         * 对应文件修改日期。
         * 可选。
         */
        private String dateTo;
    }
}