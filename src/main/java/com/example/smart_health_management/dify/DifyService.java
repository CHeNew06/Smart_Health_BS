package com.example.smart_health_management.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用 Dify 工作流 API 获取健康建议
 */
@Service
public class DifyService {

    private static final Logger log = LoggerFactory.getLogger(DifyService.class);
    private static final String WORKFLOW_RUN_PATH = "/workflows/run";

    private final DifyProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DifyService(DifyProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 Dify 工作流获取健康建议
     *
     * @param healthData 七项健康指标 JSON 对象
     * @param userId     用户 ID 用于 user 参数
     * @return 工作流输出，包含 total_score 和 suggestions，失败返回 null
     */
    public DifyAdviceResult runHealthAdviceWorkflow(Map<String, Object> healthData, Long userId) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("Dify API key 未配置，跳过健康建议生成");
            return null;
        }

        String url = properties.getApiBaseUrl().replaceAll("/$", "") + WORKFLOW_RUN_PATH;

        Map<String, Object> requestBody = new HashMap<>();
        String inputVar = properties.getInputVariableName() != null ? properties.getInputVariableName() : "Health_data";
        requestBody.put("inputs", Map.of(inputVar, healthData));
        requestBody.put("response_mode", "blocking");
        requestBody.put("user", "user_" + userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String respBody = response.getBody();
                log.info("[Dify] 收到响应, 长度={} 字符", respBody != null ? respBody.length() : 0);
                return parseWorkflowResponse(respBody);
            } else {
                log.warn("Dify 工作流调用失败: status={}, body={}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("Dify 工作流调用异常", e);
            return null;
        }
    }

    private DifyAdviceResult parseWorkflowResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            log.info("[Dify] 根节点 keys: {}", root.isObject() ? toCommaNames(root) : "非对象");
            JsonNode data = root.get("data");
            if (data == null) {
                log.warn("[Dify] 响应无 data 字段, body 前 500 字符: {}", body != null && body.length() > 500 ? body.substring(0, 500) + "..." : body);
                return null;
            }
            String status = data.has("status") ? data.get("status").asText() : "";
            log.info("[Dify] data.status={}, data.keys={}", status, data.isObject() ? toCommaNames(data) : "n/a");
            if (!"succeeded".equals(status)) {
                log.warn("[Dify] 工作流未成功: status={}, error={}", status, data.has("error") ? data.get("error").asText() : "");
                return null;
            }
            JsonNode outputs = data.get("outputs");
            if (outputs == null || outputs.isNull()) {
                log.warn("[Dify] outputs 为空");
                return null;
            }
            logOutputsStructure(outputs);

            JsonNode resultRoot = findResultNode(outputs);
            if (resultRoot == null) {
                log.warn("[Dify] outputs 中未找到 total_score, outputs 完整内容(前 1500 字符): {}", truncate(outputs.toString(), 1500));
                return null;
            }

