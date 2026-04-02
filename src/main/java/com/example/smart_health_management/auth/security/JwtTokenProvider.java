package com.example.smart_health_management.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.smart_health_management.auth.config.AuthProperties;
import com.example.smart_health_management.common.BizException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ACCOUNT = "account";
    private static final String CLAIM_JTI = "jti";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final AuthProperties authProperties;

    public JwtTokenProvider(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /** 生成 Access Token（短期，用于接口鉴权） */
    public String generateAccessToken(Long userId, String account) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(authProperties.getAccessTokenExpireMinutes(), ChronoUnit.MINUTES);
        return JWT.create()
                .withClaim(CLAIM_TYPE, TYPE_ACCESS)
                .withClaim(CLAIM_UID, userId)
                .withClaim(CLAIM_ACCOUNT, account)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expireAt))
                .sign(Algorithm.HMAC256(authProperties.getJwtSecret()));
    }

    /** 生成 Refresh Token（长期，用于刷新 Access Token），返回 token 与 jti（用于 Redis 存储） */
    public RefreshTokenResult generateRefreshToken(Long userId, String account) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(authProperties.getRefreshTokenExpireDays(), ChronoUnit.DAYS);
        String jti = UUID.randomUUID().toString();
        String token = JWT.create()
                .withClaim(CLAIM_TYPE, TYPE_REFRESH)
                .withClaim(CLAIM_UID, userId)
                .withClaim(CLAIM_ACCOUNT, account)
                .withClaim(CLAIM_JTI, jti)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expireAt))
                .sign(Algorithm.HMAC256(authProperties.getJwtSecret()));
        return new RefreshTokenResult(token, jti);
    }

    public static String getJti(DecodedJWT decoded) {
        return decoded.getClaim(CLAIM_JTI).asString();
    }

    public record RefreshTokenResult(String token, String jti) {}

    /** 验证 Access Token */
    public DecodedJWT verifyAccessToken(String token) {
        DecodedJWT decoded = verify(token);
        if (!TYPE_ACCESS.equals(decoded.getClaim(CLAIM_TYPE).asString())) {
            throw new BizException(401, "无效的 Access Token，请使用 Access Token 访问接口");
        }
        return decoded;
    }

    /** 验证 Refresh Token */
    public DecodedJWT verifyRefreshToken(String token) {
        DecodedJWT decoded = verify(token);
        if (!TYPE_REFRESH.equals(decoded.getClaim(CLAIM_TYPE).asString())) {
            throw new BizException(401, "无效的 Refresh Token");
        }
        return decoded;
    }

    /** 获取 token 剩余有效期（毫秒） */
    public long getRemainMs(DecodedJWT decoded) {
        return decoded.getExpiresAt().getTime() - System.currentTimeMillis();
    }

    /** 解码 token（不校验 type），用于 logout 等需区分类型的场景 */
    public DecodedJWT decode(String token) {
        return verify(token);
    }

    private DecodedJWT verify(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(authProperties.getJwtSecret()))
                    .build()
                    .verify(token);
        } catch (Exception e) {
            throw new BizException(401, "登录已过期或无效，请重新登录");
        }
    }
}

