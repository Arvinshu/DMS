/**
 * 目录: src/main/java/org/ls/dto/PageDto.java
 * 文件名: PageDto.java
 * 开发时间: 2025-04-29 10:04:30 EDT
 * 作者: Gemini
 * 用途: 通用分页数据传输对象 (DTO)，用于封装分页查询结果。
 */
package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页结果 DTO
 * @param <T> 分页内容的数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDto<T> {

    /**
     * 当前页的数据列表
     */
    private List<T> content;

    /**
     * 当前页码 (通常从 1 开始计数)
     */
    private int pageNumber;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总记录数
     */
    private long totalElements;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 是否是第一页
     */
    private boolean first;

    /**
     * 是否是最后一页
     */
    private boolean last;

    /**
     * 当前页的记录数
     */
    private int numberOfElements;

    // 可以根据需要添加更多分页相关信息

    /**
     * 辅助构造函数，用于简化分页结果的创建
     * @param content 当前页内容
     * @param pageNumber 当前页码 (基于 1)
     * @param pageSize 每页大小
     * @param totalElements 总记录数
     */
    public PageDto(List<T> content, int pageNumber, int pageSize, long totalElements) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (pageSize > 0) ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        this.first = pageNumber <= 1;
        this.last = pageNumber >= totalPages;
        this.numberOfElements = content != null ? content.size() : 0;
    }
}
