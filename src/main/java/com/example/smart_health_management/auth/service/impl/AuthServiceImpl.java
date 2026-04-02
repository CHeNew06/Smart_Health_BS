package com.example.smart_health_management.auth.service.impl;

import com.example.smart_health_management.auth.dto.LoginRequest;
import com.example.smart_health_management.auth.dto.LoginResponse;
import com.example.smart_health_management.auth.dto.RegisterRequest;
import com.example.smart_health_management.auth.dto.ResetPasswordRequest;
import com.example.smart_health_management.auth.dto.UserProfileResponse;
import com.example.smart_health_management.auth.mapper.UserAccountMapper;
import com.example.smart_health_management.auth.model.UserAccount;
import com.example.smart_health_management.auth.config.AuthProperties;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.smart_health_management.auth.security.JwtTokenProvider;
import com.example.smart_health_management.auth.security.JwtTokenProvider.RefreshTokenResult;
import com.example.smart_health_management.auth.service.AuthService;
import com.example.smart_health_management.auth.service.MailCodeService;
import com.example.smart_health_management.common.BizException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final UserAccountMapper userAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthProperties authProperties;
    private final MailCodeService mailCodeService;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserAccountMapper userAccountMapper,
                           JwtTokenProvider jwtTokenProvider,
                           AuthProperties authProperties,
                           MailCodeService mailCodeService,
                           StringRedisTemplate redisTemplate) {
        this.userAccountMapper = userAccountMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authProperties = authProperties;
        this.mailCodeService = mailCodeService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String input = request.getAccountOrEmail();
        boolean hasPassword = request.getPassword() != null && !request.getPassword().isBlank();
        boolean hasCode = request.getCode() != null && !request.getCode().isBlank();

        if (!hasPassword && !hasCode) {
            throw new BizException(400, "密码和验证码不能同时为空");
        }

        UserAccount user = resolveUser(input);
        if (user == null) {
            throw new BizException(401, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }

        if (hasCode) {
            if (!mailCodeService.verifyCode(user.getEmail(), request.getCode(), MailCodeService.SCENE_LOGIN)) {
                throw new BizException(401, "验证码错误或已过期");
            }
        } else {
            if (!passwordMatched(request.getPassword(), user.getPasswordHash())) {
                throw new BizException(401, "账号或密码错误");
            }
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getAccount());
        RefreshTokenResult refreshResult = jwtTokenProvider.generateRefreshToken(user.getId(), user.getAccount());
        long refreshTtlMs = jwtTokenProvider.getRemainMs(jwtTokenProvider.verifyRefreshToken(refreshResult.token()));
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + refreshResult.jti(), String.valueOf(user.getId()),
                refreshTtlMs, TimeUnit.MILLISECONDS);

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setAccount(user.getAccount());
        response.setNickname(user.getNickname());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshResult.token());
        response.setExpiresIn(authProperties.getAccessTokenExpireMinutes() * 60L);
        return response;
    }

    @Override
    public UserProfileResponse currentUser(Long userId) {
        UserAccount user = userAccountMapper.findById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getId());
        response.setAccount(user.getAccount());
        response.setNickname(user.getNickname());
        return response;
    }

    @Override
    public void register(RegisterRequest request) {
        if (!mailCodeService.verifyCode(request.getEmail(), request.getCode(), MailCodeService.SCENE_REGISTER)) {
            throw new BizException(400, "验证码错误或已过期");
        }
        if (userAccountMapper.findByAccount(request.getAccount()) != null) {
            throw new BizException(400, "账号已被注册");
        }
        if (userAccountMapper.findByEmail(request.getEmail()) != null) {
            throw new BizException(400, "该邮箱已被注册");
        }

        UserAccount user = new UserAccount();
        user.setAccount(request.getAccount());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getAccount());
        userAccountMapper.insert(user);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!mailCodeService.verifyCode(request.getEmail(), request.getCode(), MailCodeService.SCENE_RESET)) {
            throw new BizException(400, "验证码错误或已过期");
        }
        UserAccount user = userAccountMapper.findByEmail(request.getEmail());
        if (user == null) {
            throw new BizException(404, "该邮箱未注册");
        }
        userAccountMapper.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    public void logout(String token) {
        try {
            var decoded = jwtTokenProvider.decode(token);
            String type = decoded.getClaim("type").asString();
            if ("access".equals(type)) {
                long remainMs = decoded.getExpiresAt().getTime() - System.currentTimeMillis();
                if (remainMs > 0) {
                    redisTemplate.opsForValue().set(
                            TOKEN_BLACKLIST_PREFIX + token, "1", remainMs, TimeUnit.MILLISECONDS);
                }
            } else if ("refresh".equals(type)) {
                String jti = JwtTokenProvider.getJti(decoded);
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + jti);
            }
        } catch (Exception ignored) {
            // token 已失效无需处理
        }
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        DecodedJWT decoded = jwtTokenProvider.verifyRefreshToken(refreshToken);
        String jti = JwtTokenProvider.getJti(decoded);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + jti))) {
            throw new BizException(401, "Refresh Token 已失效，请重新登录");
        }
        Long userId = decoded.getClaim("uid").asLong();
        String account = decoded.getClaim("account").asString();
        UserAccount user = userAccountMapper.findById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "用户不存在或已被禁用");
        }
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + jti);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getAccount());
        RefreshTokenResult refreshResult = jwtTokenProvider.generateRefreshToken(user.getId(), user.getAccount());
        long refreshTtlMs = jwtTokenProvider.getRemainMs(jwtTokenProvider.verifyRefreshToken(refreshResult.token()));
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + refreshResult.jti(), String.valueOf(user.getId()),
                refreshTtlMs, TimeUnit.MILLISECONDS);

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setAccount(user.getAccount());
        response.setNickname(user.getNickname());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshResult.token());
        response.setExpiresIn(authProperties.getAccessTokenExpireMinutes() * 60L);
        return response;
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }

    private UserAccount resolveUser(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        if (input.contains("@")) {
            return userAccountMapper.findByEmail(input);
        }
        return userAccountMapper.findByAccount(input);
    }

    private boolean passwordMatched(String rawPassword, String dbPassword) {
        if (rawPassword == null || dbPassword == null || dbPassword.isBlank()) {
            return false;
        }
        if (dbPassword.startsWith("$2a$") || dbPassword.startsWith("$2b$") || dbPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, dbPassword);
        }
        return rawPassword.equals(dbPassword);
    }
}
