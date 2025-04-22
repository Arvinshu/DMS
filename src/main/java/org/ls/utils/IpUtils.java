package org.ls.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 获取客户端 IP 地址的工具类
 */
public class IpUtils {

    private static final Logger log = LoggerFactory.getLogger(IpUtils.class);
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    /**
     * 获取客户端真实 IP 地址
     * 考虑了代理服务器和反向代理的情况 (X-Forwarded-For, Proxy-Client-IP, WL-Proxy-Client-IP, HTTP_CLIENT_IP, HTTP_X_FORWARDED_FOR)
     *
     * @param request HttpServletRequest 对象
     * @return 客户端 IP 地址字符串，获取失败返回 "unknown"
     */
    public static String getClientIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (isIpInvalid(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (isIpInvalid(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isIpInvalid(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isIpInvalid(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isIpInvalid(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (LOCALHOST_IPV4.equals(ipAddress) || LOCALHOST_IPV6.equals(ipAddress)) {
                // 根据网卡取本机配置的 IP
                try {
                    InetAddress inetAddress = InetAddress.getLocalHost();
                    ipAddress = inetAddress.getHostAddress();
                } catch (UnknownHostException e) {
                    log.warn("获取本地主机 IP 地址失败: {}", e.getMessage());
                    // 保留 127.0.0.1 或 ::1
                }
            }
        }

        // 对于通过多个代理的情况，第一个 IP 为客户端真实 IP，多个 IP 按照 ',' 分割
        // "***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.length() > 15) {
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress == null ? UNKNOWN : ipAddress;
    }

    /**
     * 检查 IP 地址字符串是否无效
     *
     * @param ipAddress IP 地址字符串
     * @return 如果 IP 为 null、空字符串或 "unknown" (忽略大小写)，则返回 true，否则返回 false
     */
    private static boolean isIpInvalid(String ipAddress) {
        return ipAddress == null || ipAddress.isEmpty() || UNKNOWN.equalsIgnoreCase(ipAddress);
    }
}
