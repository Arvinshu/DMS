package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DTO 用于封装批量导入工时记录的结果。
 * 文件路径: src/main/java/org/ls/dto/BatchInsertResult.java
 */
@Data // Lombok: 生成 getter, setter, toString, equals, hashCode
@NoArgsConstructor // Lombok: 生成无参构造函数
@AllArgsConstructor // Lombok: 生成全参构造函数
public class BatchInsertResult {

    /**
     * 原始数据在提交列表中的索引 (从 0 开始)。
     * 用于前端匹配结果到对应的预览行。
     */
    private int originalIndex;

    /**
     * 导入是否成功。
     */
    private boolean success;

    /**
     * 导入结果的消息。
     * 成功时可以是 "导入成功"，失败时是具体的错误原因（如 "主键冲突", "数据验证失败: xx不能为空"）。
     */
    private String message;

    // --- 以下为标识字段，帮助前端定位记录 ---

    /**
     * 工时申请单号 (用于前端匹配)。
     */
    private String tsId;

    /**
     * 员工信息 (用于前端匹配)。
     */
    private String employee;

    /**
     * 工时日期 (以字符串形式，方便前端匹配)。
     */
    private String tsDate;

    /**
     * 工时编码 (用于前端匹配)。
     */
    private String tsBm;

    /**
     * 静态工厂方法，方便创建实例。
     * @param index 索引
     * @param success 是否成功
     * @param message 消息
     * @param tsId 工时单号
     * @param employee 员工
     * @param tsDate 日期
     * @param tsBm 工时编码
     * @return BatchInsertResult 实例
     */
    public static BatchInsertResult create(int index, boolean success, String message, String tsId, String employee, LocalDate tsDate, String tsBm) {
        String dateStr = (tsDate != null) ? tsDate.format(DateTimeFormatter.ISO_DATE) : null;
        return new BatchInsertResult(index, success, message, tsId, employee, dateStr, tsBm);
    }
}

//        * **说明:**
//        * 使用 Lombok 简化代码。
//        * `originalIndex`: 帮助前端将结果与预览表格中的行对应起来。
//        * `success`: 标记该行数据是否成功导入。
//        * `message`: 提供用户可读的反馈信息。
//        * `tsId`, `employee`, `tsDate` (String), `tsBm`: 包含记录的关键标识，进一步帮助前端定位。`tsDate` 使用 String 是为了避免序列化/反序列化及前端匹配时可能出现的日期格式问题。
//        * 提供了一个静态工厂方法 `create` 方便在 Service 中创建