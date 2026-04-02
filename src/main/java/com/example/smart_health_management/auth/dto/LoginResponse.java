package com.example.smart_health_management.auth.dto;

public class LoginResponse {
    /** Access Token，短期有效，用于接口鉴权 */
    private String accessToken;
    /** Refresh Token，长期有效，用于刷新 Access Token */
    private String refreshToken;
    /** Access Token 剩余有效秒数 */
    private Long expiresIn;
    private Long userId;
    private String account;
    private String nickname;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

