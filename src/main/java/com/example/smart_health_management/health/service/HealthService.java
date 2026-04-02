package com.example.smart_health_management.health.service;

import com.example.smart_health_management.health.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HealthService {

    /** 手动录入健康数据 */
    void submitManualInput(Long userId, HealthInputRequest request);

    /** 批量录入健康数据（一次提交多个指标） */
    void submitBatchInput(Long userId, HealthInputBatchRequest request);

    /** 语音录入（解析 extractedData 批量插入） */
    void submitVoiceInput(Long userId, HealthInputVoiceRequest request);

    /** 获取指标汇总（首页展示） */
    List<HealthMetricsSummaryDto> getHealthMetrics(Long userId);

    /** 获取某类指标详情（最近 N 条记录） */
    HealthMetricDetailResponse getMetricDetail(Long userId, String metricType, Integer limit);

    /** 获取某类指标趋势（按日期范围） */
    HealthTrendDto getMetricTrend(Long userId, String metricType, LocalDate startDate, LocalDate endDate);

    /** 获取健康历史记录（混合所有类型） */
    List<HealthMetricRecordDto> getHealthHistory(Long userId, Integer limit);

    /** 获取健康评分（简化版：基于最近数据计算） */
    Map<String, Object> getHealthScore(Long userId);

    /** 获取今日健康建议（来自 Dify 工作流，用户提交数据时生成） */
    HealthAdviceResponse getHealthAdvice(Long userId);
}
