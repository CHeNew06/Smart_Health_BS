package com.example.smart_health_management.profile.controller;

import com.example.smart_health_management.auth.security.AuthInterceptor;
import com.example.smart_health_management.common.ApiResponse;
import com.example.smart_health_management.profile.dto.ProfileResponse;
import com.example.smart_health_management.profile.dto.ProfileUpdateRequest;
import com.example.smart_health_management.profile.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(profileService.getProfile(userId));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest body,
                                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(profileService.updateProfile(userId, body));
    }

    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        String url = profileService.uploadAvatar(userId, file);
        return ApiResponse.success(Map.of("avatar", url));
    }
}
