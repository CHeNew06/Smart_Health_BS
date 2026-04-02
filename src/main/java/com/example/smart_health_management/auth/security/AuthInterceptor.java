package com.example.smart_health_management.auth.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.smart_health_management.auth.service.impl.AuthServiceImpl;
import com.example.smart_health_management.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_CURRENT_USER_ID = "currentUserId";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthServiceImpl authService;

    public AuthInterceptor(JwtTokenProvider jwtTokenProvider, AuthServiceImpl authService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(401, "请先登录");
        }
        String token = authHeader.substring(7);

        if (authService.isTokenBlacklisted(token)) {
            throw new BizException(401, "登录已失效，请重新登录");
        }

        DecodedJWT decodedJWT = jwtTokenProvider.verifyAccessToken(token);
        Long userId = decodedJWT.getClaim("uid").asLong();
        if (userId == null) {
            throw new BizException(401, "无效登录凭证");
        }
        request.setAttribute(ATTR_CURRENT_USER_ID, userId);
        return true;
    }
}
