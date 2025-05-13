/**
 * 文件路径: src/main/java/org/ls/dto/ApiResponse.java
 * 开发时间: 2025-05-10 19:05:00 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 定义通用的API响应结构，支持泛型数据荷载。
 */
package org.ls.dto; // 放在基础dto包下，因为它更通用

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 请求是否成功
     */
    private boolean success;

    /**
     * 响应消息，通常在失败时提供更多信息
     */
    private String message;

    /**
     * 实际的响应数据荷载
     */
    private T data;

    /**
     * 成功的静态工厂方法
     * @param data 响应数据
     * @return 包含数据的成功响应对象
     * @param <T> 数据的泛型类型
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    /**
     * 成功的静态工厂方法 (无特定数据返回，仅表示成功)
     * @return 成功响应对象
     * @param <T> 数据的泛型类型 (通常为 Void 或 Object)
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "操作成功", null);
    }

    /**
     * 失败的静态工厂方法
     * @param message 错误消息
     * @return 失败响应对象
     * @param <T> 数据的泛型类型
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    /**
     * 失败的静态工厂方法 (包含错误数据，较少使用)
     * @param message 错误消息
     * @param data 相关的错误数据或上下文
     * @return 失败响应对象
     * @param <T> 数据的泛型类型
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}
