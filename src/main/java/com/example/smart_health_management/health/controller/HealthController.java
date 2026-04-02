package com.example.smart_health_management.health.controller;

import com.example.smart_health_management.auth.security.AuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.smart_health_management.common.ApiResponse;
import com.example.smart_health_management.health.dto.*;
import com.example.smart_health_management.health.service.HealthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
        log.info("HealthController 已注册: POST /api/health/input/manual, POST /api/health/input/voice, GET /api/health/metrics 等");
    }

    /** 手动录入健康数据 */
    @PostMapping("/input/manual")
    public ApiResponse<Void> submitManualInput(@Valid @RequestBody HealthInputRequest request,
                                               HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        healthService.submitManualInput(userId, request);
        return ApiResponse.success(null);
    }

    /** 批量录入健康数据（一次提交多个指标） */
    @PostMapping("/input/batch")
    public ApiResponse<Void> submitBatchInput(@Valid @RequestBody HealthInputBatchRequest request,
                                              HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        healthService.submitBatchInput(userId, request);
        return ApiResponse.success(null);
    }

    /** 语音录入健康数据 */
    @PostMapping("/input/voice")
    public ApiResponse<Void> submitVoiceInput(@Valid @RequestBody HealthInputVoiceRequest request,
                                               HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        healthService.submitVoiceInput(userId, request);
        return ApiResponse.success(null);
    }

    /** 获取健康指标汇总（首页展示） */
    @GetMapping("/metrics")
    public ApiResponse<List<HealthMetricsSummaryDto>> getHealthMetrics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(healthService.getHealthMetrics(userId));
    }

    /** 获取某类指标详情 */
    @GetMapping("/metrics/{type}")
    public ApiResponse<HealthMetricDetailResponse> getMetricDetail(
            @PathVariable String type,
            @RequestParam(required = false, defaultValue = "7") Integer limit,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(healthService.getMetricDetail(userId, type, limit));
    }

    /** 获取某类指标趋势 */
    @GetMapping("/metrics/{type}/trend")
    public ApiResponse<HealthTrendDto> getMetricTrend(
            @PathVariable String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(healthService.getMetricTrend(userId, type, startDate, endDate));
    }

    /** 获取健康历史记录 */
    @GetMapping("/history")
    public ApiResponse<List<HealthMetricRecordDto>> getHealthHistory(
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(healthService.getHealthHistory(userId, limit));
    }

    /** 获取健康评分 */
    @GetMapping("/score")
    public ApiResponse<Map<String, Object>> getHealthScore(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(healthService.getHealthScore(userId));
    }

    /** 获取今日健康建议 */
    @GetMapping("/advice")
    public ApiResponse<HealthAdviceResponse> getHealthAdvice(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_CURRENT_USER_ID);
        return ApiResponse.success(healthService.getHealthAdvice(userId));
    }
}
