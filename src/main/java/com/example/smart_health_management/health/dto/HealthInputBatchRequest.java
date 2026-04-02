package com.example.smart_health_management.health.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量录入健康数据请求。一次提交多个指标，共用同一测量时间和备注。
 * 支持前端传入 items 或 metrics 字段。
 */
public class HealthInputBatchRequest {

    @NotBlank(message = "测量日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
    private String recordDate;

    @Pattern(regexp = "^$|^\\d{1,2}:\\d{2}$", message = "时间格式应为 HH:mm")
    private String recordTime;

    private String notes;

    @NotEmpty(message = "至少需要提交一个指标")
    @Valid
    @JsonProperty("items")  // 前端传 items，后端用 metrics 接收
    private List<MetricItem> metrics;

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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<MetricItem> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricItem> metrics) {
        this.metrics = metrics;
    }

    /** 单条指标项（不含日期时间，由父级提供） */
    public static class MetricItem {
        @NotBlank(message = "指标类型不能为空")
        @Pattern(regexp = "^(bp|heartRate|temperature|bloodSugar|sleep|breath|weight|height)$",
                message = "指标类型无效")
        private String metricType;

        private BigDecimal value1;
        private BigDecimal value2;
        private BigDecimal bpHigh;
        private BigDecimal bpLow;
        private BigDecimal value;

        public String getMetricType() {
            return metricType;
        }

        public void setMetricType(String metricType) {
            this.metricType = metricType;
        }

        public BigDecimal getValue1() {
            return value1;
        }

        public void setValue1(BigDecimal value1) {
            this.value1 = value1;
        }

        public BigDecimal getValue2() {
            return value2;
        }

        public void setValue2(BigDecimal value2) {
            this.value2 = value2;
        }

        public BigDecimal getBpHigh() {
            return bpHigh;
        }

        public void setBpHigh(BigDecimal bpHigh) {
            this.bpHigh = bpHigh;
        }

        public BigDecimal getBpLow() {
            return bpLow;
        }

        public void setBpLow(BigDecimal bpLow) {
            this.bpLow = bpLow;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }
}
