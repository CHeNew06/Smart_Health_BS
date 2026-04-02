package com.example.smart_health_management.profile.service;

import com.example.smart_health_management.profile.dto.ProfileResponse;
import com.example.smart_health_management.profile.dto.ProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    ProfileResponse getProfile(Long userId);

    ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request);

    String uploadAvatar(Long userId, MultipartFile file);
}
