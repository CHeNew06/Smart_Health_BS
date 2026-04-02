package com.example.smart_health_management.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;

/**
 * 对 JSON 中的敏感字段进行屏蔽，避免密码、验证码、token 等泄露到日志。
 */
public final class SensitiveDataMasker {

    private static final String MASK = "****";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "newpassword", "oldpassword", "confirmpassword",
            "new_password", "old_password", "confirm_password",
            "code", "verificationcode", "verify_code",
            "refreshtoken", "refresh_token", "accesstoken", "access_token",
            "token", "secret", "apikey", "api_key"
    );

    private SensitiveDataMasker() {
    }

    /**
     * 对 JSON 字符串中的敏感字段进行屏蔽后返回。若解析失败则返回原始字符串的脱敏摘要。
     */
    public static String mask(String json) {
        if (json == null || json.isBlank()) {
            return "-";
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            maskNode(root);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "[parse failed, len=" + json.length() + "]";
        }
    }

    private static void maskNode(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (isSensitive(key)) {
                    obj.put(key, MASK);
                } else {
                    maskNode(entry.getValue());
                }
            });
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                maskNode(arr.get(i));
            }
        }
    }

    private static boolean isSensitive(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase().replace("_", "").replace("-", "");
        return SENSITIVE_KEYS.stream().anyMatch(sk -> lower.contains(sk.replace("_", "")));
    }
}
