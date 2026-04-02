package com.example.smart_health_management.health.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 指标趋势数据：用于图表展示。
 */
public class HealthTrendDto {
    private String type;
    private String refText;        // 参考范围文案
    private BigDecimal avg;
    private BigDecimal max;
    private BigDecimal min;
    private List<String> labels;   // X 轴标签（日期）
    private List<BigDecimal> values; // 数值序列
    private String analysis;       // 趋势分析文案

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    public BigDecimal getAvg() {
        return avg;
    }

    public void setAvg(BigDecimal avg) {
        this.avg = avg;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<BigDecimal> getValues() {
        return values;
    }

    public void setValues(List<BigDecimal> values) {
        this.values = values;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
}
