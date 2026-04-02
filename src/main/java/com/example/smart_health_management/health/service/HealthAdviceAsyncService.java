package com.example.smart_health_management.health.service;

import com.example.smart_health_management.dify.DifyService;
import com.example.smart_health_management.dify.DifyService.DifyAdviceResult;
import com.example.smart_health_management.health.mapper.HealthAdviceMapper;
import com.example.smart_health_management.health.mapper.HealthMetricMapper;
import com.example.smart_health_management.health.mapper.HealthScoreMapper;
import com.example.smart_health_management.health.model.HealthAdvice;
import com.example.smart_health_management.health.model.HealthMetric;
import com.example.smart_health_management.health.model.HealthScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步调用 Dify 生成健康建议，避免阻塞用户提交（Dify 工作流可能需 1-2 分钟）
 */
@Service
public class HealthAdviceAsyncService {

    private static final Logger log = LoggerFactory.getLogger(HealthAdviceAsyncService.class);

    private final HealthMetricMapper healthMetricMapper;
    private final HealthAdviceMapper healthAdviceMapper;
    private final HealthScoreMapper healthScoreMapper;
    private final DifyService difyService;

    public HealthAdviceAsyncService(HealthMetricMapper healthMetricMapper,
                                    HealthAdviceMapper healthAdviceMapper,
                                    HealthScoreMapper healthScoreMapper,
                                    DifyService difyService) {
        this.healthMetricMapper = healthMetricMapper;
        this.healthAdviceMapper = healthAdviceMapper;
        this.healthScoreMapper = healthScoreMapper;
        this.difyService = difyService;
    }

    @Async
    public void generateAdviceAsync(Long userId) {
        try {
            Map<String, Object> healthData = buildHealthDataForDify(userId);
            if (healthData.isEmpty()) {
                log.debug("用户 {} 暂无健康数据，跳过 Dify 调用", userId);
                return;
            }
            log.info("用户 {} 开始异步生成健康建议（Dify 工作流可能需 1-2 分钟）", userId);
            DifyAdviceResult result = difyService.runHealthAdviceWorkflow(healthData, userId);
            if (result == null) {
                log.warn("用户 {} Dify 调用无结果", userId);
                return;
            }

            LocalDate today = LocalDate.now();

            // 保存 Dify 返回的 total_score 到 health_score 表
            if (result.getTotalScore() != null) {
                int score = (int) Math.round(result.getTotalScore());
                String bodyStatus = score >= 80 ? "良好" : score >= 60 ? "一般" : "注意";
                HealthScore hs = new HealthScore();
                hs.setUserId(userId);
                hs.setScore(score);
                hs.setBodyStatus(bodyStatus);
                hs.setScoreDate(today);
                healthScoreMapper.upsert(hs);
            }

            healthAdviceMapper.deleteByUserIdAndDate(userId, today);

            List<HealthAdvice> toInsert = new ArrayList<>();
            addAdvice(toInsert, userId, "diet", "饮食建议", result.getDiet(), today);
            addAdvice(toInsert, userId, "exercise", "运动建议", result.getExercise(), today);
            addAdvice(toInsert, userId, "lifestyle", "生活建议", result.getLifestyle(), today);
            addAdvice(toInsert, userId, "medical", "就医建议", result.getMedical(), today);

            for (HealthAdvice a : toInsert) {
                healthAdviceMapper.insert(a);
            }
            log.info("用户 {} 健康建议已更新", userId);
        } catch (Exception e) {
            log.warn("获取健康建议失败: userId={}", userId, e);
        }
    }

    private void addAdvice(List<HealthAdvice> list, Long userId, String category, String title, String content, LocalDate date) {
        if (content == null || content.isBlank()) return;
        HealthAdvice a = new HealthAdvice();
        a.setUserId(userId);
        a.setCategory(category);
        a.setTitle(title);
        a.setContent(content);
        a.setAdviceDate(date);
        list.add(a);
    }

    private Map<String, Object> buildHealthDataForDify(Long userId) {
        Map<String, Object> data = new HashMap<>();
        // 血压：同时传收缩压和舒张压
        HealthMetric bp = getLatestMetric(userId, "bp");
        if (bp != null && bp.getValue1() != null && bp.getValue2() != null) {
            data.put("bpHigh", bp.getValue1().doubleValue());
            data.put("bpLow", bp.getValue2().doubleValue());
        }
        putLatestValue(data, userId, "heartRate", "heartRate");
        putLatestValue(data, userId, "temperature", "temperature");
        putLatestValue(data, userId, "bloodSugar", "bloodSugar");
        putLatestValue(data, userId, "sleep", "sleep");
        putLatestValue(data, userId, "breath", "breath");
        // 身高体重：计算 BMI 后传参，不传 weight/height
        HealthMetric weight = getLatestMetric(userId, "weight");
        HealthMetric height = getLatestMetric(userId, "height");
        if (weight != null && weight.getValue1() != null && height != null
                && height.getValue1() != null && height.getValue1().doubleValue() > 0) {
            double h = height.getValue1().doubleValue() / 100;
            double bmi = weight.getValue1().doubleValue() / (h * h);
            data.put("bmi", Math.round(bmi * 10) / 10.0);
        }
        return data;
    }

    private void putLatestValue(Map<String, Object> data, Long userId, String metricType, String jsonKey) {
        HealthMetric m = getLatestMetric(userId, metricType);
        if (m != null && m.getValue1() != null) {
            data.put(jsonKey, m.getValue1().doubleValue());
        }
    }

    private HealthMetric getLatestMetric(Long userId, String metricType) {
        List<HealthMetric> list = healthMetricMapper.findByUserIdAndType(userId, metricType, 1);
        return list.isEmpty() ? null : list.get(0);
    }
}
