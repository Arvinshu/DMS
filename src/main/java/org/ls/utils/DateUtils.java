/**
 * 文件路径: src/main/java/org/ls/utils/DateUtils.java
 * 开发时间: 2025-05-10 19:45:00 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 提供日期和时间相关的实用工具方法。
 * 本次更新将添加对预定义和自定义日期范围的解析功能。
 */

package org.ls.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import java.util.Date;

/**
 * 日期处理工具类
 */
@Component // 声明为Spring组件，方便在Service中注入使用
public class DateUtils {

    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM = "yyyy-MM";

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);
    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    // 定义日期范围的常量
    public static final String THIS_WEEK = "this_week";
    public static final String LAST_WEEK = "last_week";
    public static final String THIS_MONTH = "this_month";
    public static final String LAST_MONTH = "last_month";
    public static final String THIS_QUARTER = "this_quarter";
    public static final String LAST_QUARTER = "last_quarter"; // 新增：上个季度
    public static final String CUSTOM_RANGE_PREFIX = "custom_";

    /**
     * 内部类或记录 (Java 14+) 用于封装日期范围的开始和结束。
     * 如果项目JDK版本低于14，可以使用普通类。
     */
    public static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        @Override
        public String toString() {
            return "DateRange{" +
                    "startDate=" + startDate +
                    ", endDate=" + endDate +
                    '}';
        }
    }

    /**
     * 解析日期范围字符串，将其转换为包含开始日期和结束日期的 DateRange 对象。
     * 支持的字符串格式: "this_week", "last_week", "this_month", "last_month", "this_quarter", "last_quarter",
     * 以及自定义范围 "custom_yyyy-MM-dd_yyyy-MM-dd"。
     *
     * @param dateRangeString 代表日期范围的字符串。
     * @return DateRange 对象，包含解析后的开始和结束日期。
     * 如果字符串格式无效或无法解析，则默认返回当天的日期范围或抛出异常。
     */
    public DateRange parseDateRange(String dateRangeString) {
        if (StringUtils.isBlank(dateRangeString)) {
            logger.warn("日期范围字符串为空，默认返回当天。");
            return new DateRange(LocalDate.now(), LocalDate.now());
        }

        LocalDate today = LocalDate.now();

        switch (dateRangeString.toLowerCase()) {
            case THIS_WEEK:
                return getStartAndEndOfWeek(today);
            case LAST_WEEK:
                return getStartAndEndOfWeek(today.minusWeeks(1));
            case THIS_MONTH:
                return getStartAndEndOfMonth(today);
            case LAST_MONTH:
                return getStartAndEndOfMonth(today.minusMonths(1));
            case THIS_QUARTER:
                return getStartAndEndOfQuarter(today);
            case LAST_QUARTER:
                return getStartAndEndOfQuarter(today.minusMonths(3)); // 上个季度大致是3个月前
            default:
                if (dateRangeString.startsWith(CUSTOM_RANGE_PREFIX)) {
                    String customDates = dateRangeString.substring(CUSTOM_RANGE_PREFIX.length());
                    String[] dates = customDates.split("_");
                    if (dates.length == 2) {
                        try {
                            LocalDate startDate = LocalDate.parse(dates[0], DEFAULT_DATE_FORMATTER);
                            LocalDate endDate = LocalDate.parse(dates[1], DEFAULT_DATE_FORMATTER);
                            if (endDate.isBefore(startDate)) {
                                logger.warn("自定义日期范围中，结束日期 {} 早于开始日期 {}。将交换它们。", endDate, startDate);
                                return new DateRange(endDate, startDate);
                            }
                            return new DateRange(startDate, endDate);
                        } catch (DateTimeParseException e) {
                            logger.error("无法解析自定义日期范围字符串: {}。错误: {}", customDates, e.getMessage());
                        }
                    } else {
                        logger.warn("自定义日期范围格式无效: {}。期望格式 'custom_yyyy-MM-dd_yyyy-MM-dd'。", dateRangeString);
                    }
                } else {
                    logger.warn("未知的日期范围字符串: {}。默认返回当天。", dateRangeString);
                }
                // 默认或错误处理：返回当天
                return new DateRange(today, today);
        }
    }

    /**
     * 获取指定日期所在周的开始日期（周一）和结束日期（周日）。
     * 中国地区通常认为周一是一周的开始。
     *
     * @param date 参考日期
     * @return DateRange 对象，包含该周的周一和周日
     */
    public DateRange getStartAndEndOfWeek(LocalDate date) {
        // WeekFields.ISO 定义周一为一周的开始，周日为结束。
        // WeekFields.SUNDAY_START 定义周日为一周的开始。
        // 根据项目需求选择，这里使用中国常用的周一为开始。
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new DateRange(startOfWeek, endOfWeek);
    }

    /**
     * 获取指定日期所在月份的开始日期和结束日期。
     *
     * @param date 参考日期
     * @return DateRange 对象，包含该月的第一天和最后一天
     */
    public DateRange getStartAndEndOfMonth(LocalDate date) {
        LocalDate startOfMonth = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
        return new DateRange(startOfMonth, endOfMonth);
    }

    /**
     * 获取指定日期所在季度的开始日期和结束日期。
     * 季度定义：Q1 (1-3月), Q2 (4-6月), Q3 (7-9月), Q4 (10-12月)。
     *
     * @param date 参考日期
     * @return DateRange 对象，包含该季度的第一天和最后一天
     */
    public DateRange getStartAndEndOfQuarter(LocalDate date) {
        YearMonth currentYearMonth = YearMonth.from(date);
        int currentMonth = currentYearMonth.getMonthValue();
        LocalDate firstDayOfQuarter;
        LocalDate lastDayOfQuarter;

        if (currentMonth >= 1 && currentMonth <= 3) { // Q1
            firstDayOfQuarter = LocalDate.of(date.getYear(), 1, 1);
            lastDayOfQuarter = LocalDate.of(date.getYear(), 3, 31);
        } else if (currentMonth >= 4 && currentMonth <= 6) { // Q2
            firstDayOfQuarter = LocalDate.of(date.getYear(), 4, 1);
            lastDayOfQuarter = LocalDate.of(date.getYear(), 6, 30);
        } else if (currentMonth >= 7 && currentMonth <= 9) { // Q3
            firstDayOfQuarter = LocalDate.of(date.getYear(), 7, 1);
            lastDayOfQuarter = LocalDate.of(date.getYear(), 9, 30);
        } else { // Q4 (currentMonth >= 10 && currentMonth <= 12)
            firstDayOfQuarter = LocalDate.of(date.getYear(), 10, 1);
            lastDayOfQuarter = LocalDate.of(date.getYear(), 12, 31);
        }
        // 确保月份最后一天正确处理闰年
        lastDayOfQuarter = YearMonth.from(lastDayOfQuarter).atEndOfMonth();
        return new DateRange(firstDayOfQuarter, lastDayOfQuarter);
    }

    /**
     * 将 LocalDate 对象格式化为 "yyyy-MM-dd" 格式的字符串。
     *
     * @param date 要格式化的日期
     * @return 格式化后的日期字符串，如果date为null则返回null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DEFAULT_DATE_FORMATTER);
    }
//
//    /**
//     * 将 "yyyy-MM-dd" 格式的字符串解析为 LocalDate 对象。
//     *
//     * @param dateString 要解析的日期字符串
//     * @return 解析后的 LocalDate 对象，如果字符串无效或为null则返回null
//     */
//    public static LocalDate parseDate(String dateString) {
//        if (StringUtils.isBlank(dateString)) {
//            return null;
//        }
//        try {
//            return LocalDate.parse(dateString, DEFAULT_DATE_FORMATTER);
//        } catch (DateTimeParseException e) {
//            logger.error("无法解析日期字符串: {}。期望格式 'yyyy-MM-dd'。", dateString, e);
//            return null;
//        }
//    }

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
        if (date == null) {
            return null;
        }
        return formatDate(date, YYYY_MM_DD_HH_MM_SS);
    }


    /**
     * 将 LocalDateTime 日期对象格式化为 Date
     *
     * @param localDateTime 日期对象
     * @return Date 格式化后的日期字符串，如果 date 为 null 则返回 null
     */
    public static Date convertlocalDateTimeToDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        Date date = Date.from( localDateTime.atZone( ZoneId.systemDefault()).toInstant());
        return date;
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