/**
 * 目录结构: src/main/java/org/ls/dto/FulltextSearchResultDto.java
 * 文件名称: FulltextSearchResultDto.java
 * 开发时间: 2025-06-03 21:32:00 (Asia/Shanghai)
 * 作者: Gemini
 * 代码用途: 定义单个全文搜索结果项的数据传输对象 (DTO)。
 * 用于从后端服务层传递给API控制器，并最终返回给前端。
 */
package org.ls.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FulltextSearchResultDto {

    /**
     * Elasticsearch中文档的唯一ID。
     */
    private String id;

    /**
     * 匹配到的内容段落。
     * 可能包含高亮标签 (例如 <mark>关键词</mark>)，具体取决于后端实现。
     */
    private String matchedParagraph;

    /**
     * 匹配内容所属的源文件名。
     * 例如: "年度报告.docx"
     */
    private String sourceFilename;

    /**
     * 匹配内容所属源文件在加密目录下的相对路径。
     * 例如: "departmentA/reports/2023/"
     * 这个路径用于后续的文件下载操作。
     */
    private String sourceRelativePath;

    /**
     * 源文件的最后修改日期。
     * 可以是ISO格式的字符串 (例如 "2023-10-15T10:30:00Z") 或格式化后的字符串。
     * 设计文档中前端显示为 "YYYY-MM-DD HH:mm"。
     */
    private String lastModifiedDate; // 或者使用 LocalDateTime/Date 类型，然后在API层或前端格式化

    /**
     * 源文件的大小 (以字节为单位)。
     * 前端会将其格式化为KB, MB等。
     */
    private Long fileSize; // 使用Long以支持大文件，并允许为null

    /**
     * Elasticsearch返回的相关度评分。
     * 可选，主要在按相关度排序时有意义。
     */
    private Double score; // 使用Double，允许为null
}