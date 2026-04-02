package com.example.smart_health_management.auth.controller;

import com.example.smart_health_management.auth.dto.LoginRequest;
import com.example.smart_health_management.auth.dto.LoginResponse;
import com.example.smart_health_management.auth.dto.RefreshTokenRequest;
import com.example.smart_health_management.auth.dto.RegisterRequest;
import com.example.smart_health_management.auth.dto.ResetPasswordRequest;
import com.example.smart_health_management.auth.dto.SendCodeRequest;
import com.example.smart_health_management.auth.dto.UserProfileResponse;
import com.example.smart_health_management.auth.security.AuthInterceptor;
import com.example.smart_health_management.auth.service.AuthService;
import com.example.smart_health_management.auth.service.MailCodeService;
import com.example.smart_health_management.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MailCodeService mailCodeService;

    public AuthController(AuthService authService, MailCodeService mailCodeService) {
        this.authService = authService;
        this.mailCodeService = mailCodeService;
    }

    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        mailCodeService.sendCode(request.getEmail(), request.getScene());
        return ApiResponse.success(null);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, @RequestBody(required = false) RefreshTokenRequest body) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            authService.logout(body.getRefreshToken());
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> currentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(authService.currentUser(userId));
    }
}
