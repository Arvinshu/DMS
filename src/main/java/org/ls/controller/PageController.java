package org.ls.controller;

import jakarta.servlet.http.HttpServletRequest; // 确保导入 HttpServletRequest
import org.ls.utils.DateUtils;
import org.ls.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// 注意：这里移除了之前可能存在的 java.util.Map 导入，因为它在此文件中不再需要

/**
 * 处理基本页面导航的控制器 (已修正 URI 处理)
 * 文件路径: src/main/java/org/ls/controller/PageController.java
 * 职责：处理指向主要 HTML 页面的请求，并为模板准备必要的数据。
 */
@Controller // 标记此类为 Spring MVC 控制器，用于处理 Web 请求并返回视图名称
public class PageController {

    // 2. 创建 Logger 实例
    private static final Logger log = LoggerFactory.getLogger(PageController.class);

    // 从 application.properties 文件注入 app.copyright 属性值
    // 如果属性不存在，则使用默认值 "© 默认版权信息"
    @Value("${app.copyright:© 默认版权信息}")
    private String copyrightInfo;

    /**
     * 处理首页请求 (根路径 "/" 或 "/index")
     * @param model Model 对象，用于向 Thymeleaf 模板传递数据
     * @param request HttpServletRequest 对象，由 Spring MVC 自动注入，用于获取请求信息
     * @return 首页模板的逻辑视图名称 "index" (对应 templates/index.html)
     */
    @GetMapping({"/", "/index"}) // 映射 HTTP GET 请求到 "/" 或 "/index" 路径
    public String indexPage(Model model, HttpServletRequest request) {
        String currentURI = request.getRequestURI();
        log.info("Rendering /index, requestURI: {}", currentURI); // 添加日志

        // 添加当前时间字符串到模型
        model.addAttribute("currentTime", DateUtils.getCurrentDateTimeStr());
        // 添加客户端 IP 地址到模型
        model.addAttribute("clientIp", IpUtils.getClientIpAddr(request));
        // 添加版权信息到模型
        model.addAttribute("copyright", copyrightInfo);
        // 将当前请求的 URI 添加到模型，属性名为 "requestURI"
        // 用于在 header.html 中判断哪个导航链接应该高亮显示
        model.addAttribute("requestURI", request.getRequestURI());

        log.debug("Model attributes for /index: {}", model.asMap()); // 打印模型内容
        // 返回视图名称
        return "index";
    }

    /**
     * 处理工时统计页面请求 ("/timesheets/statistics")
     * @param model Model 对象
     * @param request HttpServletRequest 对象 (自动注入)
     * @return 工时统计页面模板的逻辑视图名称 "timesheet_statistics" (对应 templates/timesheet_statistics.html)
     */
    @GetMapping("/timesheets/statistics")
      public String timesheetStatisticsPage(Model model, HttpServletRequest request) {
        String currentURI = request.getRequestURI();
        log.info("Rendering /timesheets/statistics, requestURI: {}", currentURI); // 添加日志

        // 将当前请求 URI 添加到 Model 中，供 header.html 使用
        model.addAttribute("requestURI", currentURI);
        log.debug("Model attributes for /timesheets/statistics: {}", model.asMap()); // 打印模型内容
        // 此处可以根据需要添加其他需要在页面加载时传递给工时统计页面的模型属性
        // 例如：默认的过滤条件、下拉列表选项等
        return "timesheet_statistics";
    }



    /**
     * 处理数据维护主页面请求 ("/datamaintenance")
     * @param model Model 对象
     * @param request HttpServletRequest 对象 (自动注入)
     * @return 数据维护页面模板的逻辑视图名称 "datamaintenance" (对应 templates/datamaintenance.html)
     */
    @GetMapping("/datamaintenance")
    public String dataMaintenancePage(Model model, HttpServletRequest request) {
        String currentURI = request.getRequestURI();
        // 3. 添加详细日志，检查 request 和 URI 是否为 null
        if (request == null) {
            log.error("HttpServletRequest is NULL in dataMaintenancePage!");
            currentURI = "/error_request_null"; // 设置一个特殊值以便追踪
        } else {
            currentURI = request.getRequestURI();
            if (currentURI == null) {
                log.error("request.getRequestURI() returned NULL in dataMaintenancePage!");
                currentURI = "/error_uri_null"; // 设置一个特殊值
            }
        }
        log.info("Rendering /datamaintenance, obtained requestURI: {}", currentURI); // 确认获取到的值
        // 将当前请求 URI 添加到 Model 中，供 header.html 使用
        model.addAttribute("requestURI", currentURI);
        // 4. 再次确认 Model 中的值
        log.debug("Model attributes for /datamaintenance: {}", model.asMap());

        // 此处可以根据需要添加其他需要在页面加载时传递给数据维护页面的模型属性
        // 例如：用户权限信息、通用的下拉列表数据等
        return "datamaintenance";
    }

    /**
     * 新增：处理部门工作统计页面请求 ("/department-stats")
     * @param model Model 对象
     * @param request HttpServletRequest 对象 (自动注入)
     * @return 部门工作统计页面模板的逻辑视图名称 "department_stats" (对应 templates/department_stats.html)
     */
    @GetMapping("/department-stats")
    public String departmentStatsPage(Model model, HttpServletRequest request) {
        String currentURI = request.getRequestURI();
        log.info("Rendering /department-stats, requestURI: {}", currentURI);
        // 将当前请求 URI 添加到 Model 中，供 header.html 使用
        model.addAttribute("requestURI", currentURI);
        // 此处可以添加其他需要在页面加载时使用的模型属性，例如默认日期范围
        // model.addAttribute("defaultStartDate", LocalDate.now().withDayOfYear(1));
        // model.addAttribute("defaultEndDate", LocalDate.now());
        return "department_stats"; // 返回新页面的模板名
    }





    // 如果将来添加了其他需要包含公共 header 的页面，
    // 对应的 Controller 方法也应该按照类似的方式注入 HttpServletRequest
    // 并将 request.getRequestURI() 添加到 Model 中。
}
