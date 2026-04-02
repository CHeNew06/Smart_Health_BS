package com.example.smart_health_management.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * 手动录入健康数据请求。
 * 支持两种格式：
 * 1. 血压：metricType=bp, value1=收缩压, value2=舒张压
 * 2. 其他：metricType=xxx, value1=数值（value2 可为空）
 * 前端也可传 bpHigh/bpLow 或 singleValue，由 Service 转换。
 */
public class HealthInputRequest {

    @NotBlank(message = "指标类型不能为空")
    @Pattern(regexp = "^(bp|heartRate|temperature|bloodSugar|sleep|breath|weight|height)$",
            message = "指标类型无效")
    private String metricType;

    /** 主值（血压时为收缩压，其他为单值） */
    private BigDecimal value1;
    /** 副值（仅血压时为舒张压） */
    private BigDecimal value2;

    /** 兼容前端：血压收缩压 */
    private BigDecimal bpHigh;
    /** 兼容前端：血压舒张压 */
    private BigDecimal bpLow;
    /** 兼容前端：单值指标 */
    private BigDecimal value;

    @NotBlank(message = "测量日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
    private String recordDate;

    /** 测量时间 HH:mm，可选 */
    @Pattern(regexp = "^$|^\\d{1,2}:\\d{2}$", message = "时间格式应为 HH:mm")
    private String recordTime;

    private String notes;

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
}
