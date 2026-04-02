package com.example.smart_health_management.health.mapper;

import com.example.smart_health_management.health.model.HealthMetric;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HealthMetricMapper {

    @Insert("""
            INSERT INTO health_metric (user_id, metric_type, value1, value2, unit, record_date, record_time, notes, source)
            VALUES (#{userId}, #{metricType}, #{value1}, #{value2}, #{unit}, #{recordDate}, #{recordTime}, #{notes}, #{source})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HealthMetric metric);

    @Select("""
            SELECT id, user_id, metric_type, value1, value2, unit, record_date, record_time, notes, source, created_at
            FROM health_metric
            WHERE user_id = #{userId} AND metric_type = #{metricType}
            ORDER BY record_date DESC, record_time DESC
            LIMIT #{limit}
            """)
    List<HealthMetric> findByUserIdAndType(@Param("userId") Long userId, @Param("metricType") String metricType, @Param("limit") int limit);

    @Select("""
            SELECT id, user_id, metric_type, value1, value2, unit, record_date, record_time, notes, source, created_at
            FROM health_metric
            WHERE user_id = #{userId} AND metric_type = #{metricType}
              AND record_date >= #{startDate} AND record_date <= #{endDate}
            ORDER BY record_date ASC, record_time ASC
            """)
    List<HealthMetric> findByUserIdAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("metricType") String metricType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT id, user_id, metric_type, value1, value2, unit, record_date, record_time, notes, source, created_at
            FROM health_metric
            WHERE user_id = #{userId}
            ORDER BY record_date DESC, record_time DESC
            LIMIT #{limit}
            """)
    List<HealthMetric> findByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("""
            SELECT id, user_id, metric_type, value1, value2, unit, record_date, record_time, notes, source, created_at
            FROM health_metric
            WHERE user_id = #{userId} AND metric_type = 'weight'
            ORDER BY record_date DESC, record_time DESC
            LIMIT 1
            """)
    HealthMetric findLatestWeight(Long userId);

    @Select("""
            SELECT id, user_id, metric_type, value1, value2, unit, record_date, record_time, notes, source, created_at
            FROM health_metric
            WHERE user_id = #{userId} AND metric_type = 'height'
            ORDER BY record_date DESC, record_time DESC
            LIMIT 1
            """)
    HealthMetric findLatestHeight(Long userId);
}
