package com.example.smart_health_management.auth.service;

import com.example.smart_health_management.auth.dto.LoginRequest;
import com.example.smart_health_management.auth.dto.LoginResponse;
import com.example.smart_health_management.auth.dto.RegisterRequest;
import com.example.smart_health_management.auth.dto.ResetPasswordRequest;
import com.example.smart_health_management.auth.dto.UserProfileResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserProfileResponse currentUser(Long userId);

    void register(RegisterRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(String token);

    /** 使用 Refresh Token 刷新，返回新的 Access Token 和 Refresh Token */
    LoginResponse refresh(String refreshToken);
}
