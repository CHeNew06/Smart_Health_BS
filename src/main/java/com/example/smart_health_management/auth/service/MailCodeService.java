package com.example.smart_health_management.auth.service;

public interface MailCodeService {

    void sendCode(String email, String scene);

    boolean verifyCode(String email, String code, String scene);

    /** 场景常量 */
    String SCENE_REGISTER = "register";
    String SCENE_LOGIN = "login";
    String SCENE_RESET = "reset";
}
