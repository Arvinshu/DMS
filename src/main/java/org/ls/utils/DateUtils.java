package org.ls.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日期处理工具类
 */
public class DateUtils {

    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM = "yyyy-MM";

    /**
     * 获取当前时间的指定格式字符串
     *
     * @param pattern 日期格式，例如 "yyyy-MM-dd HH:mm:ss"
     * @return 格式化后的日期字符串
     */
    public static String getCurrentDateStr(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(new Date());
    }

    /**
     * 获取当前时间的默认格式 (yyyy-MM-dd HH:mm:ss) 字符串
     *
     * @return 格式化后的日期字符串
     */
    public static String getCurrentDateTimeStr() {
        return getCurrentDateStr(YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 获取当前时间的默认格式 (yyyy-MM-dd) 字符串
     *
     * @return 格式化后的日期字符串
     */
    public static String getCurrentDateStr() {
        return getCurrentDateStr(YYYY_MM_DD);
    }

    /**
     * 将日期对象格式化为指定格式的字符串
     *
     * @param date    日期对象
     * @param pattern 日期格式
     * @return 格式化后的日期字符串，如果 date 为 null 则返回 null
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * 将日期对象格式化为默认格式 (yyyy-MM-dd HH:mm:ss) 的字符串
     *
     * @param date 日期对象
     * @return 格式化后的日期字符串，如果 date 为 null 则返回 null
     */
    public static String formatDateTime(Date date) {
        return formatDate(date, YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 将日期对象格式化为默认日期格式 (yyyy-MM-dd) 的字符串
     *
     * @param date 日期对象
     * @return 格式化后的日期字符串，如果 date 为 null 则返回 null
     */
    public static String formatDate(Date date) {
        return formatDate(date, YYYY_MM_DD);
    }

    /**
     * 将指定格式的字符串解析为日期对象
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return 解析后的日期对象，如果解析失败则返回 null
     */
    public static Date parseDate(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            log.error("日期字符串解析失败: str={}, pattern={}, error={}", dateStr, pattern, e.getMessage());
            return null;
        }
    }

    /**
     * 将默认格式 (yyyy-MM-dd HH:mm:ss) 的字符串解析为日期对象
     *
     * @param dateStr 日期字符串
     * @return 解析后的日期对象，如果解析失败则返回 null
     */
    public static Date parseDateTime(String dateStr) {
        return parseDate(dateStr, YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 将默认日期格式 (yyyy-MM-dd) 的字符串解析为日期对象
     *
     * @param dateStr 日期字符串
     * @return 解析后的日期对象，如果解析失败则返回 null
     */
    public static Date parseDate(String dateStr) {
        return parseDate(dateStr, YYYY_MM_DD);
    }

}