            DifyAdviceResult result = new DifyAdviceResult();
            JsonNode scoreNode = resultRoot.get("total_score");
            if (scoreNode != null && !scoreNode.isNull()) {
                result.setTotalScore(scoreNode.isNumber() ? scoreNode.asDouble() : Double.parseDouble(scoreNode.asText()));
            }
            JsonNode suggestionsNode = resultRoot.get("suggestions");
            if (suggestionsNode != null && suggestionsNode.isObject()) {
                result.setDiet(safeText(suggestionsNode.get("diet")));
                result.setExercise(safeText(suggestionsNode.get("exercise")));
                result.setLifestyle(safeText(suggestionsNode.get("lifestyle")));
                result.setMedical(safeText(suggestionsNode.get("medical")));
            }
            log.info("[Dify] 解析成功: total_score={}, diet_len={}, exercise_len={}, lifestyle_len={}, medical_len={}",
                    result.getTotalScore(),
                    result.getDiet() != null ? result.getDiet().length() : 0,
                    result.getExercise() != null ? result.getExercise().length() : 0,
                    result.getLifestyle() != null ? result.getLifestyle().length() : 0,
                    result.getMedical() != null ? result.getMedical().length() : 0);
            return result;
        } catch (Exception e) {
            log.error("[Dify] 解析异常, body 前 800 字符: {}", truncate(body, 800), e);
            return null;
        }
    }

    /** 从 outputs 中定位包含 total_score 的节点。官方文档：outputs 为 End 节点定义的输出变量，keys 由工作流决定 */
    private JsonNode findResultNode(JsonNode outputs) {
        if (outputs == null || !outputs.isObject()) return null;
        if (outputs.has("total_score") && outputs.has("suggestions")) {
            log.info("[Dify] findResultNode: 命中「outputs 直接包含 total_score」");
            return outputs;
        }
        var it = outputs.fields();
        while (it.hasNext()) {
            var entry = it.next();
            String key = entry.getKey();
            JsonNode child = entry.getValue();
            if (child == null) continue;
            if (child.isObject() && child.has("total_score")) {
                log.info("[Dify] findResultNode: 命中「outputs.{} 为对象且含 total_score」", key);
                return child;
            }
            if (child.isTextual()) {
                String text = child.asText();
                log.info("[Dify] findResultNode: outputs.{} 为 string, len={}, head={}", key, text != null ? text.length() : 0, truncate(text, 100));
                JsonNode parsed = parseJsonFromString(text);
                if (parsed != null) {
                    log.info("[Dify] findResultNode: 从 outputs.{} 的 string 解析出 JSON 成功", key);
                    return parsed;
                }
            }
            if (child.isObject()) {
                for (String subKey : new String[]{"result", "text", "output", "content", "answer"}) {
                    if (child.has(subKey)) {
                        JsonNode val = child.get(subKey);
                        if (val != null && val.isTextual()) {
                            String text = val.asText();
                            log.info("[Dify] findResultNode: outputs.{}.{} 为 string, len={}, head={}", key, subKey, text != null ? text.length() : 0, truncate(text, 100));
                            JsonNode parsed = parseJsonFromString(text);
                            if (parsed != null) {
                                log.info("[Dify] findResultNode: 从 outputs.{}.{} 解析出 JSON 成功", key, subKey);
                                return parsed;
                            }
                        }
                    }
                }
            }
        }
        log.info("[Dify] findResultNode: 遍历完毕未找到 total_score");
        return null;
    }

    /** 从字符串解析 JSON，支持裸 JSON、```json...``` 或 ```...``` 包裹 */
    private JsonNode parseJsonFromString(String text) {
        if (text == null || text.isBlank()) {
            log.debug("[Dify] parseJsonFromString: 输入为空");
            return null;
        }
        text = text.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            if (start > 0) text = text.substring(start + 1);
            int end = text.lastIndexOf("```");
            if (end > 0) text = text.substring(0, end).trim();
            log.debug("[Dify] parseJsonFromString: 去除 markdown 代码块后 len={}", text.length());
        }
        if (!text.startsWith("{")) {
            log.debug("[Dify] parseJsonFromString: 非 JSON 对象 (head={})", truncate(text, 50));
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(text);
            boolean hasScore = parsed != null && parsed.has("total_score");
            if (!hasScore) {
                log.debug("[Dify] parseJsonFromString: 解析成功但无 total_score, keys={}", parsed != null && parsed.isObject() ? toCommaNames(parsed) : "n/a");
            }
            return hasScore ? parsed : null;
        } catch (Exception e) {
            log.debug("[Dify] parseJsonFromString: JSON 解析异常 - {}", e.getMessage());
            return null;
        }
    }

    private static String safeText(JsonNode node) {
        return node != null && !node.isNull() ? node.asText("") : "";
    }

    private static String toCommaNames(JsonNode node) {
        if (node == null || !node.isObject()) return "";
        var list = new ArrayList<String>();
        node.fields().forEachRemaining(e -> list.add(e.getKey()));
        return String.join(", ", list);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private void logOutputsStructure(JsonNode outputs) {
        if (outputs == null || !outputs.isObject()) return;
        var sb = new StringBuilder();
        var it = outputs.fields();
        while (it.hasNext()) {
            var e = it.next();
            String k = e.getKey();
            JsonNode v = e.getValue();
            String type = v == null ? "null" : (v.isTextual() ? "string" : v.isObject() ? "object" : v.isArray() ? "array" : v.getNodeType().toString());
            String preview = "";
            if (v != null && v.isTextual()) {
                String t = v.asText();
                preview = " (len=" + (t != null ? t.length() : 0) + ", head=" + truncate(t, 80) + ")";
            } else if (v != null && v.isObject()) {
                preview = " keys=" + toCommaNames(v);
            }
            if (sb.length() > 0) sb.append("; ");
            sb.append(k).append(":").append(type).append(preview);
        }
        log.info("[Dify] outputs 结构: {}", sb.toString());
    }

    public static class DifyAdviceResult {
        private Double totalScore;
        private String diet;
        private String exercise;
        private String lifestyle;
        private String medical;

        public Double getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(Double totalScore) {
            this.totalScore = totalScore;
        }

        public String getDiet() {
            return diet;
        }

        public void setDiet(String diet) {
            this.diet = diet;
        }

        public String getExercise() {
            return exercise;
        }

        public void setExercise(String exercise) {
            this.exercise = exercise;
        }

        public String getLifestyle() {
            return lifestyle;
        }

        public void setLifestyle(String lifestyle) {
            this.lifestyle = lifestyle;
        }

        public String getMedical() {
            return medical;
        }

        public void setMedical(String medical) {
            this.medical = medical;
        }
    }
}
