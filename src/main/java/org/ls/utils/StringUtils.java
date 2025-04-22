package org.ls.utils;

/**
 * 字符串处理工具类
 *
 * 包含了一些常用的字符串检查方法（`isEmpty`, `isBlank`等）。
 * 提供了一个非常基础的 `simpleSanitize` 方法用于XSS防护，**强烈建议**在生产环境中使用更专业的库（如 OWASP Java HTML Sanitizer）来确保安全。
 * 添加了 `splitAndGet` 方法，用于根据需求拆分 `employee` 和 `zone`
 */
public class StringUtils {

    /**
     * 判断字符串是否为空（null 或 ""）
     *
     * @param str 待检查的字符串
     * @return 如果字符串为空则返回 true，否则返回 false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否不为空（既不是 null 也不是 ""）
     *
     * @param str 待检查的字符串
     * @return 如果字符串不为空则返回 true，否则返回 false
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断字符串是否为空白（null、"" 或只包含空白字符）
     *
     * @param str 待检查的字符串
     * @return 如果字符串为空白则返回 true，否则返回 false
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否不为空白（既不是 null、"" 也不是只包含空白字符）
     *
     * @param str 待检查的字符串
     * @return 如果字符串不为空白则返回 true，否则返回 false
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 简单的 XSS 清理方法 (基础实现，建议使用成熟库如 OWASP Java HTML Sanitizer)
     * 替换常见的 HTML 特殊字符
     *
     * @param value 输入的字符串
     * @return 清理后的字符串
     */
    public static String simpleSanitize(String value) {
        if (value == null) {
            return null;
        }
        // 替换可能引起 XSS 的字符
        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        // value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;"); // 按需替换括号
        value = value.replaceAll("'", "&#39;"); // 替换单引号
        value = value.replaceAll("eval\\((.*)\\)", ""); // 移除 eval 表达式
        value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\""); // 移除 javascript: 协议
        value = value.replaceAll("script", ""); // 移除 script 标签 (简单粗暴，可能误伤)
        return value;
    }

    /**
     * 按照分隔符拆分字符串，并获取指定索引的元素
     * @param source 源字符串，例如 "工号-姓名" 或 "部分1-部分2-部分3"
     * @param delimiter 分隔符，例如 "-"
     * @param index 要获取的部分的索引 (从 0 开始)
     * @return 拆分后的指定部分，如果源字符串为空、索引越界或拆分结果不足，则返回 null
     */
    public static String splitAndGet(String source, String delimiter, int index) {
        if (isEmpty(source) || isEmpty(delimiter) || index < 0) {
            return null;
        }
        String[] parts = source.split(delimiter);
        if (index >= parts.length) {
            return null; // 索引超出范围
        }
        return parts[index].trim(); // 返回并去除可能的前后空格
    }
}
