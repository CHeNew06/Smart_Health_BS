package com.example.smart_health_management.health.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class HealthMetricRecordDto {
    private Long id;
    private String metricType;
    private BigDecimal value1;
    private BigDecimal value2;
    private String unit;
    private LocalDate recordDate;
    private LocalTime recordTime;
    private String notes;
    private String source;
    private String valueDisplay;  // 展示用，如 "120/80" 或 "72"
    private String status;        // normal/warn/high/low
    private String statusText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public LocalTime getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(LocalTime recordTime) {
        this.recordTime = recordTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getValueDisplay() {
        return valueDisplay;
    }

    public void setValueDisplay(String valueDisplay) {
        this.valueDisplay = valueDisplay;
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
}
