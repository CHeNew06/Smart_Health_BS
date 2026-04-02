package com.example.smart_health_management.health.dto;

import java.util.List;

/**
 * 某类指标详情：含最新值、状态、最近记录列表。
 */
public class HealthMetricDetailResponse {
    private String type;
    private String title;
    private String unit;
    private String refRange;
    private String value;         // 当前值展示
    private String statusText;
    private String statusClass;   // normal/warn/danger
    private List<HealthMetricRecordDto> records;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRefRange() {
        return refRange;
    }

    public void setRefRange(String refRange) {
        this.refRange = refRange;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public void setStatusClass(String statusClass) {
        this.statusClass = statusClass;
    }

    public List<HealthMetricRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<HealthMetricRecordDto> records) {
        this.records = records;
    }
}
