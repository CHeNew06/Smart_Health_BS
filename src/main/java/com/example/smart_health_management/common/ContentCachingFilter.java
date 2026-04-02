package com.example.smart_health_management.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * 包装 HttpServletRequest 为 ContentCachingRequestWrapper，以便在请求完成后读取请求体用于日志记录。
 * 仅对 application/json 的 POST/PUT/PATCH 请求启用缓存，避免大文件上传占用内存。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentCachingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_CACHE_SIZE = 64 * 1024; // 64KB

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldWrap(request)) {
            ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, MAX_BODY_CACHE_SIZE);
            filterChain.doFilter(wrapped, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean shouldWrap(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("application/json");
    }
}
