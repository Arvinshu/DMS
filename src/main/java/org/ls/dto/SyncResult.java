package org.ls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO 用于封装单个同步操作（如同步部门）的执行结果。
 * 文件路径: src/main/java/org/ls/dto/SyncResult.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    /**
     * 本次同步操作成功新增的记录数。
     */
    private long successCount = 0;

    /**
     * 本次同步操作失败的记录数（例如，由于查找依赖失败等）。
     * 注意：主键冲突通常不算作这里的失败，因为它们只是表示数据已存在。
     */
    private long failureCount = 0;

    /**
     * 关于本次同步操作的总结性消息。
     * 例如："部门同步完成，新增 5 条记录。" 或 "员工同步完成，新增 10 条，失败 2 条（部门未找到）。"
     */
    private String message;

    // 可以根据需要添加更详细的错误列表
    // private List<String> errorDetails;
}


//        * **说明:**
//        * 用于后端同步方法（如 `syncDepartments`）返回给 Controller，再由 Controller 返回给前端。
//        * 包含成功新增的数量、处理失败的数量以及一个总结
