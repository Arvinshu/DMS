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
import org.springframework.web.context.request.WebRequest; // Import WebRequest

import java.io.FileNotFoundException; // Import FileNotFoundException
import java.io.IOException; // Import IOException
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

//    /**
//     * 处理参数验证异常 (@Valid 注解触发)
//     *
//     * @param ex MethodArgumentNotValidException
//     * @param request HttpServletRequest
//     * @return 错误页面或 JSON 响应
//     */
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST) // 返回 400 状态码
//    public Object handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getFieldErrors().forEach(error ->
//                errors.put(error.getField(), error.getDefaultMessage()));
//
//        log.warn("参数验证失败: 请求URL [{}], 错误详情 [{}]", request.getRequestURI(), errors);
//
//        if (isAjaxRequest(request)) {
//            Map<String, Object> errorDetails = createErrorDetails(HttpStatus.BAD_REQUEST, "参数验证失败", errors.toString(), request.getRequestURI());
//            errorDetails.put("errors", errors); // 可以将详细错误信息放入响应
//            return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
//        } else {
//            ModelAndView mav = new ModelAndView();
//            mav.addObject("timestamp", new Date());
//            mav.addObject("status", HttpStatus.BAD_REQUEST.value());
//            mav.addObject("error", "参数错误");
//            mav.addObject("message", "请求参数不合法，请检查后重试。");
//            mav.addObject("errors", errors); // 将错误详情传递给页面
//            mav.addObject("path", request.getRequestURI());
//            mav.setViewName("error/400"); // 指向参数错误的页面 templates/error/400.html
//            return mav;
//        }
//    }

//    /**
//     * 处理自定义业务异常 (如果定义了)
//     * @param ex BusinessException
//     * @param request HttpServletRequest
//     * @return ResponseEntity (通常返回 JSON)
//     */
    /*
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: 请求URL [{}], 错误代码 [{}], 错误信息 [{}]", request.getRequestURI(), ex.getCode(), ex.getMessage());
        Map<String, Object> errorDetails = createErrorDetails(HttpStatus.BAD_REQUEST, "业务处理失败", ex.getMessage(), request.getRequestURI());
        errorDetails.put("code", ex.getCode()); // 可以包含业务错误码
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
    */

//    /**
//     * 处理所有其他未捕获的异常
//     *
//     * @param ex Exception
//     * @param request HttpServletRequest
//     * @return 错误页面或 JSON 响应
//     */
//    @ExceptionHandler(Exception.class)
//    public Object handleGenericException(Exception ex, HttpServletRequest request) {
//        log.error("未捕获的异常: 请求URL [{}], 异常类型 [{}], 错误信息 [{}]",
//                request.getRequestURI(), ex.getClass().getName(), ex.getMessage(), ex);
//
//        if (isAjaxRequest(request)) {
//            Map<String, Object> errorDetails = createErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", "发生未知错误，请联系管理员", request.getRequestURI());
//            return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
//        } else {
//            ModelAndView mav = new ModelAndView();
//            mav.addObject("timestamp", new Date());
//            mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
//            mav.addObject("error", "内部服务器错误");
//            mav.addObject("message", "系统发生了一个意外错误，请稍后重试或联系管理员。");
//            mav.addObject("path", request.getRequestURI());
//            mav.setViewName("error/500"); // 指向通用的错误页面 templates/error/500.html
//            return mav;
//        }
//    }

    /**
     * 处理数据验证失败异常 (例如 @Valid 注解)
     * @param ex MethodArgumentNotValidException
     * @return 包含错误信息的 ResponseEntity (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validation failed: {}", errors);
        // 返回统一的错误结构体，例如:
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("messages", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // --- New Exception Handlers for File Management/Sync ---

    /**
     * 处理文件未找到异常 (例如下载时)
     * @param ex FileNotFoundException
     * @param request WebRequest
     * @return ResponseEntity (404 Not Found)
     */
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Object> handleFileNotFoundException(FileNotFoundException ex, WebRequest request) {
        log.warn("File not found exception for request {}: {}", request.getDescription(false), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "请求的资源未找到"); // Provide a user-friendly message
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * 处理通用的 IO 异常 (例如文件读写、目录操作失败)
     * @param ex IOException
     * @param request WebRequest
     * @return ResponseEntity (500 Internal Server Error)
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Object> handleIOException(IOException ex, WebRequest request) {
        // Log IOExceptions that are not FileNotFoundException more seriously
        if (!(ex instanceof FileNotFoundException)) {
            log.error("IO exception occurred for request {}: {}", request.getDescription(false), ex.getMessage(), ex);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "文件操作时发生内部错误，请稍后重试或联系管理员。"); // Generic message for security
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理文件同步服务中的非法状态异常 (例如在错误状态下调用控制方法)
     * @param ex IllegalStateException (You might want a more specific custom exception)
     * @param request WebRequest
     * @return ResponseEntity (400 Bad Request or 409 Conflict)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        // Check if the exception message indicates a sync state issue
        if (ex.getMessage() != null && ex.getMessage().contains("sync") || ex.getMessage().contains("directory")) {
            log.warn("Illegal state exception related to file sync/config for request {}: {}", request.getDescription(false), ex.getMessage());
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.CONFLICT.value()); // 409 Conflict might be suitable for state issues
            body.put("error", "Conflict");
            body.put("message", ex.getMessage()); // Provide the specific reason
            return new ResponseEntity<>(body, HttpStatus.CONFLICT);
        } else {
            // Handle other IllegalStateExceptions as general server errors
            return handleGenericException(ex, request);
        }
    }


    // --- Generic Exception Handler (Catch-all) ---

    /**
     * 处理所有其他未捕获的异常
     * @param ex Exception
     * @param request WebRequest
     * @return ResponseEntity (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception occurred for request {}: {}", request.getDescription(false), ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "发生意外错误，请联系管理员。"); // Generic error message
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
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