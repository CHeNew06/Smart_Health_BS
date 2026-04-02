package com.example.smart_health_management.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String jwtSecret;
    /** @deprecated 已废弃，使用 accessTokenExpireMinutes */
    private Long jwtExpireHours = 168L;
    /** Access Token 有效期（分钟），默认 30 分钟 */
    private Long accessTokenExpireMinutes = 30L;
    /** Refresh Token 有效期（天），默认 7 天 */
    private Long refreshTokenExpireDays = 7L;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Deprecated
    public Long getJwtExpireHours() {
        return jwtExpireHours;
    }

    @Deprecated
    public void setJwtExpireHours(Long jwtExpireHours) {
        this.jwtExpireHours = jwtExpireHours;
    }

    public Long getAccessTokenExpireMinutes() {
        return accessTokenExpireMinutes;
    }

    public void setAccessTokenExpireMinutes(Long accessTokenExpireMinutes) {
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
    }

    public Long getRefreshTokenExpireDays() {
        return refreshTokenExpireDays;
    }

    public void setRefreshTokenExpireDays(Long refreshTokenExpireDays) {
        this.refreshTokenExpireDays = refreshTokenExpireDays;
    }
}

