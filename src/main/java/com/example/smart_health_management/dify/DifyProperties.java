package com.example.smart_health_management.dify;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dify")
public class DifyProperties {

    private String apiBaseUrl = "https://api.dify.ai/v1";
    private String apiKey;
    /** 工作流 Start 节点的输入变量名，需与 Dify 工作流中定义的一致 */
    private String inputVariableName = "Health_data";

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getInputVariableName() {
        return inputVariableName;
    }

    public void setInputVariableName(String inputVariableName) {
        this.inputVariableName = inputVariableName;
    }
}
