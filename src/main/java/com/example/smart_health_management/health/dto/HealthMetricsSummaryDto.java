package com.example.smart_health_management.health.dto;

import java.math.BigDecimal;

/**
 * 首页/指标汇总展示：各指标最新值及状态。
 */
public class HealthMetricsSummaryDto {
    private String type;           // temperature, bp, bloodSugar, bmi, heartRate, sleep
    private String label;          // 指标名称
    private String value;          // 展示值，如 "36.5" 或 "120/80"
    private String unit;          // 单位
    private String status;         // normal/warn/harn/danger
    private String statusText;     // 状态文案
    private BigDecimal value1;     // 原始数值（用于排序、计算）
    private BigDecimal value2;     // 血压舒张压

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
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
}
