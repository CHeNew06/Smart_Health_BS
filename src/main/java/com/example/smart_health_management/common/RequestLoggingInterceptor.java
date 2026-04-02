package com.example.smart_health_management.common;

import com.example.smart_health_management.auth.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String ATTR_START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        String query = request.getQueryString();
        String path = query == null || query.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + query;

        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String contentType = request.getContentType();
        boolean hasAuth = request.getHeader("Authorization") != null && !request.getHeader("Authorization").isBlank();

        log.info(">>> [请求进入] method={}, path={}, clientIp={}, contentType={}, hasAuth={}, userAgent={}",
                request.getMethod(),
                path,
                clientIp,
                contentType != null ? contentType : "-",
                hasAuth,
                userAgent != null && !userAgent.isBlank() ? truncate(userAgent, 80) : "-");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = 0L;
        Object attr = request.getAttribute(ATTR_START_TIME);
        if (attr instanceof Long value) {
            startTime = value;
        }

        long duration = startTime == 0L ? 0L : System.currentTimeMillis() - startTime;
        String query = request.getQueryString();
        String path = query == null || query.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + query;
        Object userId = request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);

        String bodyLog = getRequestBodyForLog(request);

        if (ex != null) {
            log.error("<<< [请求失败] method={}, path={}, status={}, duration={}ms, userId={}, body={}, error={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    duration,
                    userId,
                    bodyLog,
                    ex.getMessage(),
                    ex);
            return;
        }

        log.info("<<< [请求完成] method={}, path={}, status={}, duration={}ms, userId={}, body={}",
                request.getMethod(),
                path,
                response.getStatus(),
                duration,
                userId != null ? userId : "-",
                bodyLog);
    }

    private String getRequestBodyForLog(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return "-";
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return "-";
        }
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            return "[非JSON，已略过]";
        }
        String body = new String(content, java.nio.charset.StandardCharsets.UTF_8);
        return SensitiveDataMasker.mask(body);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "-";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
