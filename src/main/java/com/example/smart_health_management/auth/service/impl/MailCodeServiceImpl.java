package com.example.smart_health_management.auth.service.impl;

import com.example.smart_health_management.auth.service.MailCodeService;
import com.example.smart_health_management.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MailCodeServiceImpl implements MailCodeService {

    private static final long CODE_EXPIRE_MINUTES = 5;
    private static final long RATE_LIMIT_SECONDS = 60;
    private static final int CODE_LENGTH = 6;

    private static final Map<String, String> SCENE_LABELS = Map.of(
            SCENE_REGISTER, "注册",
            SCENE_LOGIN, "登录",
            SCENE_RESET, "重置密码"
    );

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public MailCodeServiceImpl(StringRedisTemplate redisTemplate, JavaMailSender mailSender) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
    }

    @Override
    public void sendCode(String email, String scene) {
        String rateLimitKey = "mail:rate:" + scene + ":" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BizException(429, "发送过于频繁，请60秒后再试");
        }

        String code = generateCode();
        String codeKey = "mail:code:" + scene + ":" + email;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);

        String label = SCENE_LABELS.getOrDefault(scene, "操作");
        sendMail(email, code, label);
    }

    @Override
    public boolean verifyCode(String email, String code, String scene) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String codeKey = "mail:code:" + scene + ":" + email;
        String cached = redisTemplate.opsForValue().get(codeKey);
        if (cached != null && cached.equals(code.trim())) {
            redisTemplate.delete(codeKey);
            return true;
        }
        return false;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void sendMail(String to, String code, String label) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(label + "验证码 - " + fromName);
            String content = """
                    <div style="max-width:480px;margin:0 auto;padding:32px;font-family:'Microsoft YaHei',sans-serif;background:#f7f8fc;border-radius:12px;">
                      <h2 style="color:#2563eb;margin:0 0 16px;">%s</h2>
                      <p style="color:#333;font-size:15px;">您好，您正在进行<strong>%s</strong>操作，验证码为：</p>
                      <div style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#2563eb;
                                  background:#fff;border-radius:8px;padding:16px 24px;margin:16px 0;
                                  text-align:center;border:1px solid #e5e7eb;">%s</div>
                      <p style="color:#666;font-size:13px;">验证码有效期为 <strong>5分钟</strong>，请尽快完成操作。</p>
                      <p style="color:#999;font-size:12px;margin-top:24px;">如非本人操作，请忽略此邮件。</p>
                    </div>
                    """.formatted(fromName, label, code);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new BizException(500, "验证码邮件发送失败，请稍后重试");
        }
    }
}
