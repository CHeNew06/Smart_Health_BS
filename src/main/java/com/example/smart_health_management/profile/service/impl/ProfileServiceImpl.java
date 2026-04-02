package com.example.smart_health_management.profile.service.impl;

import com.example.smart_health_management.auth.mapper.UserAccountMapper;
import com.example.smart_health_management.auth.model.UserAccount;
import com.example.smart_health_management.common.BizException;
import com.example.smart_health_management.profile.dto.ProfileResponse;
import com.example.smart_health_management.profile.dto.ProfileUpdateRequest;
import com.example.smart_health_management.profile.mapper.UserProfileMapper;
import com.example.smart_health_management.profile.model.UserProfile;
import com.example.smart_health_management.profile.service.FileService;
import com.example.smart_health_management.profile.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileMapper profileMapper;
    private final UserAccountMapper userAccountMapper;
    private final FileService fileService;

    public ProfileServiceImpl(UserProfileMapper profileMapper,
                              UserAccountMapper userAccountMapper,
                              FileService fileService) {
        this.profileMapper = profileMapper;
        this.userAccountMapper = userAccountMapper;
        this.fileService = fileService;
    }

    @Override
    public ProfileResponse getProfile(Long userId) {
        UserAccount user = userAccountMapper.findById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        UserProfile profile = profileMapper.findByUserId(userId);
        return buildResponse(user, profile);
    }

    @Override
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        UserAccount user = userAccountMapper.findById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname());
            userAccountMapper.updateNickname(userId, request.getNickname());
        }

        UserProfile profile = profileMapper.findByUserId(userId);
        boolean isNew = (profile == null);
        if (isNew) {
            profile = new UserProfile();
            profile.setUserId(userId);
        }

        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            profile.setBirthday(parseBirthday(request.getBirthday()));
        }
        if (request.getRegion() != null) {
            profile.setRegion(request.getRegion());
        }
        if (request.getSignature() != null) {
            profile.setSignature(request.getSignature());
        }

        if (isNew) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateByUserId(profile);
        }

        return buildResponse(user, profile);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        UserAccount user = userAccountMapper.findById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        String avatarUrl = fileService.uploadAvatar(file);

        UserProfile profile = profileMapper.findByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setAvatar(avatarUrl);
            profileMapper.insert(profile);
        } else {
            profileMapper.updateAvatar(userId, avatarUrl);
        }

        return avatarUrl;
    }

    private ProfileResponse buildResponse(UserAccount user, UserProfile profile) {
        ProfileResponse resp = new ProfileResponse();
        resp.setUserId(user.getId());
        resp.setAccount(user.getAccount());
        resp.setEmail(user.getEmail());
        resp.setNickname(user.getNickname());
        if (profile != null) {
            resp.setAvatar(profile.getAvatar());
            resp.setGender(profile.getGender());
            resp.setBirthday(profile.getBirthday() != null ? profile.getBirthday().toString() : null);
            resp.setRegion(profile.getRegion());
            resp.setSignature(profile.getSignature());
        }
        return resp;
    }

    private LocalDate parseBirthday(String birthday) {
        if (birthday == null || birthday.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(birthday);
        } catch (DateTimeParseException e) {
            throw new BizException(400, "生日格式不正确，请使用 yyyy-MM-dd 格式");
        }
    }
}
