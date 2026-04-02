package com.example.smart_health_management.health.service.impl;

import com.example.smart_health_management.common.BizException;
import com.example.smart_health_management.health.dto.*;
import com.example.smart_health_management.health.mapper.HealthAdviceMapper;
import com.example.smart_health_management.health.mapper.HealthMetricMapper;
import com.example.smart_health_management.health.mapper.HealthScoreMapper;
import com.example.smart_health_management.health.model.HealthAdvice;
import com.example.smart_health_management.health.model.HealthMetric;
import com.example.smart_health_management.health.model.HealthScore;
import com.example.smart_health_management.health.service.HealthAdviceAsyncService;
import com.example.smart_health_management.health.service.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HealthServiceImpl implements HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthServiceImpl.class);

    private static final Map<String, MetricConfig> METRIC_CONFIG = Map.of(
            "bp", new MetricConfig("血压", "mmHg", "90-140/60-90", "90", "140", "60", "90"),
            "heartRate", new MetricConfig("心率", "次/分", "60-100", "60", "100", null, null),
            "temperature", new MetricConfig("体温", "°C", "36.0-37.3", "36.0", "37.3", null, null),
            "bloodSugar", new MetricConfig("血糖", "mmol/L", "空腹3.9-6.1", "3.9", "6.1", null, null),
            "sleep", new MetricConfig("睡眠", "小时", "7-9", "7", "9", null, null),
            "breath", new MetricConfig("呼吸", "次/分", "12-20", "12", "20", null, null),
            "weight", new MetricConfig("体重", "kg", "-", null, null, null, null),
            "height", new MetricConfig("身高", "cm", "-", null, null, null, null)
    );

    private static final Map<String, String> UNIT_MAP = Map.of(
            "bp", "mmHg",
            "heartRate", "次/分",
            "temperature", "°C",
            "bloodSugar", "mmol/L",
            "sleep", "小时",
            "breath", "次/分",
            "weight", "kg",
            "height", "cm"
    );

    private final HealthMetricMapper healthMetricMapper;
    private final HealthAdviceMapper healthAdviceMapper;
    private final HealthScoreMapper healthScoreMapper;
    private final HealthAdviceAsyncService healthAdviceAsyncService;

    public HealthServiceImpl(HealthMetricMapper healthMetricMapper,
                             HealthAdviceMapper healthAdviceMapper,
                             HealthScoreMapper healthScoreMapper,
                             HealthAdviceAsyncService healthAdviceAsyncService) {
        this.healthMetricMapper = healthMetricMapper;
        this.healthAdviceMapper = healthAdviceMapper;
        this.healthScoreMapper = healthScoreMapper;
        this.healthAdviceAsyncService = healthAdviceAsyncService;
    }

    @Override
    public void submitManualInput(Long userId, HealthInputRequest request) {
        BigDecimal v1, v2 = null;
        if ("bp".equals(request.getMetricType())) {
            v1 = request.getBpHigh() != null ? request.getBpHigh() : request.getValue1();
            v2 = request.getBpLow() != null ? request.getBpLow() : request.getValue2();
            if (v1 == null || v2 == null) {
                throw new BizException(400, "血压需同时填写收缩压和舒张压");
            }
        } else {
            v1 = request.getValue() != null ? request.getValue() : request.getValue1();
            if (v1 == null) {
                throw new BizException(400, "请输入数值");
            }
        }

        String unit = UNIT_MAP.getOrDefault(request.getMetricType(), "");
        LocalTime recordTime = parseTime(request.getRecordTime());
        LocalDate recordDate = LocalDate.parse(request.getRecordDate());

        HealthMetric metric = new HealthMetric();
        metric.setUserId(userId);
        metric.setMetricType(request.getMetricType());
        metric.setValue1(v1);
        metric.setValue2(v2);
        metric.setUnit(unit);
        metric.setRecordDate(recordDate);
        metric.setRecordTime(recordTime);
        metric.setNotes(request.getNotes());
        metric.setSource("manual");

        healthMetricMapper.insert(metric);
        healthAdviceAsyncService.generateAdviceAsync(userId);
    }

    @Override
    public void submitBatchInput(Long userId, HealthInputBatchRequest request) {
        LocalDate recordDate = LocalDate.parse(request.getRecordDate());
        LocalTime recordTime = parseTime(request.getRecordTime());
        String notes = request.getNotes();
        int inserted = 0;

        for (HealthInputBatchRequest.MetricItem item : request.getMetrics()) {
            BigDecimal v1, v2 = null;
            if ("bp".equals(item.getMetricType())) {
                v1 = item.getBpHigh() != null ? item.getBpHigh() : item.getValue1();
                v2 = item.getBpLow() != null ? item.getBpLow() : item.getValue2();
                if (v1 == null || v2 == null) continue;
            } else {
                v1 = item.getValue() != null ? item.getValue() : item.getValue1();
                if (v1 == null) continue;
            }

            String unit = UNIT_MAP.getOrDefault(item.getMetricType(), "");
            HealthMetric metric = new HealthMetric();
            metric.setUserId(userId);
            metric.setMetricType(item.getMetricType());
            metric.setValue1(v1);
            metric.setValue2(v2);
            metric.setUnit(unit);
            metric.setRecordDate(recordDate);
            metric.setRecordTime(recordTime);
            metric.setNotes(notes);
            metric.setSource("manual");
            healthMetricMapper.insert(metric);
            inserted++;
        }
        if (inserted == 0) {
            throw new BizException(400, "未提交任何有效指标，请至少填写一个指标的数值");
        }
        healthAdviceAsyncService.generateAdviceAsync(userId);
    }

    @Override
    public void submitVoiceInput(Long userId, HealthInputVoiceRequest request) {
        List<HealthInputVoiceRequest.ExtractedMetric> list = request.getExtractedData();
        if (list == null || list.isEmpty()) {
            throw new BizException(400, "未提取到有效指标数据");
        }

        LocalDate recordDate = request.getRecordDate() != null && !request.getRecordDate().isBlank()
                ? LocalDate.parse(request.getRecordDate()) : LocalDate.now();
        LocalTime recordTime = parseTime(request.getRecordTime());

        for (HealthInputVoiceRequest.ExtractedMetric em : list) {
            if (em.getType() == null || em.getValue() == null) continue;

            BigDecimal v1, v2 = null;
            if (em.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) em.getValue();
                Object high = m.get("high");
                Object low = m.get("low");
                if (high != null && low != null) {
                    v1 = toBigDecimal(high);
                    v2 = toBigDecimal(low);
                } else {
                    continue;
                }
            } else {
                v1 = toBigDecimal(em.getValue());
            }

            String unit = UNIT_MAP.getOrDefault(em.getType(), "");
            HealthMetric metric = new HealthMetric();
            metric.setUserId(userId);
            metric.setMetricType(em.getType());
            metric.setValue1(v1);
            metric.setValue2(v2);
            metric.setUnit(unit);
            metric.setRecordDate(recordDate);
            metric.setRecordTime(recordTime);
            metric.setSource("voice");
            healthMetricMapper.insert(metric);
        }
        healthAdviceAsyncService.generateAdviceAsync(userId);
    }

    @Override
    public List<HealthMetricsSummaryDto> getHealthMetrics(Long userId) {
        List<HealthMetricsSummaryDto> result = new ArrayList<>();
        String[] displayTypes = {"temperature", "bp", "bloodSugar", "bmi", "heartRate", "sleep"};

        for (String type : displayTypes) {
            HealthMetricsSummaryDto dto = new HealthMetricsSummaryDto();
            dto.setType(type);

            if ("bmi".equals(type)) {
                HealthMetric w = healthMetricMapper.findLatestWeight(userId);
                HealthMetric h = healthMetricMapper.findLatestHeight(userId);
                if (w != null && h != null && h.getValue1() != null && h.getValue1().doubleValue() > 0) {
                    double bmi = w.getValue1().doubleValue() / Math.pow(h.getValue1().doubleValue() / 100, 2);
                    dto.setValue1(BigDecimal.valueOf(bmi).setScale(1, RoundingMode.HALF_UP));
                    dto.setValue(String.format("%.1f", bmi));
                    dto.setUnit("kg/m²");
                    dto.setLabel("BMI");
                    evalBmiStatus(dto, bmi);
                } else {
                    dto.setValue("-");
                    dto.setLabel("BMI");
                    dto.setUnit("kg/m²");
                    dto.setStatus("normal");
                    dto.setStatusText("暂无数据");
                }
            } else {
                HealthMetric latest = getLatestMetric(userId, type);
                if (latest != null) {
                    dto.setValue1(latest.getValue1());
                    dto.setValue2(latest.getValue2());
                    dto.setUnit(latest.getUnit());
                    MetricConfig cfg = METRIC_CONFIG.get(type);
                    dto.setLabel(cfg != null ? cfg.label : type);
                    dto.setValue(formatValue(latest));
                    evalStatus(dto, type, latest);
                } else {
                    dto.setValue("-");
                    dto.setLabel(METRIC_CONFIG.containsKey(type) ? METRIC_CONFIG.get(type).label : type);
                    dto.setUnit(METRIC_CONFIG.get(type) != null ? METRIC_CONFIG.get(type).unit : "");
                    dto.setStatus("normal");
                    dto.setStatusText("暂无数据");
                }
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public HealthMetricDetailResponse getMetricDetail(Long userId, String metricType, Integer limit) {
        validateMetricType(metricType);
        int lim = limit != null && limit > 0 ? Math.min(limit, 100) : 7;

        List<HealthMetric> list;
        if ("bmi".equals(metricType)) {
            list = buildBmiRecords(userId, lim);
        } else {
            list = healthMetricMapper.findByUserIdAndType(userId, metricType, lim);
        }

        HealthMetricDetailResponse resp = new HealthMetricDetailResponse();
        resp.setType(metricType);
        MetricConfig cfg = METRIC_CONFIG.get("bmi".equals(metricType) ? "weight" : metricType);
        resp.setTitle(cfg != null ? cfg.label : metricType);
        resp.setUnit("bmi".equals(metricType) ? "kg/m²" : (cfg != null ? cfg.unit : ""));
        resp.setRefRange(cfg != null ? cfg.refRange : "-");
        resp.setRecords(toRecordDtos(list, metricType));

        if (!list.isEmpty()) {
            HealthMetric first = list.get(0);
            resp.setValue(formatValue(first));
            HealthMetricRecordDto dto = resp.getRecords().get(0);
            resp.setStatusText(dto.getStatusText());
            resp.setStatusClass(dto.getStatus());
        } else {
            resp.setValue("-");
            resp.setStatusText("暂无数据");
            resp.setStatusClass("normal");
        }
        return resp;
    }

    @Override
    public HealthTrendDto getMetricTrend(Long userId, String metricType, LocalDate startDate, LocalDate endDate) {
        validateMetricType(metricType);
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();

        List<HealthMetric> list;
        if ("bmi".equals(metricType)) {
            list = buildBmiRecords(userId, startDate, endDate);
        } else {
            list = healthMetricMapper.findByUserIdAndTypeAndDateRange(userId, metricType, startDate, endDate);
        }

        HealthTrendDto dto = new HealthTrendDto();
        dto.setType(metricType);
        MetricConfig cfg = METRIC_CONFIG.get("bmi".equals(metricType) ? "weight" : metricType);
        dto.setRefText(cfg != null ? cfg.refRange : "-");

        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal max = null;
        BigDecimal min = null;

        for (HealthMetric m : list) {
            labels.add(m.getRecordDate().toString());
            BigDecimal v = m.getValue1();
            if (v != null) {
                values.add(v);
                sum = sum.add(v);
                max = max == null ? v : (v.compareTo(max) > 0 ? v : max);
                min = min == null ? v : (v.compareTo(min) < 0 ? v : min);
            }
        }

        dto.setLabels(labels);
        dto.setValues(values);
        if (!values.isEmpty()) {
            dto.setAvg(sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP));
            dto.setMax(max);
            dto.setMin(min);
        }
        dto.setAnalysis(buildTrendAnalysis(values, metricType));
        return dto;
    }

    @Override
    public List<HealthMetricRecordDto> getHealthHistory(Long userId, Integer limit) {
        int lim = limit != null && limit > 0 ? Math.min(limit, 100) : 50;
        List<HealthMetric> list = healthMetricMapper.findByUserId(userId, lim);
        return toRecordDtos(list, null);
    }

    @Override
    public Map<String, Object> getHealthScore(Long userId) {
        List<HealthMetric> anyMetric = healthMetricMapper.findByUserId(userId, 1);
        if (anyMetric.isEmpty()) {
            Map<String, Object> m = new HashMap<>();
            m.put("score", null);
            m.put("bodyStatus", "暂无数据");
            return m;
        }
        LocalDate today = LocalDate.now();
        HealthScore fromDb = healthScoreMapper.findByUserIdAndDate(userId, today);
        if (fromDb != null) {
            return Map.of(
                    "score", fromDb.getScore(),
                    "bodyStatus", fromDb.getBodyStatus() != null ? fromDb.getBodyStatus() : "一般");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("score", null);
        m.put("bodyStatus", "生成中");
        return m;
    }

    @Override
    public HealthAdviceResponse getHealthAdvice(Long userId) {
        LocalDate today = LocalDate.now();
        List<HealthAdvice> list = healthAdviceMapper.findByUserIdAndDate(userId, today);
        HealthAdviceResponse resp = new HealthAdviceResponse();
        if (!list.isEmpty()) {
            resp.setSuggestions(list.stream()
                    .map(a -> new HealthAdviceResponse.AdviceItem(a.getCategory(), a.getTitle(), a.getContent()))
                    .collect(Collectors.toList()));
        } else {
            resp.setSuggestions(List.of());
        }
        // 附带当天健康评分（来自 Dify）
        HealthScore scoreRecord = healthScoreMapper.findByUserIdAndDate(userId, today);
        if (scoreRecord != null && scoreRecord.getScore() != null) {
            resp.setTotalScore(scoreRecord.getScore().doubleValue());
        }
        return resp;
    }

    private HealthMetric getLatestMetric(Long userId, String metricType) {
        List<HealthMetric> list = healthMetricMapper.findByUserIdAndType(userId, metricType, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    private void validateMetricType(String type) {
        if (type == null || !METRIC_CONFIG.containsKey(type) && !"bmi".equals(type)) {
            throw new BizException(400, "无效的指标类型");
        }
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        return new BigDecimal(o.toString());
    }

    private String formatValue(HealthMetric m) {
        if (m.getValue2() != null) {
            return formatNum(m.getValue1()) + "/" + formatNum(m.getValue2());
        }
        return m.getValue1() != null ? formatNum(m.getValue1()) : "-";
    }

    /** 去除多余小数位，如 121.00 -> 121, 36.5 -> 36.5 */
    private String formatNum(BigDecimal v) {
        if (v == null) return "-";
        return v.stripTrailingZeros().toPlainString();
    }

    private void evalStatus(HealthMetricsSummaryDto dto, String type, HealthMetric m) {
        MetricConfig cfg = METRIC_CONFIG.get(type);
        if (cfg == null || cfg.low == null) {
            dto.setStatus("normal");
            dto.setStatusText("已记录");
            return;
        }
        BigDecimal v1 = m.getValue1();
        BigDecimal v2 = m.getValue2();
        if (v1 == null) {
            dto.setStatus("normal");
            dto.setStatusText("已记录");
            return;
        }
        if ("bp".equals(type) && v2 != null) {
            double high = v1.doubleValue();
            double low = v2.doubleValue();
            if (high > 140 || low > 90) {
                dto.setStatus("danger");
                dto.setStatusText("偏高");
            } else if (high < 90 || low < 60) {
                dto.setStatus("warn");
                dto.setStatusText("偏低");
            } else {
                dto.setStatus("normal");
                dto.setStatusText("正常");
            }
        } else {
            double v = v1.doubleValue();
            double low = Double.parseDouble(cfg.low);
            double high = Double.parseDouble(cfg.high);
            if (v < low) {
                dto.setStatus("warn");
                dto.setStatusText("偏低");
            } else if (v > high) {
                dto.setStatus("danger");
                dto.setStatusText("偏高");
            } else {
                dto.setStatus("normal");
                dto.setStatusText("正常");
            }
        }
    }

    private void evalBmiStatus(HealthMetricsSummaryDto dto, double bmi) {
        if (bmi < 18.5) {
            dto.setStatus("warn");
            dto.setStatusText("偏瘦");
        } else if (bmi > 24) {
            dto.setStatus("danger");
            dto.setStatusText("偏胖");
        } else {
            dto.setStatus("normal");
            dto.setStatusText("正常");
        }
    }

    private List<HealthMetricRecordDto> toRecordDtos(List<HealthMetric> list, String metricType) {
        List<HealthMetricRecordDto> result = new ArrayList<>();
        for (HealthMetric m : list) {
            HealthMetricRecordDto dto = new HealthMetricRecordDto();
            dto.setId(m.getId());
            dto.setMetricType(m.getMetricType());
            dto.setValue1(m.getValue1());
            dto.setValue2(m.getValue2());
            dto.setUnit(m.getUnit());
            dto.setRecordDate(m.getRecordDate());
            dto.setRecordTime(m.getRecordTime());
            dto.setNotes(m.getNotes());
            dto.setSource(m.getSource());
            dto.setValueDisplay(formatValue(m));
            evalRecordStatus(dto, m.getMetricType(), m);
            result.add(dto);
        }
        return result;
    }

    private void evalRecordStatus(HealthMetricRecordDto dto, String type, HealthMetric m) {
        MetricConfig cfg = METRIC_CONFIG.get(type);
        if (cfg == null || cfg.low == null) {
            dto.setStatus("normal");
            dto.setStatusText("已记录");
            return;
        }
        BigDecimal v1 = m.getValue1();
        BigDecimal v2 = m.getValue2();
        if (v1 == null) {
            dto.setStatus("normal");
            dto.setStatusText("已记录");
            return;
        }
        if ("bp".equals(type) && v2 != null) {
            double high = v1.doubleValue();
            double low = v2.doubleValue();
            if (high > 140 || low > 90) {
                dto.setStatus("high");
                dto.setStatusText("偏高");
            } else if (high < 90 || low < 60) {
                dto.setStatus("warn");
                dto.setStatusText("偏低");
            } else {
                dto.setStatus("normal");
                dto.setStatusText("正常");
            }
        } else {
            double v = v1.doubleValue();
            double low = Double.parseDouble(cfg.low);
            double high = Double.parseDouble(cfg.high);
            if (v < low) {
                dto.setStatus("warn");
                dto.setStatusText("偏低");
            } else if (v > high) {
                dto.setStatus("high");
                dto.setStatusText("偏高");
            } else {
                dto.setStatus("normal");
                dto.setStatusText("正常");
            }
        }
    }

    private List<HealthMetric> buildBmiRecords(Long userId, int limit) {
        List<HealthMetric> weightList = healthMetricMapper.findByUserIdAndType(userId, "weight", limit * 2);
        List<HealthMetric> heightList = healthMetricMapper.findByUserIdAndType(userId, "height", 1);
        HealthMetric latestHeight = heightList.isEmpty() ? null : heightList.get(0);
        if (latestHeight == null || latestHeight.getValue1() == null || latestHeight.getValue1().doubleValue() <= 0) {
            return Collections.emptyList();
        }
        double h = latestHeight.getValue1().doubleValue() / 100;
        List<HealthMetric> result = new ArrayList<>();
        for (HealthMetric w : weightList) {
            if (w.getValue1() == null) continue;
            HealthMetric bmi = new HealthMetric();
            bmi.setValue1(BigDecimal.valueOf(w.getValue1().doubleValue() / (h * h)).setScale(1, RoundingMode.HALF_UP));
            bmi.setRecordDate(w.getRecordDate());
            bmi.setRecordTime(w.getRecordTime());
            bmi.setMetricType("bmi");
            bmi.setUnit("kg/m²");
            result.add(bmi);
            if (result.size() >= limit) break;
        }
        return result;
    }

    private List<HealthMetric> buildBmiRecords(Long userId, LocalDate start, LocalDate end) {
        List<HealthMetric> weightList = healthMetricMapper.findByUserIdAndTypeAndDateRange(userId, "weight", start, end);
        HealthMetric latestHeight = healthMetricMapper.findLatestHeight(userId);
        if (latestHeight == null || latestHeight.getValue1() == null || latestHeight.getValue1().doubleValue() <= 0) {
            return Collections.emptyList();
        }
        double h = latestHeight.getValue1().doubleValue() / 100;
        return weightList.stream()
                .filter(w -> w.getValue1() != null)
                .map(w -> {
                    HealthMetric bmi = new HealthMetric();
                    bmi.setValue1(BigDecimal.valueOf(w.getValue1().doubleValue() / (h * h)).setScale(1, RoundingMode.HALF_UP));
                    bmi.setRecordDate(w.getRecordDate());
                    bmi.setRecordTime(w.getRecordTime());
                    bmi.setMetricType("bmi");
                    return bmi;
                })
                .collect(Collectors.toList());
    }

    private String buildTrendAnalysis(List<BigDecimal> values, String type) {
        if (values == null || values.size() < 2) return "数据不足，请继续记录";
        BigDecimal first = values.get(0);
        BigDecimal last = values.get(values.size() - 1);
        int cmp = last.compareTo(first);
        if (cmp > 0) return "呈上升趋势";
        if (cmp < 0) return "呈下降趋势";
        return "趋势平稳";
    }

    private static class MetricConfig {
        final String label;
        final String unit;
        final String refRange;
        final String low;
        final String high;
        final String low2;
        final String high2;

        MetricConfig(String label, String unit, String refRange, String low, String high, String low2, String high2) {
            this.label = label;
            this.unit = unit;
            this.refRange = refRange;
            this.low = low;
            this.high = high;
            this.low2 = low2;
            this.high2 = high2;
        }
    }
}
