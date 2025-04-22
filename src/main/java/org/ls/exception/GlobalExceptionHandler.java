package org.ls.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理类
 * 使用 @ControllerAdvice 注解，使其成为一个全局的异常处理组件。
 * 可以处理控制器层抛出的各种异常。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 SQL 相关异常
     *
     * @param ex SQLException
     * @param request HttpServletRequest
     * @return 错误页面或 JSON 响应
     */
    @ExceptionHandler(SQLException.class)
    public Object handleSQLException(SQLException ex, HttpServletRequest request) {
        log.error("SQL异常: 请求URL [{}], 错误信息 [{}]", request.getRequestURI(), ex.getMessage(), ex);

        // 判断请求是否期望 JSON 响应
        if (isAjaxRequest(request)) {
            Map<String, Object> errorDetails = createErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, "数据库操作失败", ex.getMessage(), request.getRequestURI());
            return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            ModelAndView mav = new ModelAndView();
            mav.addObject("timestamp", new Date());
            mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            mav.addObject("error", "数据库错误");
            mav.addObject("message", "执行数据库操作时发生错误，请联系管理员。");
            mav.addObject("path", request.getRequestURI());
            mav.setViewName("error/500"); // 指向通用的错误页面 templates/error/500.html
            return mav;
        }
    }

    /**
     * 处理参数验证异常 (@Valid 注解触发)
     *
     * @param ex MethodArgumentNotValidException
     * @param request HttpServletRequest
     * @return 错误页面或 JSON 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 返回 400 状态码
    public Object handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("参数验证失败: 请求URL [{}], 错误详情 [{}]", request.getRequestURI(), errors);

        if (isAjaxRequest(request)) {
            Map<String, Object> errorDetails = createErrorDetails(HttpStatus.BAD_REQUEST, "参数验证失败", errors.toString(), request.getRequestURI());
            errorDetails.put("errors", errors); // 可以将详细错误信息放入响应
            return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
        } else {
            ModelAndView mav = new ModelAndView();
            mav.addObject("timestamp", new Date());
            mav.addObject("status", HttpStatus.BAD_REQUEST.value());
            mav.addObject("error", "参数错误");
            mav.addObject("message", "请求参数不合法，请检查后重试。");
            mav.addObject("errors", errors); // 将错误详情传递给页面
            mav.addObject("path", request.getRequestURI());
            mav.setViewName("error/400"); // 指向参数错误的页面 templates/error/400.html
            return mav;
        }
    }

    /**
     * 处理自定义业务异常 (如果定义了)
     * @param ex BusinessException
     * @param request HttpServletRequest
     * @return ResponseEntity (通常返回 JSON)
     */
    /*
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: 请求URL [{}], 错误代码 [{}], 错误信息 [{}]", request.getRequestURI(), ex.getCode(), ex.getMessage());
        Map<String, Object> errorDetails = createErrorDetails(HttpStatus.BAD_REQUEST, "业务处理失败", ex.getMessage(), request.getRequestURI());
        errorDetails.put("code", ex.getCode()); // 可以包含业务错误码
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
    */

    /**
     * 处理所有其他未捕获的异常
     *
     * @param ex Exception
     * @param request HttpServletRequest
     * @return 错误页面或 JSON 响应
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("未捕获的异常: 请求URL [{}], 异常类型 [{}], 错误信息 [{}]",
                request.getRequestURI(), ex.getClass().getName(), ex.getMessage(), ex);

        if (isAjaxRequest(request)) {
            Map<String, Object> errorDetails = createErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", "发生未知错误，请联系管理员", request.getRequestURI());
            return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            ModelAndView mav = new ModelAndView();
            mav.addObject("timestamp", new Date());
            mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            mav.addObject("error", "内部服务器错误");
            mav.addObject("message", "系统发生了一个意外错误，请稍后重试或联系管理员。");
            mav.addObject("path", request.getRequestURI());
            mav.setViewName("error/500"); // 指向通用的错误页面 templates/error/500.html
            return mav;
        }
    }

    /**
     * 判断请求是否为 AJAX 请求
     *
     * @param request HttpServletRequest
     * @return boolean
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWithHeader = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWithHeader);
    }

    /**
     * 创建标准的错误详情 Map
     *
     * @param status HttpStatus
     * @param error String 错误类型
     * @param message String 错误信息
     * @param path String 请求路径
     * @return Map<String, Object>
     */
    private Map<String, Object> createErrorDetails(HttpStatus status, String error, String message, String path) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", new Date());
        errorDetails.put("status", status.value());
        errorDetails.put("error", error);
        errorDetails.put("message", message);
        errorDetails.put("path", path);
        return errorDetails;
    }
}