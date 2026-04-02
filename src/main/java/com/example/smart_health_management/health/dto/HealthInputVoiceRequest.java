package com.example.smart_health_management.health.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 语音录入请求。前端可传入语音识别文本和 AI 提取的指标列表。
 */
public class HealthInputVoiceRequest {

    @NotBlank(message = "语音识别结果不能为空")
    private String voiceResult;

    /** AI 提取的指标，每项含 type、value、label 等 */
    private List<ExtractedMetric> extractedData;

    private String recordDate;
    private String recordTime;

    public String getVoiceResult() {
        return voiceResult;
    }

    public void setVoiceResult(String voiceResult) {
        this.voiceResult = voiceResult;
    }

    public List<ExtractedMetric> getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(List<ExtractedMetric> extractedData) {
        this.extractedData = extractedData;
    }

    public String getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(String recordDate) {
        this.recordDate = recordDate;
    }

    public String getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(String recordTime) {
        this.recordTime = recordTime;
    }

    public static class ExtractedMetric {
        private String type;   // bp, heartRate, temperature, bloodSugar, sleep, breath, weight, height
        private Object value; // 单值或 {high, low} 血压
        private String label;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
